package com.createhydro.hydro;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MOTOR_FLUID_PER_SECOND;
    public static final ModConfigSpec.DoubleValue MOTOR_STRESS_PER_RPM;
    public static final ModConfigSpec.DoubleValue MOTOR_PRESSURE_PER_RPM;

    public static final ModConfigSpec.DoubleValue FIST_PRESSURE;
    public static final ModConfigSpec.IntValue FIST_REACH;

    public static final ModConfigSpec.DoubleValue DRILL_PRESSURE;

    public static final ModConfigSpec.DoubleValue COMPRESSOR_PRESSURE_PER_RPM;
    public static final ModConfigSpec.DoubleValue COMPRESSOR_STRESS_IMPACT;

    static {
        BUILDER.push("hydraulicMotor");

        MOTOR_FLUID_PER_SECOND = BUILDER
                .comment("Hydro Fluid consumed by an active Hydraulic Motor, in mB per second.")
                .defineInRange("fluidPerSecond", 5, 0, 100_000);

        MOTOR_STRESS_PER_RPM = BUILDER
                .comment("Stress capacity the Hydraulic Motor provides per RPM. The Stress Units a motor",
                        "supplies equal this value multiplied by its current speed (e.g. 128 × 16 RPM = 2048 su).")
                .defineInRange("stressCapacityPerRpm", 128.0, 0.0, 1_000_000.0);

        MOTOR_PRESSURE_PER_RPM = BUILDER
                .comment("Pressure (PU) the connected line must carry per RPM for the motor to run. The motor",
                        "stalls (underpressured) until its line supplies at least this value times its set RPM.")
                .defineInRange("pressurePerRpm", 4.0, 0.0, 100_000.0);

        BUILDER.pop();

        BUILDER.push("hydraulicFist");

        FIST_PRESSURE = BUILDER
                .comment("Pressure (PU) a Hydraulic Fist's line must carry for it to punch. Like Create stress this",
                        "is a continuous load, not a consumed resource; if the combined load of every Fist and Press",
                        "on a line exceeds its supplied pressure they all stall (underpressured).")
                .defineInRange("pressure", 200.0, 0.0, 100_000.0);

        FIST_REACH = BUILDER
                .comment("How many blocks ahead of its front face the Hydraulic Fist scans for a target and",
                        "telescopes its fist out to reach (1 = only the directly adjacent block).")
                .defineInRange("reach", 4, 1, 16);

        BUILDER.pop();

        BUILDER.push("hydraulicDrill");

        DRILL_PRESSURE = BUILDER
                .comment("Pressure (PU) a Hydraulic Drill's line must carry for it to break the block in front of",
                        "it. Like Create stress this is a continuous load, not a consumed resource; if the combined",
                        "load on a line exceeds its supplied pressure the machines on it stall (underpressured).")
                .defineInRange("pressure", 300.0, 0.0, 100_000.0);

        BUILDER.pop();

        BUILDER.push("centrifugalCompressor");

        COMPRESSOR_PRESSURE_PER_RPM = BUILDER
                .comment("Pressure (PU) the Centrifugal Compressor pushes into its pipe network per RPM. The output",
                        "equals this value multiplied by the shaft's current speed (e.g. 8 × 32 RPM = 256 PU), capped",
                        "at the maximum a single line can hold.")
                .defineInRange("pressurePerRpm", 8.0, 0.0, 100_000.0);

        COMPRESSOR_STRESS_IMPACT = BUILDER
                .comment("Stress impact of the Centrifugal Compressor, in Stress Units consumed per RPM. This is a",
                        "real load on the rotation network (like any Create machine): the faster it is driven, the",
                        "more Stress Units it draws while it compresses.")
                .defineInRange("stressImpact", 8.0, 0.0, 100_000.0);

        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
