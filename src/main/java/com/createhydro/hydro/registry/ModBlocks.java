package com.createhydro.hydro.registry;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.AqueductIntakeBlock;
import com.createhydro.hydro.block.CentrifugalCompressorBlock;
import com.createhydro.hydro.block.FlowGaugeBlock;
import com.createhydro.hydro.block.HardenedIronPipeBlock;
import com.createhydro.hydro.block.HydraulicAssemblyUnitBlock;
import com.createhydro.hydro.block.HydraulicDrillBlock;
import com.createhydro.hydro.block.HydraulicFistBlock;
import com.createhydro.hydro.block.HydraulicMotorBlock;
import com.createhydro.hydro.block.HydraulicPressBlock;
import com.createhydro.hydro.block.HydrostaticIntakeBlock;
import com.createhydro.hydro.block.PressureValveBlock;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All blocks added by Create: Hydraulics. */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateHydraulics.MODID);

    public static final DeferredBlock<HardenedIronPipeBlock> HARDENED_IRON_PIPE = BLOCKS.register(
            "hardened_iron_pipe",
            () -> new HardenedIronPipeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion() // not a full cube; neighbouring faces need to still render
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<PressureValveBlock> PRESSURE_VALVE = BLOCKS.register(
            "pressure_valve",
            () -> new PressureValveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<FlowGaugeBlock> FLOW_GAUGE = BLOCKS.register(
            "flow_gauge",
            () -> new FlowGaugeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<AqueductIntakeBlock> AQUEDUCT_INTAKE = BLOCKS.register(
            "aqueduct_intake",
            () -> new AqueductIntakeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HydrostaticIntakeBlock> HYDROSTATIC_INTAKE = BLOCKS.register(
            "hydrostatic_intake",
            () -> new HydrostaticIntakeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<CentrifugalCompressorBlock> CENTRIFUGAL_COMPRESSOR = BLOCKS.register(
            "centrifugal_compressor",
            () -> new CentrifugalCompressorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion() // the rotating shaft is drawn by a BER
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HydraulicPressBlock> HYDRAULIC_PRESS = BLOCKS.register(
            "hydraulic_press",
            () -> new HydraulicPressBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion() // the moving head is drawn by a BER
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HydraulicMotorBlock> HYDRAULIC_MOTOR = BLOCKS.register(
            "hydraulic_motor",
            () -> new HydraulicMotorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion() // the rotating shaft is drawn by a BER
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HydraulicFistBlock> HYDRAULIC_FIST = BLOCKS.register(
            "hydraulic_fist",
            () -> new HydraulicFistBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion() // the punching fist is drawn by a BER
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HydraulicDrillBlock> HYDRAULIC_DRILL = BLOCKS.register(
            "hydraulic_drill",
            () -> new HydraulicDrillBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion() // the breaker head is drawn by a BER
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HydraulicAssemblyUnitBlock> HYDRAULIC_ASSEMBLY_UNIT = BLOCKS.register(
            "hydraulic_assembly_unit",
            () -> new HydraulicAssemblyUnitBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<LiquidBlock> HYDRO_FLUID_BLOCK = BLOCKS.register(
            "hydro_fluid",
            () -> new LiquidBlock(ModFluids.HYDRO_FLUID.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()
                    .sound(SoundType.EMPTY)));

    private ModBlocks() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
