package com.createhydro.hydro.block;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.block.entity.HydrostaticIntakeBlockEntity;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Hydrostatic Intake.
 *
 * <p>A mid-game, fully <b>passive</b> hydraulic <b>pressure source</b>. It reads the column of water stacked
 * directly above it: the taller the column of water <i>sources</i>, the higher the pressure it pushes &ndash;
 * exactly like the hydrostatic pressure at the bottom of a standpipe. One source above generates
 * {@link HydrostaticIntakeBlockEntity#OUTPUT_AT_ONE_SOURCE} PU; the count is honoured up to
 * {@link HydrostaticIntakeBlockEntity#MAX_WATER_SOURCES} sources, at which point it generates
 * {@link HydrostaticIntakeBlockEntity#OUTPUT_AT_MAX_SOURCES} PU. In between the output scales linearly with the
 * column height (see {@link HydrostaticIntakeBlockEntity#outputFor(int)}).</p>
 *
 * <h2>Connection</h2>
 * Pressure leaves <b>only</b> from the <b>bottom</b> face, into a {@link HardenedIronPipeBlock} placed directly
 * below. The intake never connects on any other face: the pipe's own rule
 * ({@link HardenedIronPipeBlock#canConnectTo}) recognises the intake only when the pipe sits below it, and the
 * block entity only ever feeds the segment directly underneath. Unlike the {@link AqueductIntakeBlock} (a
 * submerged source that outputs upward), this is a dry machine that taps a water column overhead and outputs
 * downward.
 *
 * <h2>Blockstate</h2>
 * A single {@link #ACTIVE} boolean drives the active/inactive model swap (set by the block entity each tick from
 * its column check). The block never rotates &ndash; its output is always down and the water column it reads is
 * always up.
 */
public class HydrostaticIntakeBlock extends Block implements EntityBlock {

    /** Required since 1.20.5 so the block round-trips through the block codec as its real type. */
    public static final MapCodec<HydrostaticIntakeBlock> CODEC = simpleCodec(HydrostaticIntakeBlock::new);

    /** {@code true} while at least one water source sits above the intake and it is pressurizing the line. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public HydrostaticIntakeBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    // ------------------------------------------------------------------------------------------------
    // Block entity wiring
    // ------------------------------------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HydrostaticIntakeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null; // column check and pressure output are server-authoritative
        }
        return createTickerHelper(type, ModBlockEntities.HYDROSTATIC_INTAKE.get(), HydrostaticIntakeBlockEntity::serverTick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}
