package com.createhydro.hydro.block.entity;

import java.util.List;

import com.createhydro.hydro.Config;
import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.HydraulicMotorBlock;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.createhydro.hydro.registry.ModBlocks;
import com.createhydro.hydro.registry.ModFluids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

// reads pressure from the pipe behind it and spins the shaft in front. stalls if the line can't keep up.
public class HydraulicMotorBlockEntity extends GeneratingKineticBlockEntity {

    public static final int TANK_CAPACITY = 2000;
    public static final int DEFAULT_RPM = 16;
    public static final int MAX_RPM = 256;

    public ScrollValueBehaviour generatedSpeed;

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

    private boolean active = false;
    private boolean underpressured = false;
    private int fuelTimer = 0;

    public HydraulicMotorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRAULIC_MOTOR.get(), pos, state);
    }

    public FluidTank getFluidTank() {
        return tank;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isUnderpressured() {
        return underpressured;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        generatedSpeed = new KineticScrollValueBehaviour(
                Component.translatable("createhydraulics.kinetics.hydraulic_motor.rotation_speed"),
                this, new MotorValueBox());
        generatedSpeed.between(-MAX_RPM, MAX_RPM);
        generatedSpeed.value = DEFAULT_RPM;
        generatedSpeed.withCallback(i -> updateGeneratedRotation());
        behaviours.add(generatedSpeed);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) {
            updateGeneratedRotation();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (!ModBlocks.HYDRAULIC_MOTOR.get().equals(getBlockState().getBlock())) {
            return 0;
        }
        if (!active) {
            return 0;
        }
        return convertToDirection(generatedSpeed.getValue(), getBlockState().getValue(HydraulicMotorBlock.FACING));
    }

    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = 0;
        return 0;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = (float) (double) Config.MOTOR_STRESS_PER_RPM.get();
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    public static float requiredPressure(int rpm) {
        return (float) (Config.MOTOR_PRESSURE_PER_RPM.get() * Math.abs(rpm));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        serverTick();
    }

    private void serverTick() {
        int rpm = generatedSpeed.getValue();
        float available = readAvailablePressure();

        boolean hasFluid = !tank.getFluid().isEmpty();
        boolean wantsToRun = rpm != 0 && hasFluid;
        boolean hasPressure = available >= requiredPressure(rpm);
        boolean shouldRun = wantsToRun && hasPressure;

        if (shouldRun) {
            if (++fuelTimer >= 20) {
                fuelTimer = 0;
                int perSecond = Config.MOTOR_FLUID_PER_SECOND.get();
                if (perSecond > 0) {
                    tank.drain(perSecond, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        } else {
            fuelTimer = 0;
        }

        if (shouldRun != active) {
            active = shouldRun;
            updateGeneratedRotation(); // pushes the new speed (or 0) through the kinetic network
        }

        // underpressured = pipe connected but can't carry the load. no pipe at all = just unpowered.
        boolean nowUnder = rpm != 0 && hasPressureLine() && !hasPressure;
        if (nowUnder != underpressured) {
            underpressured = nowUnder;
            sendData();
        }
    }

    private boolean hasPressureLine() {
        Direction back = getBlockState().getValue(HydraulicMotorBlock.FACING).getOpposite();
        BlockPos backPos = worldPosition.relative(back);
        return level.getBlockEntity(backPos) instanceof HardenedIronPipeBlockEntity pipe
                && !pipe.isFlowBlocked() && pipe.connectsOnSide(back.getOpposite());
    }

    private float readAvailablePressure() {
        Direction back = getBlockState().getValue(HydraulicMotorBlock.FACING).getOpposite();
        BlockPos backPos = worldPosition.relative(back);
        if (level.getBlockEntity(backPos) instanceof HardenedIronPipeBlockEntity pipe
                && !pipe.isFlowBlocked() && pipe.connectsOnSide(back.getOpposite())) {
            return pipe.getPressure();
        }
        return 0.0F;
    }

    private void updateFullState() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        boolean shouldBeFull = !tank.getFluid().isEmpty();
        if (state.hasProperty(HydraulicMotorBlock.FULL) && state.getValue(HydraulicMotorBlock.FULL) != shouldBeFull) {
            level.setBlock(worldPosition, state.setValue(HydraulicMotorBlock.FULL, shouldBeFull), Block.UPDATE_ALL);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        float available = level != null ? readAvailablePressure() : 0.0F;

        lang().translate("gui.goggles.hydraulic_motor").forGoggles(tooltip);

        if (underpressured) {
            lang().add(dot(ChatFormatting.RED))
                    .add(lang().translate("gui.hydraulic_motor.underpressured").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else if (active) {
            lang().add(dot(ChatFormatting.GREEN))
                    .add(lang().translate("gui.hydraulic_motor.running").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else if (tank.getFluid().isEmpty()) {
            lang().add(dot(ChatFormatting.GRAY))
                    .add(lang().translate("gui.hydraulic_motor.no_fluid").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        } else {
            lang().add(dot(ChatFormatting.GRAY))
                    .add(lang().translate("gui.hydraulic_motor.idle").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        }

        lang().add(lang().translate("gui.hydraulic_motor.pressure").style(ChatFormatting.GRAY))
                .space()
                .add(lang().text(LangNumberFormat.format(available)).style(ChatFormatting.GOLD))
                .add(lang().text(ChatFormatting.DARK_GRAY,
                        " / " + LangNumberFormat.format(requiredPressure(generatedSpeed.getValue())) + " PU"))
                .forGoggles(tooltip, 1);

        lang().add(lang().translate("gui.hydraulic_motor.fluid").style(ChatFormatting.GRAY))
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
        compound.putBoolean("Active", active);
        compound.putBoolean("Underpressured", underpressured);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        tank.readFromNBT(registries, compound.getCompound("Tank"));
        active = compound.getBoolean("Active");
        underpressured = compound.getBoolean("Underpressured");
    }

    // keeps the RPM scroll box on the motor's body surface instead of floating in mid-air
    private static class MotorValueBox extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 12.5);
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction facing = state.getValue(HydraulicMotorBlock.FACING);
            return super.getLocalOffset(level, pos, state)
                    .add(Vec3.atLowerCornerOf(facing.getNormal()).scale(-1 / 16f));
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            super.rotate(level, pos, state, ms);
            Direction facing = state.getValue(HydraulicMotorBlock.FACING);
            if (facing.getAxis() == Axis.Y) {
                return;
            }
            if (getSide() != Direction.UP) {
                return;
            }
            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            Direction facing = state.getValue(HydraulicMotorBlock.FACING);
            if (facing.getAxis() != Axis.Y && direction == Direction.DOWN) {
                return false;
            }
            return direction.getAxis() != facing.getAxis();
        }
    }
}
