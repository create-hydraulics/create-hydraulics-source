package com.createhydro.hydro.registry;

import com.createhydro.hydro.CreateHydraulics;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Fluid types added by Create: Hydraulics. */
public final class ModFluidTypes {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CreateHydraulics.MODID);

    /**
     * Hydro Fluid &ndash; the working fluid of the hydraulic system. This is the {@link FluidType}: the
     * shared behaviour/properties of the fluid (the actual source/flowing fluids live in {@link ModFluids}).
     * It behaves much like water to stand in / swim through; its in-world appearance (still/flow textures and
     * tint) is supplied client-side in {@code CreateHydraulicsClient}.
     */
    public static final DeferredHolder<FluidType, FluidType> HYDRO_FLUID_TYPE =
            FLUID_TYPES.register("hydro_fluid", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.createhydraulics.hydro_fluid")
                    .canSwim(true)
                    .canDrown(true)
                    .canExtinguish(true)));

    private ModFluidTypes() {}

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
    }
}
