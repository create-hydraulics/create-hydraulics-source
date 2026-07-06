package com.createhydro.hydro.registry;

import com.createhydro.hydro.CreateHydraulics;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Creative inventory tabs added by Create: Hydraulics. */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateHydraulics.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createhydraulics"))
                    .icon(() -> new ItemStack(ModItems.HARDENED_IRON_PIPE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.HARDENED_IRON_PIPE.get());
                        output.accept(ModItems.PRESSURE_VALVE.get());
                        output.accept(ModItems.FLOW_GAUGE.get());
                        output.accept(ModItems.AQUEDUCT_INTAKE.get());
                        output.accept(ModItems.HYDROSTATIC_INTAKE.get());
                        output.accept(ModItems.CENTRIFUGAL_COMPRESSOR.get());
                        output.accept(ModItems.HYDRAULIC_PRESS.get());
                        output.accept(ModItems.HYDRAULIC_MOTOR.get());
                        output.accept(ModItems.HYDRAULIC_FIST.get());
                        output.accept(ModItems.HYDRAULIC_DRILL.get());
                        output.accept(ModItems.HYDRAULIC_ASSEMBLY_UNIT.get());
                        output.accept(ModItems.HYDRO_FLUID_BUCKET.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
