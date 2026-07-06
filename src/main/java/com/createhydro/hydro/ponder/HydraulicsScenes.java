package com.createhydro.hydro.ponder;

import com.createhydro.hydro.block.*;
import com.createhydro.hydro.block.entity.FlowGaugeBlockEntity;
import com.createhydro.hydro.block.entity.HydraulicDrillBlockEntity;
import com.createhydro.hydro.block.entity.HydraulicFistBlockEntity;
import com.createhydro.hydro.block.entity.HydraulicMotorBlockEntity;
import com.createhydro.hydro.block.entity.HydraulicPressBlockEntity;
import com.createhydro.hydro.block.entity.HydrostaticIntakeBlockEntity;
import com.createhydro.hydro.registry.ModBlocks;
import com.createhydro.hydro.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Ponder scene scripts for Create: Hydraulics. */
public final class HydraulicsScenes {

    private HydraulicsScenes() {}

    // ------------------------------------------------------------------------------------------------
    // Hardened Iron Pipe
    // ------------------------------------------------------------------------------------------------

    public static void hardenedIronPipe(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("hardened_iron_pipe", "Hardened Iron Pipe");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(20);

        // The structure is baked into hardened_iron_pipe.nbt: a straight east-west pipe run
        // along z = 2 at y = 1, with a Flow Gauge spliced into the middle (x = 2).
        BlockPos p0    = util.grid().at(0, 1, 2);
        BlockPos p1    = util.grid().at(1, 1, 2);
        BlockPos gauge = util.grid().at(2, 1, 2);
        BlockPos p3    = util.grid().at(3, 1, 2);
        BlockPos p4    = util.grid().at(4, 1, 2);

        // Reveal the whole pipe line sitting on the base plate.
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        // 1) What the pipes are.
        scene.overlay().showText(80)
                .text("createhydraulics.ponder.hardened_iron_pipe.text_1")
                .pointAt(util.vector().centerOf(p1))
                .attachKeyFrame();
        scene.idle(90);

        // 2) Pressure travels down the line: sweep a highlight west -> east while the gauge climbs.
        scene.overlay().showText(80)
                .text("createhydraulics.ponder.hardened_iron_pipe.text_2")
                .pointAt(util.vector().blockSurface(p0, Direction.WEST))
                .attachKeyFrame();
        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.GREEN, "flow0", util.select().position(p0), 30);
        scene.idle(8);
        scene.overlay().showOutline(PonderPalette.GREEN, "flow1", util.select().position(p1), 30);
        scene.idle(8);
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class, be -> be.setPressure(800.0F));
        scene.overlay().showOutline(PonderPalette.GREEN, "flow2", util.select().position(gauge), 30);
        scene.idle(8);
        scene.overlay().showOutline(PonderPalette.GREEN, "flow3", util.select().position(p3), 30);
        scene.idle(8);
        scene.overlay().showOutline(PonderPalette.GREEN, "flow4", util.select().position(p4), 30);
        scene.idle(70);

        // 3) The Flow Gauge reads the live pressure on the line.
        scene.overlay().showOutline(PonderPalette.BLUE, "dial", util.select().position(gauge), 80);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hardened_iron_pipe.text_3")
                .pointAt(util.vector().centerOf(gauge))
                .placeNearTarget()
                .attachKeyFrame();
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class, be -> be.setPressure(200.0F));
        scene.idle(100);

        scene.markAsFinished();
    }

    // ------------------------------------------------------------------------------------------------
    // Pressure Valve
    // ------------------------------------------------------------------------------------------------

    public static void pressureValve(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("pressure_valve", "Pressure Valve");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(20);

        BlockPos valve    = util.grid().at(3, 3, 2);
        BlockPos redstone = util.grid().at(3, 2, 2);
        BlockPos gauge    = util.grid().at(3, 3, 0);

        // Force valve to OFF state (unpowered = open = pressure flows through)
        scene.world().modifyBlock(
                valve,
                s -> s.getBlock() instanceof PressureValveBlock
                        ? s.setValue(BlockStateProperties.POWERED, false)
                        : s,
                false
        );

        // Set gauge to show 200 PU (valve is open, pressure is flowing)
        // Set gauge to show 200 PU (valve is open, pressure is flowing)
        scene.world().modifyBlockEntityNBT(
                util.select().position(gauge),
                FlowGaugeBlockEntity.class,
                nbt -> nbt.putFloat("Pressure", 800.0F)
        );
        scene.world().modifyBlock(gauge, s -> s, true);

        // Show everything except redstone block
        scene.world().showSection(
                util.select().layersFrom(1).substract(util.select().position(redstone)),
                Direction.UP
        );
        scene.idle(30);

        // --- Explain OFF state (valve open, pressure flowing, gauge reads 200) ---
        scene.overlay().showText(60)
                .text("createhydraulics.ponder.pressure_valve.text_1")
                .pointAt(util.vector().centerOf(valve))
                .attachKeyFrame();
        scene.idle(70);

        // --- Reveal redstone block ---
        scene.world().showSection(
                util.select().position(redstone),
                Direction.DOWN
        );
        scene.idle(20);

        // Toggle valve ON (powered = closed = pressure blocked)
        scene.world().modifyBlock(
                valve,
                s -> s.getBlock() instanceof PressureValveBlock
                        ? s.setValue(BlockStateProperties.POWERED, true)
                        : s,
                false
        );

        // Set gauge to show 0 PU (valve is closed, no pressure)
        // Set gauge to show 0 PU (valve is closed, no pressure)
        scene.world().modifyBlockEntityNBT(
                util.select().position(gauge),
                FlowGaugeBlockEntity.class,
                nbt -> nbt.putFloat("Pressure", 0.0F)
        );
        scene.world().modifyBlock(gauge, s -> s, true);
        scene.idle(20);

        // --- Explain ON state (valve closed, pressure blocked, gauge reads 0) ---
        scene.overlay().showText(60)
                .text("createhydraulics.ponder.pressure_valve.text_2")
                .pointAt(util.vector().centerOf(valve))
                .attachKeyFrame();
        scene.idle(70);

        scene.markAsFinished();
    }

    // ------------------------------------------------------------------------------------------------
    // Flow Gauge
    // ------------------------------------------------------------------------------------------------

    public static void flowGauge(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("flow_gauge", "Reading pressure with the Flow Gauge");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(20);

        // Baked into flow_gauge.nbt: a short east-west pipe run with the gauge spliced in the middle.
        BlockPos gauge = util.grid().at(2, 1, 2);

        scene.world().showSection(util.select().fromTo(1, 1, 2, 3, 1, 2), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(70)
                .text("createhydraulics.ponder.flow_gauge.text_1")
                .pointAt(util.vector().centerOf(gauge))
                .attachKeyFrame();
        scene.idle(80);

        // Drive the needle so it visibly sweeps as pressure rises.
        scene.overlay().showText(70)
                .text("createhydraulics.ponder.flow_gauge.text_2")
                .pointAt(util.vector().blockSurface(gauge, Direction.NORTH))
                .attachKeyFrame();
        scene.idle(10);
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class, be -> be.setPressure(500.0F));
        scene.idle(35);
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class, be -> be.setPressure(1000.0F));
        scene.idle(35);
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class, be -> be.setPressure(2048.0F));
        scene.idle(40);

        scene.overlay().showOutline(PonderPalette.BLUE, "dial", util.select().position(gauge), 80);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.flow_gauge.text_3")
                .pointAt(util.vector().centerOf(gauge))
                .placeNearTarget()
                .attachKeyFrame();
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class, be -> be.setPressure(1000.0F));
        scene.idle(100);

        scene.markAsFinished();
    }

    // ------------------------------------------------------------------------------------------------
    // Aqueduct Intake
    // ------------------------------------------------------------------------------------------------

    public static void aqueductIntake(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("aqueduct_intake", "Generating pressure with the Aqueduct Intake");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        // Baked into aqueduct_intake.nbt: a 3x3 water pool with intakes at (1,1,2) and (3,1,2),
        // each feeding an elbow up to a shared Flow Gauge at (2,2,2).
        BlockPos intakeA = util.grid().at(1, 1, 2);
        BlockPos intakeB = util.grid().at(3, 1, 2);
        BlockPos gauge   = util.grid().at(2, 2, 2);

        // 1) The pool with the first intake submerged — hold the second intake + its pipe back for now.
        scene.world().showSection(
                util.select().fromTo(1, 1, 1, 3, 1, 3).substract(util.select().position(intakeB)),
                Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(80)
                .text("createhydraulics.ponder.aqueduct_intake.text_1")
                .pointAt(util.vector().centerOf(intakeA))
                .attachKeyFrame();
        scene.idle(90);

        // 2) It switches on and pressurizes the pipe on its top face.
        scene.world().modifyBlock(intakeA, s -> s.setValue(AqueductIntakeBlock.ACTIVE, true), false);
        scene.world().showSection(util.select().fromTo(1, 2, 2, 2, 2, 2), Direction.DOWN);
        scene.idle(10);
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class, be -> be.setPressure(100.0F));
        scene.overlay().showOutline(PonderPalette.GREEN, "one", util.select().position(intakeA), 80);
        scene.overlay().showText(80)
                .text("createhydraulics.ponder.aqueduct_intake.text_2")
                .pointAt(util.vector().centerOf(gauge))
                .attachKeyFrame();
        scene.idle(90);

        // 3) A second intake on the same line stacks the pressure.
        scene.world().showSection(util.select().fromTo(3, 1, 2, 3, 2, 2), Direction.DOWN);
        scene.idle(10);
        scene.world().modifyBlock(intakeB, s -> s.setValue(AqueductIntakeBlock.ACTIVE, true), false);
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class, be -> be.setPressure(200.0F));
        scene.overlay().showOutline(PonderPalette.GREEN, "two", util.select().fromTo(1, 1, 2, 3, 1, 2), 90);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.aqueduct_intake.text_3")
                .pointAt(util.vector().centerOf(gauge))
                .attachKeyFrame();
        scene.idle(100);

        scene.markAsFinished();
    }

    // ------------------------------------------------------------------------------------------------
    // Hydrostatic Intake
    // ------------------------------------------------------------------------------------------------

    public static void hydrostaticIntake(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("hydrostatic_intake", "Generating pressure with the Hydrostatic Intake");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        // Baked into hydrostatic_intake.nbt: the intake at (1,2,2) on an elbow feeding a Flow Gauge,
        // with a water column stacked above it at y = 3..6.
        BlockPos gauge  = util.grid().at(2, 1, 2);
        BlockPos intake = util.grid().at(1, 2, 2);
        BlockPos water1 = util.grid().at(1, 3, 2);
        BlockPos water3 = util.grid().at(1, 5, 2);

        // 1) Reveal the intake and the pipe line it feeds from its bottom face.
        scene.world().showSection(util.select().fromTo(1, 1, 2, 3, 1, 2), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(intake), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydrostatic_intake.text_1")
                .pointAt(util.vector().centerOf(intake))
                .attachKeyFrame();
        scene.idle(100);

        // 2) A single water source switches it on; pressure leaves the bottom face into the pipe.
        scene.world().showSection(util.select().position(water1), Direction.DOWN);
        scene.world().modifyBlock(intake, s -> s.setValue(HydrostaticIntakeBlock.ACTIVE, true), false);
        scene.idle(10);
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class,
                be -> be.setPressure(HydrostaticIntakeBlockEntity.outputFor(1)));
        scene.overlay().showOutline(PonderPalette.GREEN, "one", util.select().position(water1), 80);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydrostatic_intake.text_2")
                .pointAt(util.vector().centerOf(gauge))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // 3) A taller column means higher pressure — the needle climbs with the column.
        scene.world().showSection(util.select().fromTo(1, 4, 2, 1, 6, 2), Direction.DOWN);
        scene.idle(10);
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class,
                be -> be.setPressure(HydrostaticIntakeBlockEntity.outputFor(4)));
        scene.overlay().showOutline(PonderPalette.BLUE, "column", util.select().fromTo(1, 3, 2, 1, 6, 2), 80);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydrostatic_intake.text_3")
                .pointAt(util.vector().centerOf(water3))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // 4) The cap: up to 20 sources, reaching 600 PU.
        scene.overlay().showText(100)
                .text("createhydraulics.ponder.hydrostatic_intake.text_4")
                .pointAt(util.vector().centerOf(intake))
                .attachKeyFrame();
        scene.idle(110);

        scene.markAsFinished();
    }

    // ------------------------------------------------------------------------------------------------
    // Hydraulic Press
    // ------------------------------------------------------------------------------------------------

    public static void hydraulicPress(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("hydraulic_press", "Pressing items with the Hydraulic Press");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        // Baked into hydraulic_press.nbt: depot at (2,1,2), press above it, a pipe run east to a
        // submerged Aqueduct Intake at (4,1,2).
        BlockPos depot   = util.grid().at(2, 1, 2);
        BlockPos press   = util.grid().at(2, 2, 2);
        BlockPos pipeTop = util.grid().at(2, 3, 2);
        BlockPos intake  = util.grid().at(4, 1, 2);

        // 1) The depot the press acts on.
        scene.world().showSection(util.select().position(depot), Direction.UP);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("createhydraulics.ponder.hydraulic_press.text_1")
                .pointAt(util.vector().centerOf(depot))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        // 2) The press, one block above the depot.
        scene.world().showSection(util.select().position(press), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("createhydraulics.ponder.hydraulic_press.text_2")
                .pointAt(util.vector().centerOf(press))
                .attachKeyFrame();
        scene.idle(90);

        // 3) The pressure line into the top face, fed by a source.
        scene.world().showSection(util.select().fromTo(4, 1, 1, 4, 1, 3), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(2, 3, 2, 4, 3, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(util.grid().at(4, 2, 2)), Direction.DOWN);
        scene.idle(10);
        scene.world().modifyBlock(intake, s -> s.setValue(AqueductIntakeBlock.ACTIVE, true), false);
        scene.overlay().showOutline(PonderPalette.BLUE, "top",
                util.select().position(pipeTop), 80);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_press.text_3")
                .pointAt(util.vector().blockSurface(press, Direction.UP))
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showText(100)
                .text("createhydraulics.ponder.hydraulic_press.text_4")
                .pointAt(util.vector().centerOf(intake))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        // 4) Load the working fluid (flips the model to its filled look).
        scene.world().modifyBlock(press, s -> s.setValue(HydraulicPressBlock.FULL, true), false);
        scene.overlay().showText(100)
                .text("createhydraulics.ponder.hydraulic_press.text_5")
                .pointAt(util.vector().centerOf(press))
                .attachKeyFrame();
        scene.idle(110);

        // 5) Drop a pressable item on the depot and run the press.
        scene.world().modifyBlockEntity(depot, DepotBlockEntity.class,
                d -> d.setHeldItem(new ItemStack(Items.IRON_INGOT)));
        scene.idle(10);
        scene.overlay().showText(80)
                .text("createhydraulics.ponder.hydraulic_press.text_6")
                .pointAt(util.vector().centerOf(depot))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        // The press strokes: head drives down, stamps the item, lifts back up. One item per stroke.
        scene.world().modifyBlockEntity(press, HydraulicPressBlockEntity.class,
                HydraulicPressBlockEntity::startPonderStroke);
        scene.idle(20);
        scene.world().modifyBlockEntity(depot, DepotBlockEntity.class,
                d -> d.setHeldItem(new ItemStack(AllItems.IRON_SHEET.get())));
        scene.idle(25);
        scene.overlay().showText(100)
                .text("createhydraulics.ponder.hydraulic_press.text_7")
                .pointAt(util.vector().centerOf(depot))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);

        // A second stroke to show the deliberate, repeating cadence.
        scene.world().modifyBlockEntity(press, HydraulicPressBlockEntity.class,
                HydraulicPressBlockEntity::startPonderStroke);
        scene.idle(45);
        scene.overlay().showText(100)
                .text("createhydraulics.ponder.hydraulic_press.text_8")
                .pointAt(util.vector().centerOf(depot))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.markAsFinished();
    }

    // ------------------------------------------------------------------------------------------------
    // Hydraulic Motor
    // ------------------------------------------------------------------------------------------------

    public static void hydraulicMotor(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("hydraulic_motor", "Generating rotation with the Hydraulic Motor");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        // Baked into hydraulic_motor.nbt: motor facing east at (2,2,2), back pipe at (1,2,2) dropping
        // to a submerged Aqueduct Intake at (1,1,2).
        BlockPos motor  = util.grid().at(2, 2, 2);
        BlockPos intake = util.grid().at(1, 1, 2);

        // 1) The motor itself.
        scene.world().showSection(util.select().position(motor), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_motor.text_1")
                .pointAt(util.vector().centerOf(motor))
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showOutline(PonderPalette.GREEN, "shaft",
                util.select().fromTo(2, 2, 2, 3, 2, 2), 80);
        scene.overlay().showText(80)
                .text("createhydraulics.ponder.hydraulic_motor.text_2")
                .pointAt(util.vector().blockSurface(motor, Direction.EAST))
                .attachKeyFrame();
        scene.idle(90);

        // 2) Pressure into the back face, from a source.
        scene.world().showSection(util.select().fromTo(1, 1, 1, 1, 1, 3), Direction.DOWN);
        scene.world().showSection(util.select().position(util.grid().at(1, 2, 2)), Direction.DOWN);
        scene.idle(10);
        scene.world().modifyBlock(intake, s -> s.setValue(AqueductIntakeBlock.ACTIVE, true), false);
        scene.overlay().showText(100)
                .text("createhydraulics.ponder.hydraulic_motor.text_3")
                .pointAt(util.vector().blockSurface(motor, Direction.WEST))
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_motor.text_4")
                .pointAt(util.vector().centerOf(intake))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // 3) Fuel.
        scene.world().modifyBlock(motor, s -> s.setValue(HydraulicMotorBlock.FULL, true), false);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_motor.text_5")
                .pointAt(util.vector().centerOf(motor))
                .attachKeyFrame();
        scene.idle(100);

        // 4) Spin it up. Setting the kinetic speed makes the shaft turn for the demo.
        scene.world().modifyBlockEntity(motor, HydraulicMotorBlockEntity.class, m -> m.setSpeed(64.0F));
        scene.idle(20);
        scene.overlay().showText(110)
                .text("createhydraulics.ponder.hydraulic_motor.text_6")
                .pointAt(util.vector().centerOf(motor))
                .attachKeyFrame();
        scene.idle(120);

        scene.world().modifyBlockEntity(motor, HydraulicMotorBlockEntity.class, m -> m.setSpeed(64.0F));
        scene.overlay().showText(110)
                .text("createhydraulics.ponder.hydraulic_motor.text_7")
                .pointAt(util.vector().blockSurface(motor, Direction.EAST))
                .attachKeyFrame();
        scene.idle(120);

        scene.markAsFinished();
    }

    // ------------------------------------------------------------------------------------------------
    // Hydraulic Drill
    // ------------------------------------------------------------------------------------------------

    public static void hydraulicDrill(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("hydraulic_drill", "Boring through blocks with the Hydraulic Drill");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(20);

        // Structure baked into hydraulic_drill.nbt: the drill faces east at (2,2,2); a submerged
        // Aqueduct Intake at (1,1,2) feeds its back face through a pipe; stone sits ahead at x = 3, 4.
        BlockPos drill    = util.grid().at(2, 2, 2);
        BlockPos intake   = util.grid().at(1, 1, 2);
        BlockPos backPipe = util.grid().at(1, 2, 2);
        BlockPos target1  = util.grid().at(3, 2, 2);
        BlockPos target2  = util.grid().at(4, 2, 2);

        // 1) The drill itself.
        scene.world().showSection(util.select().position(drill), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_drill.text_1")
                .pointAt(util.vector().centerOf(drill))
                .attachKeyFrame();
        scene.idle(100);

        // 2) Pressure into its back face, from a source.
        scene.world().showSection(util.select().fromTo(1, 1, 1, 1, 1, 3), Direction.DOWN);
        scene.world().showSection(util.select().position(backPipe), Direction.DOWN);
        scene.idle(10);
        scene.world().modifyBlock(intake, s -> s.setValue(AqueductIntakeBlock.ACTIVE, true), false);
        scene.overlay().showText(100)
                .text("createhydraulics.ponder.hydraulic_drill.text_2")
                .pointAt(util.vector().blockSurface(drill, Direction.WEST))
                .attachKeyFrame();
        scene.idle(110);

        // 3) Working fluid.
        scene.world().modifyBlock(drill, s -> s.setValue(HydraulicDrillBlock.FULL, true), false);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_drill.text_3")
                .pointAt(util.vector().centerOf(drill))
                .attachKeyFrame();
        scene.idle(100);

        // 4) Reveal the blocks ahead and bore through them, one stroke per block.
        scene.world().showSection(util.select().fromTo(3, 2, 2, 4, 2, 2), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.RED, "ahead", util.select().fromTo(3, 2, 2, 4, 2, 2), 60);
        scene.overlay().showText(100)
                .text("createhydraulics.ponder.hydraulic_drill.text_4")
                .pointAt(util.vector().centerOf(target1))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);

        scene.world().modifyBlockEntity(drill, HydraulicDrillBlockEntity.class,
                HydraulicDrillBlockEntity::startPonderStroke);
        scene.idle(20);
        scene.world().modifyBlock(target1, s -> Blocks.AIR.defaultBlockState(), true);
        scene.idle(30);
        scene.world().modifyBlockEntity(drill, HydraulicDrillBlockEntity.class,
                HydraulicDrillBlockEntity::startPonderStroke);
        scene.idle(20);
        scene.world().modifyBlock(target2, s -> Blocks.AIR.defaultBlockState(), true);
        scene.idle(60);

        scene.markAsFinished();
    }

    // ------------------------------------------------------------------------------------------------
    // Hydro Fluid Bucket
    // ------------------------------------------------------------------------------------------------

    public static void hydroFluidBucket(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("hydro_fluid_bucket", "Fuelling machines with Hydro Fluid");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(20);

        // Structure baked into hydro_fluid_bucket.nbt: a Depot at (1,1,2) presents the bucket;
        // a Hydraulic Press at (3,1,2) stands in for any machine it loads.
        BlockPos depot = util.grid().at(1, 1, 2);
        BlockPos press = util.grid().at(3, 1, 2);

        // 1) The bucket, shown on a Depot.
        scene.world().showSection(util.select().position(depot), Direction.UP);
        scene.world().modifyBlockEntity(depot, DepotBlockEntity.class,
                d -> d.setHeldItem(new ItemStack(ModItems.HYDRO_FLUID_BUCKET.get())));
        scene.idle(10);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydro_fluid_bucket.text_1")
                .pointAt(util.vector().centerOf(depot))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // 2) Loading a machine in one right-click.
        scene.world().showSection(util.select().position(press), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydro_fluid_bucket.text_2")
                .pointAt(util.vector().centerOf(press))
                .attachKeyFrame();
        scene.idle(20);
        scene.world().modifyBlock(press, s -> s.setValue(HydraulicPressBlock.FULL, true), false);
        scene.idle(80);

        // 3) Permanent medium vs. fuel.
        scene.overlay().showText(110)
                .text("createhydraulics.ponder.hydro_fluid_bucket.text_3")
                .pointAt(util.vector().centerOf(press))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);

        scene.markAsFinished();
    }

    // ------------------------------------------------------------------------------------------------
    // Hydraulic Fist
    // ------------------------------------------------------------------------------------------------

    public static void hydraulicFist(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("hydraulic_fist", "Crushing with the Hydraulic Fist");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        // Baked into hydraulic_fist.nbt: the Fist at (2,3,2) punches down at a target at (2,2,2);
        // a pipe run drops east to a submerged Aqueduct Intake at (3,1,2).
        BlockPos fist   = util.grid().at(2, 3, 2);
        BlockPos target = util.grid().at(2, 2, 2);
        BlockPos intake = util.grid().at(3, 1, 2);

        // 1) The Fist. (The target block stays hidden until the crushing demo.)
        scene.world().showSection(util.select().position(fist), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_fist.text_1")
                .pointAt(util.vector().centerOf(fist))
                .attachKeyFrame();
        scene.idle(100);

        // 2) Pressure line into the top face + source.
        scene.world().showSection(util.select().fromTo(3, 1, 1, 3, 1, 3), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(3, 2, 2, 3, 4, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(util.grid().at(2, 4, 2)), Direction.DOWN);
        scene.idle(10);
        scene.world().modifyBlock(intake, s -> s.setValue(AqueductIntakeBlock.ACTIVE, true), false);
        scene.overlay().showText(100)
                .text("createhydraulics.ponder.hydraulic_fist.text_2")
                .pointAt(util.vector().blockSurface(fist, Direction.UP))
                .attachKeyFrame();
        scene.idle(110);

        // 3) Fluid.
        scene.world().modifyBlock(fist, s -> s.setValue(HydraulicFistBlock.FULL, true), false);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_fist.text_3")
                .pointAt(util.vector().centerOf(fist))
                .attachKeyFrame();
        scene.idle(100);

        // 4) Crushing a block in the world (the baked target starts as Stone).
        scene.world().showSection(util.select().position(target), Direction.UP);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("createhydraulics.ponder.hydraulic_fist.text_4")
                .pointAt(util.vector().centerOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.world().modifyBlockEntity(fist, HydraulicFistBlockEntity.class, f -> f.startPonderPunch(0.125F));
        scene.idle(20);
        scene.world().modifyBlock(target, s -> Blocks.COBBLESTONE.defaultBlockState(), true);
        scene.idle(25);
        scene.overlay().showText(80)
                .text("createhydraulics.ponder.hydraulic_fist.text_5")
                .pointAt(util.vector().centerOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);

        scene.world().modifyBlockEntity(fist, HydraulicFistBlockEntity.class, f -> f.startPonderPunch(0.125F));
        scene.idle(20);
        scene.world().modifyBlock(target, s -> Blocks.GRAVEL.defaultBlockState(), true);
        scene.idle(25);
        scene.overlay().showText(80)
                .text("createhydraulics.ponder.hydraulic_fist.text_6")
                .pointAt(util.vector().centerOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        // 5) Crushing items on a depot — holding the stack until it is all crushed.
        scene.world().modifyBlock(target, s -> AllBlocks.DEPOT.get().defaultBlockState(), false);
        scene.world().modifyBlockEntity(target, DepotBlockEntity.class,
                d -> d.setHeldItem(new ItemStack(Items.COBBLESTONE)));
        scene.idle(10);
        scene.overlay().showText(100)
                .text("createhydraulics.ponder.hydraulic_fist.text_7")
                .pointAt(util.vector().centerOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.world().modifyBlockEntity(fist, HydraulicFistBlockEntity.class, f -> f.startPonderPunch(0.1875F));
        scene.idle(20);
        scene.world().modifyBlockEntity(target, DepotBlockEntity.class,
                d -> d.setHeldItem(new ItemStack(Items.GRAVEL)));
        scene.idle(25);
        scene.overlay().showText(110)
                .text("createhydraulics.ponder.hydraulic_fist.text_8")
                .pointAt(util.vector().centerOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);

        scene.markAsFinished();
    }

    // ------------------------------------------------------------------------------------------------
    // Hydraulic Assembly Unit
    // ------------------------------------------------------------------------------------------------

    public static void hydraulicAssemblyUnit(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("hydraulic_assembly_unit", "Hydraulic Assembly Unit");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(20);

        // Layout (setBlock replaces NBT content at each position; showSection reveals the result):
        //   (2,3,2) - Assembly Unit
        //   (2,2,2) - Hardened Iron Pipe (UP+DOWN arms)
        //   (2,1,2) - Aqueduct Intake, water on four sides
        BlockPos unitPos   = util.grid().at(2, 3, 2);
        BlockPos pipePos   = util.grid().at(2, 2, 2);
        BlockPos intakePos = util.grid().at(2, 1, 2);

        // ── Step 1: Introduce the unit ────────────────────────────────────────────────────────────
        scene.world().setBlock(unitPos,
                ModBlocks.HYDRAULIC_ASSEMBLY_UNIT.get().defaultBlockState()
                        .setValue(HydraulicAssemblyUnitBlock.FACING,  Direction.NORTH)
                        .setValue(HydraulicAssemblyUnitBlock.FULL,     false)
                        .setValue(HydraulicAssemblyUnitBlock.RUNNING,  false), false);
        scene.world().showSection(util.select().position(unitPos), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(80)
                .text("createhydraulics.ponder.hydraulic_assembly_unit.text_1")
                .pointAt(util.vector().centerOf(unitPos))
                .attachKeyFrame();
        scene.idle(90);

        // ── Step 2: Show the pressure supply below ────────────────────────────────────────────────
        scene.world().setBlock(util.grid().at(2, 1, 1), Blocks.WATER.defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 1, 3), Blocks.WATER.defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(1, 1, 2), Blocks.WATER.defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(3, 1, 2), Blocks.WATER.defaultBlockState(), false);
        scene.world().setBlock(intakePos,
                ModBlocks.AQUEDUCT_INTAKE.get().defaultBlockState()
                        .setValue(AqueductIntakeBlock.ACTIVE, false),
                false);
        scene.world().setBlock(pipePos,
                ModBlocks.HARDENED_IRON_PIPE.get().defaultBlockState()
                        .setValue(HardenedIronPipeBlock.UP,   true)
                        .setValue(HardenedIronPipeBlock.DOWN, true), false);
        // Reveal water pool + intake + pipe in one animation. showSection shows the current (replaced)
        // blocks at each position, not the original NBT blocks.
        scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 2, 3), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_assembly_unit.text_2")
                .pointAt(util.vector().blockSurface(unitPos, Direction.DOWN))
                .attachKeyFrame();
        scene.idle(100);

        // ── Step 3: Source comes alive, pressure flows up ──────────────────────────────────────────
        scene.world().modifyBlock(intakePos, s -> s.setValue(AqueductIntakeBlock.ACTIVE, true), false);
        scene.overlay().showOutline(PonderPalette.GREEN, "pressure",
                util.select().fromTo(2, 1, 2, 2, 3, 2), 80);
        scene.overlay().showText(80)
                .text("createhydraulics.ponder.hydraulic_assembly_unit.text_3")
                .pointAt(util.vector().centerOf(intakePos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        // ── Step 4: Fill with Hydro Fluid ─────────────────────────────────────────────────────────
        scene.world().modifyBlock(unitPos, s -> s.setValue(HydraulicAssemblyUnitBlock.FULL, true), false);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_assembly_unit.text_4")
                .pointAt(util.vector().centerOf(unitPos))
                .attachKeyFrame();
        scene.idle(100);

        // ── Step 5: Set the recipe filter ─────────────────────────────────────────────────────────
        scene.overlay().showOutline(PonderPalette.BLUE, "filter",
                util.select().position(unitPos), 100);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_assembly_unit.text_5")
                .pointAt(util.vector().blockSurface(unitPos, Direction.NORTH))
                .attachKeyFrame();
        scene.idle(100);

        // ── Step 6: Assembly in progress ──────────────────────────────────────────────────────────
        scene.world().modifyBlock(unitPos, s -> s.setValue(HydraulicAssemblyUnitBlock.RUNNING, true), false);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_assembly_unit.text_6")
                .pointAt(util.vector().centerOf(unitPos))
                .attachKeyFrame();
        scene.idle(100);

        // ── Step 7: Output ─────────────────────────────────────────────────────────────────────────
        scene.world().modifyBlock(unitPos, s -> s.setValue(HydraulicAssemblyUnitBlock.RUNNING, false), false);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.hydraulic_assembly_unit.text_7")
                .pointAt(util.vector().blockSurface(unitPos, Direction.EAST))
                .attachKeyFrame();
        scene.idle(100);

        scene.markAsFinished();
    }

    // ------------------------------------------------------------------------------------------------
    // Centrifugal Compressor
    // ------------------------------------------------------------------------------------------------

    public static void centrifugalCompressor(SceneBuilder scene, SceneBuildingUtil util) {
        // Create's scene builder adds the kinetic helpers (setKineticSpeed) that spin shafts for the demo.
        CreateSceneBuilder createScene = new CreateSceneBuilder(scene);

        scene.title("centrifugal_compressor", "Generating pressure with the Centrifugal Compressor");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        // Baked into centrifugal_compressor.nbt: the compressor at (2,1,2) with its shaft along X, an input
        // shaft line running west, and a riser + Flow Gauge on its top face.
        BlockPos compressor = util.grid().at(2, 1, 2);
        BlockPos gauge      = util.grid().at(3, 2, 2);
        BlockPos riser      = util.grid().at(2, 2, 2);
        Selection inputShafts = util.select().fromTo(0, 1, 2, 1, 1, 2);
        Selection drivenLine  = util.select().fromTo(0, 1, 2, 2, 1, 2); // input shafts + compressor

        // 1) The compressor itself.
        scene.world().showSection(util.select().position(compressor), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.centrifugal_compressor.text_1")
                .pointAt(util.vector().centerOf(compressor))
                .attachKeyFrame();
        scene.idle(100);

        // 2) Drive a shaft into its side.
        scene.world().showSection(inputShafts, Direction.EAST);
        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.GREEN, "shaft",
                util.select().fromTo(0, 1, 2, 2, 1, 2), 80);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.centrifugal_compressor.text_2")
                .pointAt(util.vector().blockSurface(compressor, Direction.WEST))
                .attachKeyFrame();
        scene.idle(100);

        // 3) Load it with Hydro Fluid (flips the model to its filled look). Without fluid it cannot run.
        scene.world().modifyBlock(compressor, s -> s.setValue(CentrifugalCompressorBlock.FULL, true), false);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.centrifugal_compressor.text_3")
                .pointAt(util.vector().centerOf(compressor))
                .attachKeyFrame();
        scene.idle(100);

        // 4) Spin it up — it draws Stress off the network to run.
        createScene.world().setKineticSpeed(drivenLine, 32.0F);
        scene.idle(20);
        scene.overlay().showText(100)
                .text("createhydraulics.ponder.centrifugal_compressor.text_4")
                .pointAt(util.vector().centerOf(compressor))
                .attachKeyFrame();
        scene.idle(110);

        // 5) Pressure leaves the top face into the pipe and shows on the gauge.
        scene.world().showSection(util.select().fromTo(2, 2, 2, 3, 2, 2), Direction.DOWN);
        scene.idle(10);
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class, be -> be.setPressure(256.0F));
        scene.overlay().showOutline(PonderPalette.BLUE, "out", util.select().position(riser), 80);
        scene.overlay().showText(90)
                .text("createhydraulics.ponder.centrifugal_compressor.text_5")
                .pointAt(util.vector().blockSurface(compressor, Direction.UP))
                .attachKeyFrame();
        scene.idle(100);

        // 6) Faster = more Stress eaten, more pressure made.
        createScene.world().setKineticSpeed(drivenLine, 64.0F);
        scene.world().modifyBlockEntity(gauge, FlowGaugeBlockEntity.class, be -> be.setPressure(512.0F));
        scene.overlay().showText(100)
                .text("createhydraulics.ponder.centrifugal_compressor.text_6")
                .pointAt(util.vector().centerOf(gauge))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.markAsFinished();
    }
}
