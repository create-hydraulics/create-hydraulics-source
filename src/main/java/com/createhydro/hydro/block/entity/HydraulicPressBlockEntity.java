package com.createhydro.hydro.block.entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.AqueductIntakeBlock;
import com.createhydro.hydro.block.HydraulicPressBlock;
import com.createhydro.hydro.block.HydrostaticIntakeBlock;
import com.createhydro.hydro.recipe.HydraulicPressRecipe;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.createhydro.hydro.registry.ModFluids;
import com.createhydro.hydro.registry.ModRecipes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

// sits above a depot, squishes whatever's on it. needs pipe above + hydro fluid. deliberately slow.
public class HydraulicPressBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    public static final int CYCLE = 240;
    private static final int PRESS_SPEED = 6; // 40 ticks per stroke, ~2s. not a race car.

    public static final float PRESS_PU_COST = 100.0F;
    public static final int TANK_CAPACITY = 1000;
    private static final int PRESS_COOLDOWN = 40;
    private static final int IDLE_SCAN = 8;
    public static final int MAX_INTAKES = 3; // more than this = overpressure = everything stalls, whoops
    private static final int NETWORK_SCAN_LIMIT = 4096;

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
    private boolean overpressured = false;
    private boolean underpressured = false;
    private int idleCooldown = IDLE_SCAN;
    private int cooldown = 0;

    // client-side flash: >0 = red, <0 = green, decays to 0
    private float overstressEffect = 0.0F;
    private float prevOverstressEffect = 0.0F;
    private boolean wasStalledClient = false;
    private int stallSmokeTimer = 0;

    public HydraulicPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRAULIC_PRESS.get(), pos, state);
    }

    public FluidTank getFluidTank() {
        return tank;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isOverpressured() {
        return overpressured;
    }

    public boolean isUnderpressured() {
        return underpressured;
    }

    // ponder worlds are client-side so serverTick never fires there; scene calls this directly
    public void startPonderStroke() {
        running = true;
        runningTicks = 0;
        prevRunningTicks = 0;
    }

    // 0 = fully up, 1 = bottom of stroke. renderer multiplies by travel distance.
    public float getRenderedHeadOffset(float partialTicks) {
        if (!running) {
            return 0.0F;
        }
        int ticksNow = Math.abs(runningTicks);
        float ticks = Mth.lerp(partialTicks, prevRunningTicks, ticksNow);
        if (ticksNow < (CYCLE * 2) / 3) {
            return (float) Mth.clamp(Math.pow(ticks / CYCLE * 2, 3), 0.0, 1.0);
        }
        return Mth.clamp((CYCLE - ticks) / CYCLE * 3, 0.0F, 1.0F);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HydraulicPressBlockEntity press) {
        if (level.isClientSide) {
            press.clientTick();
        } else {
            press.serverTick(level, pos);
        }
    }

    private void clientTick() {
        tickFlash();
        tickStallSmoke();
        if (!running) {
            return;
        }
        prevRunningTicks = runningTicks;
        runningTicks += PRESS_SPEED;
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
            runningTicks += PRESS_SPEED;
            if (prevRunningTicks < CYCLE / 2 && runningTicks >= CYCLE / 2) {
                applyPress(level, pos, available);
                AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playOnServer(level, pos, 0.5F, 0.875F);
            }
            if (runningTicks > CYCLE) {
                if (evaluateAndUpdate(level, pos, available)) {
                    start();
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
            start();
        }
    }

    private void start() {
        running = true;
        runningTicks = 0;
        prevRunningTicks = 0;
        overpressured = false;
        underpressured = false;
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.7F);
        }
        sync();
    }

    private void stop() {
        running = false;
        runningTicks = 0;
        prevRunningTicks = 0;
        cooldown = PRESS_COOLDOWN;
        sync();
    }

    private float readAvailablePressure(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos.above()) instanceof HardenedIronPipeBlockEntity pipe
                && !pipe.isFlowBlocked() && pipe.connectsOnSide(Direction.DOWN)) {
            return pipe.getPressure();
        }
        return 0.0F;
    }

    private boolean hasPressableItemBelow(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos.below()) instanceof DepotBlockEntity depot) {
            ItemStack held = depot.getHeldItem();
            return !held.isEmpty() && findRecipe(level, held).isPresent();
        }
        return false;
    }

    private Optional<RecipeHolder<HydraulicPressRecipe>> findRecipe(Level level, ItemStack stack) {
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.HYDRAULIC_PRESSING.get(), new SingleRecipeInput(stack), level);
    }

    private enum RunState { READY, NO_WORK, NO_FLUID, UNDERPRESSURED, OVERPRESSURED }

    // aqueductIntakes = early-game throttle count; activeSources = "is anyone even feeding this line?"
    private record NetworkStatus(int aqueductIntakes, int activeSources, float totalDemand) {}

    private RunState evaluate(Level level, BlockPos pos, float available) {
        NetworkStatus net = scanNetwork(level, pos.above());
        if (net.aqueductIntakes() > MAX_INTAKES) {
            return RunState.OVERPRESSURED;
        }
        if (net.activeSources() == 0) {
            return RunState.NO_WORK;
        }
        if (net.totalDemand() > available) {
            return RunState.UNDERPRESSURED;
        }
        if (!hasPressableItemBelow(level, pos)) {
            return RunState.NO_WORK;
        }
        if (tank.getFluid().isEmpty()) {
            return RunState.NO_FLUID;
        }
        return RunState.READY;
    }

    private boolean evaluateAndUpdate(Level level, BlockPos pos, float available) {
        RunState state = evaluate(level, pos, available);
        boolean nowOver = state == RunState.OVERPRESSURED;
        boolean nowUnder = state == RunState.UNDERPRESSURED;
        if (nowOver != overpressured || nowUnder != underpressured) {
            overpressured = nowOver;
            underpressured = nowUnder;
            sync();
        }
        return state == RunState.READY;
    }

    // BFS over the pipe network — counts aqueduct intakes, any active sources, and total press demand
    private NetworkStatus scanNetwork(Level level, BlockPos pipeStart) {
        if (!(level.getBlockEntity(pipeStart) instanceof HardenedIronPipeBlockEntity startPipe)
                || startPipe.isFlowBlocked() || !startPipe.connectsOnSide(Direction.DOWN)) {
            // No pipe feeding us: this press is an island — its own load with no source behind it (0 supply).
            return new NetworkStatus(0, 0, PRESS_PU_COST);
        }

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(pipeStart);
        queue.add(pipeStart);

        int aqueducts = 0;
        int sources = 0;
        float demand = 0.0F;
        int budget = NETWORK_SCAN_LIMIT;
        while (!queue.isEmpty() && budget-- > 0) {
            BlockPos p = queue.poll();
            if (!(level.getBlockEntity(p) instanceof HardenedIronPipeBlockEntity pipe)) {
                continue;
            }
            // aqueduct/press hang *under* the pipe; hydrostatic sits *above* — check both or hydrostatic lines never work
            BlockPos below = p.below();
            BlockEntity device = level.getBlockEntity(below);
            if (device instanceof AqueductIntakeBlockEntity) {
                BlockState ds = level.getBlockState(below);
                if (ds.hasProperty(AqueductIntakeBlock.ACTIVE) && ds.getValue(AqueductIntakeBlock.ACTIVE)) {
                    aqueducts++;
                    sources++;
                }
            } else if (device instanceof CentrifugalCompressorBlockEntity compressor) {
                // compressor counts as a source but NOT toward the aqueduct overpressure throttle (it's kinetic)
                if (compressor.isCompressing()) {
                    sources++;
                }
            } else if (device instanceof HydraulicPressBlockEntity) {
                demand += PRESS_PU_COST;
            }
            if (level.getBlockEntity(p.above()) instanceof HydrostaticIntakeBlockEntity) {
                BlockState us = level.getBlockState(p.above());
                if (us.hasProperty(HydrostaticIntakeBlock.ACTIVE) && us.getValue(HydrostaticIntakeBlock.ACTIVE)) {
                    sources++;
                }
            }
            for (Direction dir : Direction.values()) {
                if (!pipe.connectsOnSide(dir)) {
                    continue;
                }
                BlockPos np = p.relative(dir);
                if (visited.contains(np)) {
                    continue;
                }
                if (!(level.getBlockEntity(np) instanceof HardenedIronPipeBlockEntity nb)) {
                    continue;
                }
                if (nb.isFlowBlocked() || !nb.connectsOnSide(dir.getOpposite())) {
                    continue;
                }
                visited.add(np);
                queue.add(np);
            }
        }
        return new NetworkStatus(aqueducts, sources, demand);
    }

    private void tickFlash() {
        boolean stalled = overpressured || underpressured;
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
        if (level == null || !(overpressured || underpressured)) {
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

    // one item per stroke, results stay on the depot — same as Mechanical Press behaviour
    private void applyPress(Level level, BlockPos pos, float available) {
        if (available < PRESS_PU_COST || tank.getFluid().isEmpty()) {
            stop();
            return;
        }
        BlockPos depotPos = pos.below();
        if (!(level.getBlockEntity(depotPos) instanceof DepotBlockEntity depot)) {
            return;
        }
        ItemStack held = depot.getHeldItem();
        if (held.isEmpty()) {
            return;
        }
        Optional<RecipeHolder<HydraulicPressRecipe>> recipe = findRecipe(level, held);
        if (recipe.isEmpty()) {
            return;
        }
        TransportedItemStackHandlerBehaviour handler =
                BlockEntityBehaviour.get(level, depotPos, TransportedItemStackHandlerBehaviour.TYPE);
        if (handler == null) {
            return;
        }

        HydraulicPressRecipe value = recipe.get().value();
        handler.handleProcessingOnAllItems(transported -> {
            ItemStack input = transported.stack;
            if (input.isEmpty()) {
                return TransportedResult.doNothing();
            }
            ItemStack pressed = value.assemble(new SingleRecipeInput(input), level.registryAccess());
            TransportedItemStack out = transported.copy();
            out.stack = pressed;
            if (input.getCount() <= 1) {
                return TransportedResult.convertTo(out);
            }
            TransportedItemStack leftover = transported.copy();
            leftover.stack = input.copyWithCount(input.getCount() - 1);
            return TransportedResult.convertToAndLeaveHeld(List.of(out), leftover);
        });
        sync();
    }

    private void updateFullState() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        boolean shouldBeFull = !tank.getFluid().isEmpty();
        if (state.hasProperty(HydraulicPressBlock.FULL) && state.getValue(HydraulicPressBlock.FULL) != shouldBeFull) {
            level.setBlock(worldPosition, state.setValue(HydraulicPressBlock.FULL, shouldBeFull), Block.UPDATE_ALL);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        lang().translate("gui.goggles.hydraulic_press").forGoggles(tooltip);

        if (overpressured) {
            lang().add(dot(ChatFormatting.RED))
                    .add(lang().translate("gui.hydraulic_press.overpressured").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else if (underpressured) {
            lang().add(dot(ChatFormatting.RED))
                    .add(lang().translate("gui.hydraulic_press.underpressured").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else if (running) {
            lang().add(dot(ChatFormatting.GREEN))
                    .add(lang().translate("gui.hydraulic_press.pressing").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else {
            lang().add(dot(ChatFormatting.GRAY))
                    .add(lang().translate("gui.hydraulic_press.idle").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        }

        lang().add(lang().translate("gui.hydraulic_press.pressure").style(ChatFormatting.GRAY))
                .space()
                .add(lang().text(LangNumberFormat.format(PRESS_PU_COST)).style(ChatFormatting.GOLD))
                .add(lang().text(ChatFormatting.DARK_GRAY, " PU"))
                .forGoggles(tooltip, 1);

        lang().add(lang().translate("gui.hydraulic_press.fluid").style(ChatFormatting.GRAY))
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
        tag.putBoolean("Overpressured", overpressured);
        tag.putBoolean("Underpressured", underpressured);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        running = tag.getBoolean("Running");
        runningTicks = tag.getInt("RunningTicks");
        prevRunningTicks = runningTicks;
        overpressured = tag.getBoolean("Overpressured");
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
