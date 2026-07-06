package com.createhydro.hydro.registry;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.entity.AqueductIntakeBlockEntity;
import com.createhydro.hydro.block.entity.CentrifugalCompressorBlockEntity;
import com.createhydro.hydro.block.entity.FlowGaugeBlockEntity;
import com.createhydro.hydro.block.entity.HardenedIronPipeBlockEntity;
import com.createhydro.hydro.block.entity.HydraulicAssemblyUnitBlockEntity;
import com.createhydro.hydro.block.entity.HydraulicDrillBlockEntity;
import com.createhydro.hydro.block.entity.HydraulicFistBlockEntity;
import com.createhydro.hydro.block.entity.HydraulicMotorBlockEntity;
import com.createhydro.hydro.block.entity.HydraulicPressBlockEntity;
import com.createhydro.hydro.block.entity.HydrostaticIntakeBlockEntity;
import com.createhydro.hydro.block.entity.PressureValveBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All block entity types added by Create: Hydraulics. */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateHydraulics.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HardenedIronPipeBlockEntity>> HARDENED_IRON_PIPE =
            BLOCK_ENTITIES.register("hardened_iron_pipe",
                    () -> BlockEntityType.Builder.of(HardenedIronPipeBlockEntity::new, ModBlocks.HARDENED_IRON_PIPE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressureValveBlockEntity>> PRESSURE_VALVE =
            BLOCK_ENTITIES.register("pressure_valve",
                    () -> BlockEntityType.Builder.of(PressureValveBlockEntity::new, ModBlocks.PRESSURE_VALVE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlowGaugeBlockEntity>> FLOW_GAUGE =
            BLOCK_ENTITIES.register("flow_gauge",
                    () -> BlockEntityType.Builder.of(FlowGaugeBlockEntity::new, ModBlocks.FLOW_GAUGE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AqueductIntakeBlockEntity>> AQUEDUCT_INTAKE =
            BLOCK_ENTITIES.register("aqueduct_intake",
                    () -> BlockEntityType.Builder.of(AqueductIntakeBlockEntity::new, ModBlocks.AQUEDUCT_INTAKE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugalCompressorBlockEntity>> CENTRIFUGAL_COMPRESSOR =
            BLOCK_ENTITIES.register("centrifugal_compressor",
                    () -> BlockEntityType.Builder.of(CentrifugalCompressorBlockEntity::new, ModBlocks.CENTRIFUGAL_COMPRESSOR.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HydraulicPressBlockEntity>> HYDRAULIC_PRESS =
            BLOCK_ENTITIES.register("hydraulic_press",
                    () -> BlockEntityType.Builder.of(HydraulicPressBlockEntity::new, ModBlocks.HYDRAULIC_PRESS.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HydrostaticIntakeBlockEntity>> HYDROSTATIC_INTAKE =
            BLOCK_ENTITIES.register("hydrostatic_intake",
                    () -> BlockEntityType.Builder.of(HydrostaticIntakeBlockEntity::new, ModBlocks.HYDROSTATIC_INTAKE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HydraulicMotorBlockEntity>> HYDRAULIC_MOTOR =
            BLOCK_ENTITIES.register("hydraulic_motor",
                    () -> BlockEntityType.Builder.of(HydraulicMotorBlockEntity::new, ModBlocks.HYDRAULIC_MOTOR.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HydraulicFistBlockEntity>> HYDRAULIC_FIST =
            BLOCK_ENTITIES.register("hydraulic_fist",
                    () -> BlockEntityType.Builder.of(HydraulicFistBlockEntity::new, ModBlocks.HYDRAULIC_FIST.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HydraulicDrillBlockEntity>> HYDRAULIC_DRILL =
            BLOCK_ENTITIES.register("hydraulic_drill",
                    () -> BlockEntityType.Builder.of(HydraulicDrillBlockEntity::new, ModBlocks.HYDRAULIC_DRILL.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HydraulicAssemblyUnitBlockEntity>> HYDRAULIC_ASSEMBLY_UNIT =
            BLOCK_ENTITIES.register("hydraulic_assembly_unit",
                    () -> BlockEntityType.Builder.of(HydraulicAssemblyUnitBlockEntity::new, ModBlocks.HYDRAULIC_ASSEMBLY_UNIT.get())
                            .build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
