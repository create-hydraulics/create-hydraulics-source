package com.createhydro.hydro.ponder;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.registry.ModBlocks;
import com.createhydro.hydro.registry.ModItems;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Registers Ponder in-game documentation scenes for Create: Hydraulics blocks. */
public class HydraulicsPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return CreateHydraulics.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(
                ModBlocks.HARDENED_IRON_PIPE.getId(),
                "hardened_iron_pipe",
                HydraulicsScenes::hardenedIronPipe
        );
        helper.addStoryBoard(
                ModBlocks.PRESSURE_VALVE.getId(),
                "pressure_valve/intro",
                HydraulicsScenes::pressureValve
        );
        helper.addStoryBoard(
                ModBlocks.FLOW_GAUGE.getId(),
                "flow_gauge",
                HydraulicsScenes::flowGauge
        );
        helper.addStoryBoard(
                ModBlocks.AQUEDUCT_INTAKE.getId(),
                "aqueduct_intake",
                HydraulicsScenes::aqueductIntake
        );
        helper.addStoryBoard(
                ModBlocks.HYDROSTATIC_INTAKE.getId(),
                "hydrostatic_intake",
                HydraulicsScenes::hydrostaticIntake
        );
        helper.addStoryBoard(
                ModBlocks.HYDRAULIC_PRESS.getId(),
                "hydraulic_press",
                HydraulicsScenes::hydraulicPress
        );
        helper.addStoryBoard(
                ModBlocks.HYDRAULIC_MOTOR.getId(),
                "hydraulic_motor",
                HydraulicsScenes::hydraulicMotor
        );
        helper.addStoryBoard(
                ModBlocks.CENTRIFUGAL_COMPRESSOR.getId(),
                "centrifugal_compressor",
                HydraulicsScenes::centrifugalCompressor
        );
        helper.addStoryBoard(
                ModBlocks.HYDRAULIC_FIST.getId(),
                "hydraulic_fist",
                HydraulicsScenes::hydraulicFist
        );
        helper.addStoryBoard(
                ModBlocks.HYDRAULIC_DRILL.getId(),
                "hydraulic_drill",
                HydraulicsScenes::hydraulicDrill
        );
        helper.addStoryBoard(
                ModItems.HYDRO_FLUID_BUCKET.getId(),
                "hydro_fluid_bucket",
                HydraulicsScenes::hydroFluidBucket
        );
        helper.addStoryBoard(
                ModBlocks.HYDRAULIC_ASSEMBLY_UNIT.getId(),
                "hydraulic_assembly_unit",
                HydraulicsScenes::hydraulicAssemblyUnit
        );
    }
}
