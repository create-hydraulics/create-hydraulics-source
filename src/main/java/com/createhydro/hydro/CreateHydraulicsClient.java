package com.createhydro.hydro;

import com.createhydro.hydro.client.CentrifugalCompressorRenderer;
import com.createhydro.hydro.client.FlowGaugeRenderer;
import com.createhydro.hydro.client.HydraulicDrillRenderer;
import com.createhydro.hydro.client.HydraulicFistRenderer;
import com.createhydro.hydro.client.HydraulicMotorRenderer;
import com.createhydro.hydro.client.HydraulicPressRenderer;
import com.createhydro.hydro.ponder.HydraulicsPonderPlugin;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.createhydro.hydro.registry.ModBlocks;
import com.createhydro.hydro.registry.ModFluidTypes;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CreateHydraulics.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CreateHydraulics.MODID, value = Dist.CLIENT)
public class CreateHydraulicsClient {
    public CreateHydraulicsClient(ModContainer container, IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(CreateHydraulicsClient::registerRenderers);
        modEventBus.addListener(CreateHydraulicsClient::registerAdditionalModels);
        modEventBus.addListener(CreateHydraulicsClient::registerBlockColors);
    }

    // tintindex 0 on the press model lets the BER flash it red/green; neutral white for everything else
    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> 0xFFFFFF,
                ModBlocks.HYDRAULIC_PRESS.get(), ModBlocks.HYDRAULIC_FIST.get(), ModBlocks.HYDRAULIC_DRILL.get());
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new HydraulicsPonderPlugin());
        CreateHydraulics.LOGGER.info("Create: Hydraulics — Ponder plugin registered.");
    }

    // reuses water textures with a teal tint — swap these out once we get custom fluid art
    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
            private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOW;
            }

            @Override
            public int getTintColor() {
                return 0xFF2BB3C0; // opaque teal
            }
        }, ModFluidTypes.HYDRO_FLUID_TYPE.get());
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FLOW_GAUGE.get(), FlowGaugeRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HYDRAULIC_PRESS.get(), HydraulicPressRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HYDRAULIC_MOTOR.get(), HydraulicMotorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CENTRIFUGAL_COMPRESSOR.get(), CentrifugalCompressorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HYDRAULIC_FIST.get(), HydraulicFistRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HYDRAULIC_DRILL.get(), HydraulicDrillRenderer::new);
    }

    private static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FlowGaugeRenderer.NEEDLE_MODEL);
        event.register(HydraulicPressRenderer.HEAD_MODEL);
        event.register(HydraulicPressRenderer.HEAD_MODEL_FULL);
        event.register(HydraulicFistRenderer.POLE);
        event.register(HydraulicFistRenderer.POLE_FULL);
        event.register(HydraulicFistRenderer.HAND);
        event.register(HydraulicFistRenderer.HAND_FULL);
        event.register(HydraulicDrillRenderer.HEAD);
        event.register(HydraulicDrillRenderer.HEAD_FULL);
    }
}
