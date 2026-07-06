package com.createhydro.hydro.block.entity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.Config;
import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.HydraulicFistBlock;
import com.createhydro.hydro.recipe.HydraulicFistRecipe;
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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
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

// extends a fist from its front face when pressure and fluid are available; crushes blocks and belt items.
public class HydraulicFistBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    public static final int CYCLE = 240;
    private static final int PUNCH_SPEED = 6; // 40 ticks per punch, ~2s

    public static final int TANK_CAPACITY = 1000;
    private static final int PUNCH_COOLDOWN = 40;
    private static final int IDLE_SCAN = 8;

    private static final float DEPOT_PUNCH_DEPTH = 3.0F / 16.0F;
    private static final float BELT_PUNCH_DEPTH = 4.0F / 16.0F;
    private static final float BLOCK_PUNCH_DEPTH = 2.0F / 16.0F;

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
    private float punchTravel = 0.0F;
    private boolean underpressured = false;
    private int idleCooldown = IDLE_SCAN;
    private int cooldown = 0;
    // weak keys so crushed results disappear from tracking once the item entity is gone
    private final Set<TransportedItemStack> alreadyCrushed = Collections.newSetFromMap(new WeakHashMap<>());

    // client-side flash: >0 = red, <0 = green, decays to 0
    private float overstressEffect = 0.0F;
    private float prevOverstressEffect = 0.0F;
    private boolean wasStalledClient = false;
    private int stallSmokeTimer = 0;

    public HydraulicFistBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRAULIC_FIST.get(), pos, state);
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

    public float getPunchTravel() {
        return punchTravel;
    }

    private float punchDepthFor(Level level, @Nullable Target target) {
        if (target == null) {
            return 0.0F;
        }
        float withinBlock;
        if (target.type() == TargetType.ITEM) {
            withinBlock = level.getBlockEntity(target.pos()) instanceof DepotBlockEntity
                    ? DEPOT_PUNCH_DEPTH : BELT_PUNCH_DEPTH;
        } else {
            withinBlock = BLOCK_PUNCH_DEPTH;
        }
        return (target.distance() - 1) + withinBlock;
    }

    // ponder worlds are client-side; scene calls this directly
    public void startPonderPunch(float travel) {
        running = true;
        runningTicks = 0;
        prevRunningTicks = 0;
        punchTravel = travel;
        underpressured = false;
    }

    // 0 = retracted, 1 = fully extended. renderer multiplies by punchTravel.
    public float getRenderedReach(float partialTicks) {
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

    public static void tick(Level level, BlockPos pos, BlockState state, HydraulicFistBlockEntity fist) {
        if (level.isClientSide) {
            fist.clientTick();
        } else {
            fist.serverTick(level, pos);
        }
    }

    private void clientTick() {
        tickFlash();
        tickStallSmoke();
        if (!running) {
            return;
        }
        prevRunningTicks = runningTicks;
        runningTicks += PUNCH_SPEED;
        if (runningTicks > CYCLE) {
            running = false;
            runningTicks = 0;
            prevRunningTicks = 0;
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        float available = readAvailablePressure(level, pos);

        holdWorkpiece(level, pos, available);

        if (running) {
            prevRunningTicks = runningTicks;
            runningTicks += PUNCH_SPEED;
            if (prevRunningTicks < CYCLE / 2 && runningTicks >= CYCLE / 2) {
                applyPunch(level, pos, available);
                AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playOnServer(level, pos, 0.6F, 0.7F);
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
        Target target = findTarget(level, pos);
        punchTravel = punchDepthFor(level, target);
        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.6F);
        }
        sync();
    }

    private void stop() {
        running = false;
        runningTicks = 0;
        prevRunningTicks = 0;
        cooldown = PUNCH_COOLDOWN;
        sync();
    }

    @Nullable
    private HardenedIronPipeBlockEntity backPipe(Level level, BlockPos pos) {
        Direction facing = getBlockState().getValue(HydraulicFistBlock.FACING);
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

    private enum TargetType { BLOCK, ITEM }

    private record Target(int distance, BlockPos pos, TargetType type) {}

    @Nullable
    private Target findTarget(Level level, BlockPos pos) {
        Direction facing = getBlockState().getValue(HydraulicFistBlock.FACING);
        int reach = Config.FIST_REACH.get();
        for (int k = 1; k <= reach; k++) {
            BlockPos tp = pos.relative(facing, k);

            if (BlockEntityBehaviour.get(level, tp, TransportedItemStackHandlerBehaviour.TYPE) != null) {
                return new Target(k, tp, TargetType.ITEM);
            }

            BlockState bs = level.getBlockState(tp);
            if (!bs.isAir()) {
                if (blockCrushResult(level, bs).isPresent()) {
                    return new Target(k, tp, TargetType.BLOCK);
                }
                if (!bs.getCollisionShape(level, tp).isEmpty()) {
                    return null;
                }
            }
        }
        return null;
    }

    private Optional<Block> blockCrushResult(Level level, BlockState state) {
        ItemStack asItem = new ItemStack(state.getBlock());
        if (asItem.isEmpty()) {
            return Optional.empty();
        }
        return findRecipe(level, asItem)
                .map(holder -> holder.value().getResultItem(level.registryAccess()).getItem())
                .filter(item -> item instanceof BlockItem)
                .map(item -> ((BlockItem) item).getBlock());
    }

    private Optional<RecipeHolder<HydraulicFistRecipe>> findRecipe(Level level, ItemStack stack) {
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.HYDRAULIC_FISTING.get(), new SingleRecipeInput(stack), level);
    }

    private boolean hasCrushableItem(Level level, BlockPos tp) {
        TransportedItemStackHandlerBehaviour handler =
                BlockEntityBehaviour.get(level, tp, TransportedItemStackHandlerBehaviour.TYPE);
        if (handler == null) {
            return false;
        }
        boolean[] found = {false};
        // doNothing() is a safe read-only peek — doesn't move anything
        handler.handleProcessingOnAllItems(transported -> {
            if (!found[0] && !alreadyCrushed.contains(transported)
                    && findRecipe(level, transported.stack).isPresent()) {
                found[0] = true;
            }
            return TransportedResult.doNothing();
        });
        return found[0];
    }

    private void holdWorkpiece(Level level, BlockPos pos, float available) {
        if (available < (float) (double) Config.FIST_PRESSURE.get() || tank.getFluid().isEmpty()) {
            return;
        }
        Target target = findTarget(level, pos);
        if (target == null || target.type() != TargetType.ITEM) {
            return;
        }
        TransportedItemStackHandlerBehaviour handler =
                BlockEntityBehaviour.get(level, target.pos(), TransportedItemStackHandlerBehaviour.TYPE);
        if (handler == null) {
            return;
        }
        handler.handleProcessingOnAllItems(transported -> {
            if (!alreadyCrushed.contains(transported) && findRecipe(level, transported.stack).isPresent()) {
                transported.lockedExternally = true;
            }
            return TransportedResult.doNothing();
        });
    }

    private enum RunState { READY, NO_WORK, NO_FLUID, UNDERPRESSURED }

    private RunState evaluate(Level level, BlockPos pos, float available) {
        float required = (float) (double) Config.FIST_PRESSURE.get();
        boolean lineConnected = hasPressureLine(level, pos);
        if (lineConnected && available < required) {
            return RunState.UNDERPRESSURED;
        }
        if (tank.getFluid().isEmpty()) {
            return RunState.NO_FLUID;
        }
        if (!lineConnected) {
            return RunState.NO_WORK;
        }
        Target target = findTarget(level, pos);
        boolean hasWork = target != null
                && (target.type() == TargetType.BLOCK || hasCrushableItem(level, target.pos()));
        if (!hasWork) {
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

    private void applyPunch(Level level, BlockPos pos, float available) {
        if (available < (float) (double) Config.FIST_PRESSURE.get() || tank.getFluid().isEmpty()) {
            stop();
            return;
        }
        Target target = findTarget(level, pos);
        if (target == null) {
            return;
        }
        if (target.type() == TargetType.BLOCK) {
            crushBlock(level, target.pos());
        } else {
            crushItems(level, target.pos());
        }
    }

    private void crushBlock(Level level, BlockPos tp) {
        BlockState state = level.getBlockState(tp);
        Optional<Block> result = blockCrushResult(level, state);
        if (result.isEmpty()) {
            return;
        }
        level.setBlock(tp, result.get().defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, tp, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.6F, 0.85F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    tp.getX() + 0.5, tp.getY() + 0.5, tp.getZ() + 0.5, 12, 0.25, 0.25, 0.25, 0.0);
        }
    }

    // one item per punch, results flagged so they don't get crushed again
    private void crushItems(Level level, BlockPos tp) {
        TransportedItemStackHandlerBehaviour handler =
                BlockEntityBehaviour.get(level, tp, TransportedItemStackHandlerBehaviour.TYPE);
        if (handler == null) {
            return;
        }
        handler.handleProcessingOnAllItems(transported -> {
            if (alreadyCrushed.contains(transported)) {
                return TransportedResult.doNothing();
            }
            ItemStack input = transported.stack;
            if (input.isEmpty()) {
                return TransportedResult.doNothing();
            }
            Optional<RecipeHolder<HydraulicFistRecipe>> recipe = findRecipe(level, input);
            if (recipe.isEmpty()) {
                return TransportedResult.doNothing();
            }
            ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(input), level.registryAccess());
            if (result.isEmpty()) {
                return TransportedResult.doNothing();
            }
            TransportedItemStack out = transported.copy();
            out.stack = result;
            alreadyCrushed.add(out);
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
        if (state.hasProperty(HydraulicFistBlock.FULL) && state.getValue(HydraulicFistBlock.FULL) != shouldBeFull) {
            level.setBlock(worldPosition, state.setValue(HydraulicFistBlock.FULL, shouldBeFull), Block.UPDATE_ALL);
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
        lang().translate("gui.goggles.hydraulic_fist").forGoggles(tooltip);

        if (underpressured) {
            lang().add(dot(ChatFormatting.RED))
                    .add(lang().translate("gui.hydraulic_fist.underpressured").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else if (running) {
            lang().add(dot(ChatFormatting.GREEN))
                    .add(lang().translate("gui.hydraulic_fist.punching").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else if (tank.getFluid().isEmpty()) {
            lang().add(dot(ChatFormatting.GRAY))
                    .add(lang().translate("gui.hydraulic_fist.no_fluid").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else {
            lang().add(dot(ChatFormatting.GRAY))
                    .add(lang().translate("gui.hydraulic_fist.idle").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        }

        lang().add(lang().translate("gui.hydraulic_fist.pressure").style(ChatFormatting.GRAY))
                .space()
                .add(lang().text(LangNumberFormat.format((double) Config.FIST_PRESSURE.get())).style(ChatFormatting.GOLD))
                .add(lang().text(ChatFormatting.DARK_GRAY, " PU"))
                .forGoggles(tooltip, 1);

        lang().add(lang().translate("gui.hydraulic_fist.fluid").style(ChatFormatting.GRAY))
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
        tag.putFloat("PunchTravel", punchTravel);
        tag.putBoolean("Underpressured", underpressured);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        running = tag.getBoolean("Running");
        runningTicks = tag.getInt("RunningTicks");
        prevRunningTicks = runningTicks;
        punchTravel = tag.getFloat("PunchTravel");
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
