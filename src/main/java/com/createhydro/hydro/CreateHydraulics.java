package com.createhydro.hydro;

import org.slf4j.Logger;

import com.createhydro.hydro.registry.ModBlockEntities;
import com.createhydro.hydro.registry.ModBlocks;
import com.createhydro.hydro.registry.ModCreativeTabs;
import com.createhydro.hydro.registry.ModFluidTypes;
import com.createhydro.hydro.registry.ModFluids;
import com.createhydro.hydro.registry.ModItems;
import com.createhydro.hydro.registry.ModRecipes;
import com.createhydro.hydro.contraption.HydraulicDrillMovementBehaviour;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

@Mod(CreateHydraulics.MODID)
public class CreateHydraulics {

    public static final String MODID = "createhydraulics";

    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateHydraulics(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        ModFluidTypes.register(modEventBus);
        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModRecipes.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // expose tanks so buckets and pumps can actually interact with them
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.HYDRAULIC_PRESS.get(),
                (be, side) -> be.getFluidTank());

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.HYDRAULIC_MOTOR.get(),
                (be, side) -> be.getFluidTank());

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.HYDRAULIC_FIST.get(),
                (be, side) -> be.getFluidTank());

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.HYDRAULIC_DRILL.get(),
                (be, side) -> be.getFluidTank());

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CENTRIFUGAL_COMPRESSOR.get(),
                (be, side) -> be.getFluidTank());

        // assembly unit: fluid + item capability (funnels use the item side, buckets use the fluid side)
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.HYDRAULIC_ASSEMBLY_UNIT.get(),
                (be, side) -> be.getFluidTank());

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.HYDRAULIC_ASSEMBLY_UNIT.get(),
                (be, side) -> be.getItemHandler());

        // NeoForge skips capability auto-registration for BucketItem subclasses, so we do it manually
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new FluidBucketWrapper(stack),
                ModItems.HYDRO_FLUID_BUCKET.get());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // REGISTRY isn't thread-safe, so enqueue this on the main thread
        event.enqueueWork(() -> MovementBehaviour.REGISTRY.register(
                ModBlocks.HYDRAULIC_DRILL.get(), new HydraulicDrillMovementBehaviour()));
        LOGGER.info("Create: Hydraulics loaded - hardened iron piping online.");
    }
}
