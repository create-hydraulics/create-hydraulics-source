package com.createhydro.hydro.block.entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.block.HardenedIronPipeBlock;
import com.createhydro.hydro.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// pure pressure conduit. not a fluid pipe — no IFluidHandler, no fluid stored. sources flood this each tick.
public class HardenedIronPipeBlockEntity extends BlockEntity {

    public static final float MAX_PRESSURE = 2048.0F;
    public static final float PRESSURE_FILL_PER_SECOND = 100.0F;
    private static final float PRESSURE_FILL_PER_TICK = PRESSURE_FILL_PER_SECOND / 20.0F;
    private static final float PRESSURE_DECAY_PER_TICK = 2.0F;
    private static final int HOUSEKEEPING_INTERVAL = 20;
    // safety cap so a huge network doesn't murder the tick budget
    private static final int MAX_NETWORK_SIZE = 4096;

    // set by floodFromSource each tick this segment is reachable from a source; cleared after it's consumed
    private boolean suppliedThisTick = false;
    // contributions from multiple sources on the same network add up — two intakes push twice the pressure
    private float suppliedTarget = 0.0F;

    private float pressure = 0.0F;

    // staggered per-position so not every pipe does housekeeping on the same tick
    protected int housekeepingCooldown;
    protected float lastSyncedPressure = -1.0F;
    protected int syncCooldown = 0;

    protected HardenedIronPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.housekeepingCooldown = (int) Math.floorMod(pos.asLong(), HOUSEKEEPING_INTERVAL);
    }

    public HardenedIronPipeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.HARDENED_IRON_PIPE.get(), pos, state);
    }

    public boolean isFlowBlocked() {
        return false;
    }

    public boolean connectsOnSide(Direction dir) {
        return true;
    }

    public float getPressure() {
        return pressure;
    }

    public void applySource(float target) {
        this.suppliedThisTick = true;
        this.suppliedTarget += target;
    }

    public void addPressure(float amount) {
        this.pressure = Mth.clamp(this.pressure + amount, 0.0F, MAX_PRESSURE);
        setChanged();
    }

    public void setPressure(float value) {
        this.pressure = Mth.clamp(value, 0.0F, MAX_PRESSURE);
        setChanged();
    }

    // BFS over the connected pipe network from start, calling applySource on every reachable segment.
    // both sides of each join must agree on the connection; blocked or non-connecting pipes terminate the walk.
    public static void floodFromSource(Level level, BlockPos start, float target) {
        if (!(level.getBlockEntity(start) instanceof HardenedIronPipeBlockEntity startPipe) || startPipe.isFlowBlocked()) {
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        startPipe.applySource(target);

        int budget = MAX_NETWORK_SIZE;
        while (!queue.isEmpty() && budget-- > 0) {
            BlockPos pos = queue.poll();
            if (!(level.getBlockEntity(pos) instanceof HardenedIronPipeBlockEntity pipe)) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                if (!pipe.connectsOnSide(dir)) {
                    continue;
                }
                BlockPos nextPos = pos.relative(dir);
                if (visited.contains(nextPos)) {
                    continue;
                }
                if (!(level.getBlockEntity(nextPos) instanceof HardenedIronPipeBlockEntity neighbor)) {
                    continue;
                }
                if (neighbor.isFlowBlocked() || !neighbor.connectsOnSide(dir.getOpposite())) {
                    continue;
                }
                visited.add(nextPos);
                neighbor.applySource(target);
                queue.add(nextPos);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HardenedIronPipeBlockEntity pipe) {
        // periodically reconcile connection state — catches changes that don't trigger a block update (e.g. intake becoming submerged)
        if (--pipe.housekeepingCooldown <= 0) {
            pipe.housekeepingCooldown = HOUSEKEEPING_INTERVAL;
            if (state.getBlock() instanceof HardenedIronPipeBlock pipeBlock) {
                BlockState desired = pipeBlock.getConnectedState(state, level, pos);
                if (desired != state) {
                    level.setBlock(pos, desired, Block.UPDATE_ALL);
                    state = desired;
                }
            }
        }

        float oldPressure = pipe.pressure;
        if (pipe.suppliedThisTick) {
            // ease toward the combined source target; adding or removing a source settles to the new total
            float target = Math.min(pipe.suppliedTarget, MAX_PRESSURE);
            pipe.pressure = approach(pipe.pressure, target, PRESSURE_FILL_PER_TICK);
        } else if (pipe.pressure > 0.0F) {
            // nothing feeding this segment — bleed down so the line depressurizes on its own
            pipe.pressure = Math.max(0.0F, pipe.pressure - PRESSURE_DECAY_PER_TICK);
        }
        pipe.pressure = Mth.clamp(pipe.pressure, 0.0F, MAX_PRESSURE);

        // consume the source mark; sources re-apply it next tick while still running
        pipe.suppliedThisTick = false;
        pipe.suppliedTarget = 0.0F;

        if (Math.abs(pipe.pressure - oldPressure) > 1.0e-4F) {
            pipe.setChanged();
        }

        // push to clients every 4 ticks when it changes — keeps the flow-gauge needle tracking
        if (--pipe.syncCooldown <= 0) {
            pipe.syncCooldown = 4;
            if (Math.abs(pipe.pressure - pipe.lastSyncedPressure) > 0.5F) {
                pipe.lastSyncedPressure = pipe.pressure;
                pipe.sync(state);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Pressure", pressure);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pressure = tag.getFloat("Pressure");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }

    private static float approach(float current, float target, float step) {
        if (current < target) {
            return Math.min(target, current + step);
        }
        if (current > target) {
            return Math.max(target, current - step);
        }
        return current;
    }

    protected void sync(BlockState state) {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
