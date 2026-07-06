package com.createhydro.hydro.block.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.HydrostaticIntakeBlock;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Block entity for the Hydrostatic Intake.
 *
 * <p>Every server tick it does three things:</p>
 * <ol>
 *   <li><b>Column check</b> &ndash; counts the contiguous stack of water <i>source</i> blocks directly above the
 *       intake (see {@link #countWaterColumn}), capped at {@link #MAX_WATER_SOURCES}.</li>
 *   <li><b>State sync</b> &ndash; writes whether it is running to the {@link HydrostaticIntakeBlock#ACTIVE}
 *       blockstate (for the model swap) and caches the column height so the Engineer's Goggles overlay can read
 *       it on the client.</li>
 *   <li><b>Pressure output</b> &ndash; while active, it pressurizes the pipe network on its <b>bottom</b> face to
 *       {@link #outputFor(int)} PU (filling at {@link HardenedIronPipeBlockEntity#PRESSURE_FILL_PER_SECOND}
 *       PU/second via {@link HardenedIronPipeBlockEntity#floodFromSource}). Remove the water and it goes idle, and
 *       the unfed line depressurizes on its own.</li>
 * </ol>
 *
 * <h2>Tuning</h2>
 * The output curve is defined by three constants below &ndash; {@link #MAX_WATER_SOURCES},
 * {@link #OUTPUT_AT_ONE_SOURCE} and {@link #OUTPUT_AT_MAX_SOURCES}. Change those three values to re-tune the
 * machine; everything else (the goggle readout, the connection logic, the ponder scene's numbers) derives from
 * them, so no other code needs to be touched. Output scales linearly between the two endpoints so that exactly
 * one source yields {@code OUTPUT_AT_ONE_SOURCE} PU and a full column of {@code MAX_WATER_SOURCES} yields
 * {@code OUTPUT_AT_MAX_SOURCES} PU.
 *
 * <p>With Create's Engineer's Goggles equipped, hovering the intake shows whether it is running, the height of
 * the water column it is reading, and its output pressure; see {@link #addToGoggleTooltip}.</p>
 */
public class HydrostaticIntakeBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    // --- Tuning constants: edit these three to re-tune the machine; the rest of the code follows. ------------
    /** Most water sources the intake will read from the column above it (the column is counted up to here). */
    public static final int MAX_WATER_SOURCES = 20;
    /** Pressure (PU) generated with a single water source directly above &ndash; the low end of the curve. */
    public static final float OUTPUT_AT_ONE_SOURCE = 200.0F;
    /** Pressure (PU) generated with a full column of {@link #MAX_WATER_SOURCES} &ndash; the high end of the curve. */
    public static final float OUTPUT_AT_MAX_SOURCES = 600.0F;

    /** Cached height of the water column above the intake (0..{@link #MAX_WATER_SOURCES}); synced to clients. */
    private int waterSources = 0;

    public HydrostaticIntakeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDROSTATIC_INTAKE.get(), pos, state);
    }

    /**
     * The pressure (PU) the intake generates for a given column height, scaling linearly between
     * {@link #OUTPUT_AT_ONE_SOURCE} (at 1 source) and {@link #OUTPUT_AT_MAX_SOURCES} (at
     * {@link #MAX_WATER_SOURCES} sources). Zero sources means no output.
     */
    public static float outputFor(int sources) {
        if (sources <= 0) {
            return 0.0F;
        }
        int clamped = Math.min(sources, MAX_WATER_SOURCES);
        if (MAX_WATER_SOURCES <= 1) {
            return OUTPUT_AT_MAX_SOURCES; // degenerate config: a single-source machine just outputs the high end
        }
        float t = (clamped - 1) / (float) (MAX_WATER_SOURCES - 1); // 0 at 1 source, 1 at MAX sources
        return OUTPUT_AT_ONE_SOURCE + (OUTPUT_AT_MAX_SOURCES - OUTPUT_AT_ONE_SOURCE) * t;
    }

    /** The column height this intake is currently reading (0..{@link #MAX_WATER_SOURCES}). */
    public int getWaterSources() {
        return waterSources;
    }

    /** Server-side tick: re-count the column, sync the active state/height, and pressurize the pipe below. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, HydrostaticIntakeBlockEntity intake) {
        int sources = countWaterColumn(level, pos);
        boolean active = sources > 0;

        // Reflect activity in the blockstate so the active/inactive model swaps. Only write on a real change.
        if (state.getValue(HydrostaticIntakeBlock.ACTIVE) != active) {
            state = state.setValue(HydrostaticIntakeBlock.ACTIVE, active);
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }

        // Cache the column height for the goggle overlay and push it to clients when it changes. The height only
        // moves when a player edits the column, so an immediate sync (rather than a throttled one) is cheap.
        if (sources != intake.waterSources) {
            intake.waterSources = sources;
            intake.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        if (!active) {
            return; // no column → generate nothing; the line depressurizes by itself
        }

        // Output is bottom-face only: flood the pipe network starting at the segment directly below. Respect the
        // neighbour's own connection rule (and the closed-valve case) so a sealed face is never fed.
        BlockPos outputPos = pos.below();
        if (level.getBlockEntity(outputPos) instanceof HardenedIronPipeBlockEntity pipe
                && !pipe.isFlowBlocked() && pipe.connectsOnSide(Direction.UP)) {
            HardenedIronPipeBlockEntity.floodFromSource(level, outputPos, outputFor(sources));
        }
    }

    /**
     * Count the contiguous run of water <i>source</i> blocks stacked directly above the intake, starting one
     * block up and stopping at the first block that is not a water source (or once {@link #MAX_WATER_SOURCES}
     * have been counted). A waterlogged block counts, since its fluid state is a water source.
     */
    private static int countWaterColumn(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        int count = 0;
        for (int i = 0; i < MAX_WATER_SOURCES; i++) {
            cursor.move(Direction.UP);
            FluidState fluid = level.getFluidState(cursor);
            if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) {
                break;
            }
            count++;
        }
        return count;
    }

    // ------------------------------------------------------------------------------------------------
    // Engineer's Goggles overlay
    // ------------------------------------------------------------------------------------------------

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean active = waterSources > 0;

        // Heading (default white, matching Create's own goggle panels).
        lang().translate("gui.goggles.hydrostatic_intake").forGoggles(tooltip);

        if (active) {
            // Status line: a green dot + label.
            lang().add(dot(ChatFormatting.GREEN))
                    .add(lang().translate("gui.hydrostatic_intake.active").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
            // Column line: "Water column  7 / 20".
            lang().add(lang().translate("gui.hydrostatic_intake.column").style(ChatFormatting.GRAY))
                    .space()
                    .add(lang().text(ChatFormatting.AQUA, waterSources + " / " + MAX_WATER_SOURCES))
                    .forGoggles(tooltip, 1);
            // Output line: "Output  315 PU" (rounded to a whole PU for a clean readout).
            lang().add(lang().translate("gui.hydrostatic_intake.output").style(ChatFormatting.GRAY))
                    .space()
                    .add(lang().text(LangNumberFormat.format(Math.round(outputFor(waterSources)))).style(ChatFormatting.GOLD))
                    .add(lang().text(ChatFormatting.DARK_GRAY, " PU"))
                    .forGoggles(tooltip, 1);
        } else {
            // Idle: a red dot + label.
            lang().add(dot(ChatFormatting.RED))
                    .add(lang().translate("gui.hydrostatic_intake.inactive").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        }
        return true;
    }

    /** A small status bullet, e.g. green when running / red when idle. */
    private static LangBuilder dot(ChatFormatting color) {
        return lang().text(color, "■ ");
    }

    private static LangBuilder lang() {
        return new LangBuilder(CreateHydraulics.MODID);
    }

    // ------------------------------------------------------------------------------------------------
    // Persistence & client sync — the column height is mirrored to clients for the goggle overlay
    // ------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("WaterSources", waterSources);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        waterSources = tag.getInt("WaterSources");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }
}
