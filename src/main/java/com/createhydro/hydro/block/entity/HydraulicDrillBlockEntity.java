package com.createhydro.hydro.block.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.Config;
import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.HydraulicDrillBlock;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.createhydro.hydro.registry.ModFluids;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.BlockHelper;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

// pressure-driven breaker: pipe on back, target block in front. one-tap per stroke, then retract.
public class HydraulicDrillBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    public static final int CYCLE = 240;
    private static final int DRILL_SPEED = 6; // CYCLE/6 = 40 game ticks (~2s) per stroke

    public static final int TANK_CAPACITY = 1000;
    private static final int DRILL_COOLDOWN = 20;
    private static final int IDLE_SCAN = 8;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                updateFullState();
                sync();
            }
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ModFluids.HYDRO_FLUID.get()
                    || stack.getFluid() == ModFluids.HYDRO_FLUID_FLOWING.get();
        }
    };

    private boolean running = false;
    private int runningTicks = 0;
    private int prevRunningTicks = 0;
    private boolean underpressured = false;
    private int idleCooldown = IDLE_SCAN;
    private int cooldown = 0;

    // client-side flash: >0 = red (stalled), <0 = green (recovered), decays to 0
    private float overstressEffect = 0.0F;
    private float prevOverstressEffect = 0.0F;
    private boolean wasStalledClient = false;
    private int stallSmokeTimer = 0;

    public HydraulicDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRAULIC_DRILL.get(), pos, state);
    }

    public FluidTank getFluidTank() {
        return tank;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isUnderpressured() {
        return underpressured;
    }

    // ponder worlds are client-side so serverTick never fires there; scene calls this directly
    public void startPonderStroke() {
        running = true;
        runningTicks = 0;
        prevRunningTicks = 0;
        underpressured = false;
    }

    // 0 = retracted, 1 = fully extended. renderer multiplies by head travel distance.
    public float getRenderedExtension(float partialTicks) {
        if (!running) {
            return 0.0F;
        }
        int ticksNow = Math.abs(runningTicks);
        float ticks = Mth.lerp(partialTicks, prevRunningTicks, ticksNow);
        float t = Mth.clamp(ticks / CYCLE, 0.0F, 1.0F);
        if (t <= 0.4F) {
            float x = t / 0.4F;
            return Mth.clamp(x * x, 0.0F, 1.0F);
        }
        if (t <= 0.5F) {
            return 1.0F;
        }
        return Mth.clamp(1.0F - (t - 0.5F) / 0.5F, 0.0F, 1.0F);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HydraulicDrillBlockEntity drill) {
        if (level.isClientSide) {
            drill.clientTick();
        } else {
            drill.serverTick(level, pos);
        }
    }

    private void clientTick() {
        tickFlash();
        tickStallSmoke();
        if (!running) {
            return;
        }
        prevRunningTicks = runningTicks;
        runningTicks += DRILL_SPEED;
        if (runningTicks > CYCLE) {
            running = false;
            runningTicks = 0;
            prevRunningTicks = 0;
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        float available = readAvailablePressure(level, pos);

        if (running) {
            prevRunningTicks = runningTicks;
            runningTicks += DRILL_SPEED;
            if (prevRunningTicks < CYCLE / 2 && runningTicks >= CYCLE / 2) {
                applyDrill(level, pos, available);
            }
            if (runningTicks > CYCLE) {
                if (evaluateAndUpdate(level, pos, available)) {
                    start(level, pos);
                } else {
                    stop();
                }
            }
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (--idleCooldown > 0) {
            return;
        }
        idleCooldown = IDLE_SCAN;

        if (evaluateAndUpdate(level, pos, available)) {
            start(level, pos);
        }
    }

    private void start(Level level, BlockPos pos) {
        running = true;
        runningTicks = 0;
        prevRunningTicks = 0;
        underpressured = false;
        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.4F, 0.55F);
        }
        sync();
    }

    private void stop() {
        running = false;
        runningTicks = 0;
        prevRunningTicks = 0;
        cooldown = DRILL_COOLDOWN;
        sync();
    }

    @Nullable
    private HardenedIronPipeBlockEntity backPipe(Level level, BlockPos pos) {
        Direction facing = getBlockState().getValue(HydraulicDrillBlock.FACING);
        BlockPos backPos = pos.relative(facing.getOpposite());
        if (level.getBlockEntity(backPos) instanceof HardenedIronPipeBlockEntity pipe
                && !pipe.isFlowBlocked() && pipe.connectsOnSide(facing)) {
            return pipe;
        }
        return null;
    }

    private float readAvailablePressure(Level level, BlockPos pos) {
        HardenedIronPipeBlockEntity pipe = backPipe(level, pos);
        return pipe != null ? pipe.getPressure() : 0.0F;
    }

    private boolean hasPressureLine(Level level, BlockPos pos) {
        return backPipe(level, pos) != null;
    }

    private BlockPos targetPos(BlockPos pos) {
        return pos.relative(getBlockState().getValue(HydraulicDrillBlock.FACING));
    }

    private boolean canBreak(Level level, BlockPos tp) {
        BlockState state = level.getBlockState(tp);
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, tp);
        if (hardness < 0) {
            return false; // bedrock and the like
        }
        // skip pass-through blocks (no collision shape) so we don't churn on grass or torches
        return !state.getCollisionShape(level, tp).isEmpty();
    }

    private enum RunState { READY, NO_WORK, NO_FLUID, UNDERPRESSURED }

    private RunState evaluate(Level level, BlockPos pos, float available) {
        float required = (float) (double) Config.DRILL_PRESSURE.get();
        boolean lineConnected = hasPressureLine(level, pos);
        // connected line that can't carry the load = underpressured, same concept as Create's overstress
        if (lineConnected && available < required) {
            return RunState.UNDERPRESSURED;
        }
        if (tank.getFluid().isEmpty()) {
            return RunState.NO_FLUID;
        }
        if (!lineConnected) {
            return RunState.NO_WORK;
        }
        if (!canBreak(level, targetPos(pos))) {
            return RunState.NO_WORK;
        }
        return RunState.READY;
    }

    private boolean evaluateAndUpdate(Level level, BlockPos pos, float available) {
        RunState state = evaluate(level, pos, available);
        boolean nowUnder = state == RunState.UNDERPRESSURED;
        if (nowUnder != underpressured) {
            underpressured = nowUnder;
            sync();
        }
        return state == RunState.READY;
    }

    private void applyDrill(Level level, BlockPos pos, float available) {
        if (available < (float) (double) Config.DRILL_PRESSURE.get() || tank.getFluid().isEmpty()) {
            stop();
            return;
        }
        BlockPos tp = targetPos(pos);
        if (!canBreak(level, tp)) {
            return;
        }
        BlockState state = level.getBlockState(tp);
        // loot drops at the broken pos so it falls onto belts/funnels below
        level.playSound(null, tp, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.5F, 0.9F);
        BlockHelper.destroyBlock(level, tp, 1.0F, stack -> Block.popResource(level, tp, stack));
        level.playSound(null, tp, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.6F, 0.85F);
    }

    private void updateFullState() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        boolean shouldBeFull = !tank.getFluid().isEmpty();
        if (state.hasProperty(HydraulicDrillBlock.FULL) && state.getValue(HydraulicDrillBlock.FULL) != shouldBeFull) {
            level.setBlock(worldPosition, state.setValue(HydraulicDrillBlock.FULL, shouldBeFull), Block.UPDATE_ALL);
        }
    }

    private void tickFlash() {
        boolean stalled = underpressured;
        if (stalled != wasStalledClient) {
            wasStalledClient = stalled;
            overstressEffect = stalled ? 1.0F : -1.0F;
            spawnFlashParticles(stalled);
        }
        prevOverstressEffect = overstressEffect;
        if (overstressEffect != 0.0F) {
            overstressEffect -= overstressEffect * 0.1F;
            if (Math.abs(overstressEffect) < 1.0F / 128.0F) {
                overstressEffect = 0.0F;
            }
        }
    }

    public float getRenderOverstress(float partialTick) {
        return Mth.lerp(partialTick, prevOverstressEffect, overstressEffect);
    }

    private void tickStallSmoke() {
        if (level == null || !underpressured) {
            stallSmokeTimer = 0;
            return;
        }
        if (--stallSmokeTimer > 0) {
            return;
        }
        stallSmokeTimer = 18;
        Vec3 c = Vec3.atCenterOf(worldPosition);
        level.addParticle(ParticleTypes.SMOKE,
                c.x + (level.random.nextDouble() - 0.5) * 0.4,
                c.y + 0.25 + level.random.nextDouble() * 0.25,
                c.z + (level.random.nextDouble() - 0.5) * 0.4,
                0.0, 0.02, 0.0);
    }

    private void spawnFlashParticles(boolean stalled) {
        if (level == null) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(worldPosition);
        int count = stalled ? 5 : 2;
        double spread = stalled ? 0.2 : 0.075;
        var particle = stalled ? ParticleTypes.SMOKE : ParticleTypes.CLOUD;
        for (int i = 0; i < count; i++) {
            double mx = (level.random.nextDouble() - 0.5) * 2.0 * spread;
            double my = (level.random.nextDouble() - 0.5) * 2.0 * spread;
            double mz = (level.random.nextDouble() - 0.5) * 2.0 * spread;
            level.addParticle(particle, center.x, center.y, center.z, mx, my, mz);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        lang().translate("gui.goggles.hydraulic_drill").forGoggles(tooltip);

        if (underpressured) {
            lang().add(dot(ChatFormatting.RED))
                    .add(lang().translate("gui.hydraulic_drill.underpressured").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else if (running) {
            lang().add(dot(ChatFormatting.GREEN))
                    .add(lang().translate("gui.hydraulic_drill.drilling").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else if (tank.getFluid().isEmpty()) {
            lang().add(dot(ChatFormatting.GRAY))
                    .add(lang().translate("gui.hydraulic_drill.no_fluid").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else {
            lang().add(dot(ChatFormatting.GRAY))
                    .add(lang().translate("gui.hydraulic_drill.idle").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        }

        lang().add(lang().translate("gui.hydraulic_drill.pressure").style(ChatFormatting.GRAY))
                .space()
                .add(lang().text(LangNumberFormat.format((double) Config.DRILL_PRESSURE.get())).style(ChatFormatting.GOLD))
                .add(lang().text(ChatFormatting.DARK_GRAY, " PU"))
                .forGoggles(tooltip, 1);

        lang().add(lang().translate("gui.hydraulic_drill.fluid").style(ChatFormatting.GRAY))
                .space()
                .add(lang().text(LangNumberFormat.format(tank.getFluidAmount())).style(ChatFormatting.AQUA))
                .add(lang().text(ChatFormatting.DARK_GRAY, " / " + LangNumberFormat.format(TANK_CAPACITY) + " mB"))
                .forGoggles(tooltip, 1);
        return true;
    }

    private static LangBuilder dot(ChatFormatting color) {
        return lang().text(color, "■ ");
    }

    private static LangBuilder lang() {
        return new LangBuilder(CreateHydraulics.MODID);
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Running", running);
        tag.putInt("RunningTicks", runningTicks);
        tag.putBoolean("Underpressured", underpressured);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        running = tag.getBoolean("Running");
        runningTicks = tag.getInt("RunningTicks");
        prevRunningTicks = runningTicks;
        underpressured = tag.getBoolean("Underpressured");
        tank.readFromNBT(registries, tag.getCompound("Tank"));
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
}
