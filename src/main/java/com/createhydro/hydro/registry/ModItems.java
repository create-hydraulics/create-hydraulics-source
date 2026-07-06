package com.createhydro.hydro.registry;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.item.HydroFluidBucketItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All items added by Create: Hydraulics. */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateHydraulics.MODID);

    /** The BlockItem used to place the Hardened Iron Pipe. */
    public static final DeferredItem<BlockItem> HARDENED_IRON_PIPE =
            ITEMS.registerSimpleBlockItem(ModBlocks.HARDENED_IRON_PIPE);

    /** The BlockItem used to place the Pressure Valve. */
    public static final DeferredItem<BlockItem> PRESSURE_VALVE =
            ITEMS.registerSimpleBlockItem(ModBlocks.PRESSURE_VALVE);

    /** The BlockItem used to place the Flow Gauge. */
    public static final DeferredItem<BlockItem> FLOW_GAUGE =
            ITEMS.registerSimpleBlockItem(ModBlocks.FLOW_GAUGE);

    /** The BlockItem used to place the Aqueduct Intake. */
    public static final DeferredItem<BlockItem> AQUEDUCT_INTAKE =
            ITEMS.registerSimpleBlockItem(ModBlocks.AQUEDUCT_INTAKE);

    /** The BlockItem used to place the Centrifugal Compressor. */
    public static final DeferredItem<BlockItem> CENTRIFUGAL_COMPRESSOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.CENTRIFUGAL_COMPRESSOR);

    /** The BlockItem used to place the Hydraulic Press. */
    public static final DeferredItem<BlockItem> HYDRAULIC_PRESS =
            ITEMS.registerSimpleBlockItem(ModBlocks.HYDRAULIC_PRESS);

    /** The BlockItem used to place the Hydrostatic Intake. */
    public static final DeferredItem<BlockItem> HYDROSTATIC_INTAKE =
            ITEMS.registerSimpleBlockItem(ModBlocks.HYDROSTATIC_INTAKE);

    /** The BlockItem used to place the Hydraulic Motor. */
    public static final DeferredItem<BlockItem> HYDRAULIC_MOTOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.HYDRAULIC_MOTOR);

    /** The BlockItem used to place the Hydraulic Fist. */
    public static final DeferredItem<BlockItem> HYDRAULIC_FIST =
            ITEMS.registerSimpleBlockItem(ModBlocks.HYDRAULIC_FIST);

    /** The BlockItem used to place the Hydraulic Drill. */
    public static final DeferredItem<BlockItem> HYDRAULIC_DRILL =
            ITEMS.registerSimpleBlockItem(ModBlocks.HYDRAULIC_DRILL);

    /** The BlockItem used to place the Hydraulic Assembly Unit. */
    public static final DeferredItem<BlockItem> HYDRAULIC_ASSEMBLY_UNIT =
            ITEMS.registerSimpleBlockItem(ModBlocks.HYDRAULIC_ASSEMBLY_UNIT);

    /**
     * Bucket of Hydro Fluid &ndash; what players use to fill the Hydraulic Press's tank. Single-stack and
     * leaves an empty bucket behind, like every vanilla fluid bucket. Unlike a vanilla bucket it cannot pour
     * the fluid into the world (see {@link HydroFluidBucketItem}); it only loads machine tanks. Its icon is
     * {@code item/bucket_water}.
     */
    public static final DeferredItem<HydroFluidBucketItem> HYDRO_FLUID_BUCKET = ITEMS.register(
            "hydro_fluid_bucket",
            () -> new HydroFluidBucketItem(ModFluids.HYDRO_FLUID.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private ModItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
