package com.createhydro.hydro.registry;

import com.createhydro.hydro.CreateHydraulics;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Hydro Fluid &ndash; the working fluid used by the Hydraulic Press (and the rest of the hydraulic system).
 *
 * <p>A standard NeoForge flowing fluid: a {@link BaseFlowingFluid.Source} (the still source block) and a
 * {@link BaseFlowingFluid.Flowing} (the flowing variant), tied together with its
 * {@link ModFluidTypes#HYDRO_FLUID_TYPE}, its {@link ModBlocks#HYDRO_FLUID_BLOCK liquid block} and its
 * {@link ModItems#HYDRO_FLUID_BUCKET bucket}. The bucket is what players use to load the press; the press's
 * tank only accepts this fluid.</p>
 */
public final class ModFluids {

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, CreateHydraulics.MODID);

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> HYDRO_FLUID =
            FLUIDS.register("hydro_fluid", () -> new BaseFlowingFluid.Source(properties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> HYDRO_FLUID_FLOWING =
            FLUIDS.register("hydro_fluid_flowing", () -> new BaseFlowingFluid.Flowing(properties()));

    /**
     * Builds the shared fluid properties. The bucket/block/fluid references are suppliers, so they resolve
     * lazily &ndash; safe even though those entries register in other {@code DeferredRegister}s.
     */
    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(
                ModFluidTypes.HYDRO_FLUID_TYPE, ModFluids.HYDRO_FLUID, ModFluids.HYDRO_FLUID_FLOWING)
                .bucket(ModItems.HYDRO_FLUID_BUCKET)
                .block(ModBlocks.HYDRO_FLUID_BLOCK)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(20);
    }

    private ModFluids() {}

    public static void register(IEventBus modEventBus) {
        FLUIDS.register(modEventBus);
    }
}
