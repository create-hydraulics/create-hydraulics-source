package com.createhydro.hydro.block.entity;

import java.util.List;

import com.createhydro.hydro.Config;
import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.CentrifugalCompressorBlock;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.createhydro.hydro.registry.ModFluids;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

// kinetic-driven pressure source. consumes SU off the rotation network, outputs PU into the pipe above.
public class CentrifugalCompressorBlockEntity extends KineticBlockEntity {

    public static final int TANK_CAPACITY = 1000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                updateFullState();
                sendData();
            }
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ModFluids.HYDRO_FLUID.get()
                    || stack.getFluid() == ModFluids.HYDRO_FLUID_FLOWING.get();
        }
    };

    public CentrifugalCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CENTRIFUGAL_COMPRESSOR.get(), pos, state);
    }

    public FluidTank getFluidTank() {
        return tank;
    }

    public boolean hasFluid() {
        return !tank.getFluid().isEmpty();
    }

    // PU output at the given shaft speed, clamped to MAX_PRESSURE
    public static float outputFor(float rpm) {
        float output = (float) (Config.COMPRESSOR_PRESSURE_PER_RPM.get() * Math.abs(rpm));
        return Math.min(output, HardenedIronPipeBlockEntity.MAX_PRESSURE);
    }

    public boolean isCompressing() {
        float speed = getSpeed();
        return hasFluid() && speed != 0 && outputFor(speed) > 0.0F;
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) (double) Config.COMPRESSOR_STRESS_IMPACT.get();
        this.lastStressApplied = impact;
        return impact;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        float speed = getSpeed();
        float output = (hasFluid() && speed != 0) ? outputFor(speed) : 0.0F;
        boolean compressing = output > 0.0F;

        if (!compressing) {
            return;
        }

        // output is top-face only; check the pipe above actually connects down into us
        BlockPos outputPos = worldPosition.above();
        if (level.getBlockEntity(outputPos) instanceof HardenedIronPipeBlockEntity pipe
                && !pipe.isFlowBlocked() && pipe.connectsOnSide(Direction.DOWN)) {
            HardenedIronPipeBlockEntity.floodFromSource(level, outputPos, output);
        }
    }

    private void updateFullState() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        boolean shouldBeFull = hasFluid();
        if (state.hasProperty(CentrifugalCompressorBlock.FULL)
                && state.getValue(CentrifugalCompressorBlock.FULL) != shouldBeFull) {
            level.setBlock(worldPosition, state.setValue(CentrifugalCompressorBlock.FULL, shouldBeFull),
                    Block.UPDATE_ALL);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // kinetic stats (stress impact + speed) come first — this is where the SU cost shows up
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        float speed = getSpeed();
        boolean fuelled = hasFluid();
        boolean spinning = speed != 0 && outputFor(speed) > 0.0F;

        lang().translate("gui.goggles.centrifugal_compressor").forGoggles(tooltip);

        if (!fuelled) {
            lang().add(dot(ChatFormatting.GRAY))
                    .add(lang().translate("gui.centrifugal_compressor.no_fluid").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else if (spinning) {
            lang().add(dot(ChatFormatting.GREEN))
                    .add(lang().translate("gui.centrifugal_compressor.compressing").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
            lang().add(lang().translate("gui.centrifugal_compressor.output").style(ChatFormatting.GRAY))
                    .space()
                    .add(lang().text(LangNumberFormat.format(outputFor(speed))).style(ChatFormatting.GOLD))
                    .add(lang().text(ChatFormatting.DARK_GRAY, " PU/s"))
                    .forGoggles(tooltip, 1);
        } else {
            lang().add(dot(ChatFormatting.RED))
                    .add(lang().translate("gui.centrifugal_compressor.idle").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        }

        lang().add(lang().translate("gui.centrifugal_compressor.fluid").style(ChatFormatting.GRAY))
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

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        tank.readFromNBT(registries, compound.getCompound("Tank"));
    }
}
