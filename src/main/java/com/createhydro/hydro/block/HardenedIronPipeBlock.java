package com.createhydro.hydro.block;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.block.entity.HardenedIronPipeBlockEntity;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * The Hardened Iron Pipe.
 *
 * <p>This extends vanilla {@link PipeBlock} &ndash; the same base Create's own {@code FluidPipeBlock} uses &ndash;
 * so we inherit the six directional boolean properties ({@code NORTH/EAST/SOUTH/WEST/UP/DOWN}) and their
 * collision-shape handling for free.</p>
 *
 * <h2>Connection &amp; rendering rule</h2>
 * The six booleans drive the multipart blockstate (one model arm per {@code true} side, plus the central
 * core). We compute them by re-implementing Create's {@code FluidPipeBlock#updateBlockState} rule so the pipe
 * <i>looks</i> like a Create fluid pipe (the connection logic itself is pressure-only &ndash; see
 * {@link #canConnectTo}):
 * <ul>
 *   <li><b>2+ real connections</b> &ndash; draw an arm to each (corner / tee / cross).</li>
 *   <li><b>exactly 1 connection</b> &ndash; also draw the opposite arm, forming a straight pass-through.</li>
 *   <li><b>0 connections</b> &ndash; default to a straight EAST&ndash;WEST pipe, so the west &amp; east arms
 *       are always visible until a real connection turns the pipe into a corner.</li>
 * </ul>
 *
 * <h2>Model files</h2>
 * The source BlockBench export lives at
 * {@code src/main/resources/assets/createhydraulics/models/hardened_iron_pipe.json}. Its groups are split into
 * the per-arm models referenced by {@code blockstates/hardened_iron_pipe.json}
 * (see {@code models/block/hardened_iron_pipe_core.json} and {@code ..._arm_<dir>.json}). If you re-export the
 * BlockBench model, regenerate those split models to match.
 */
public class HardenedIronPipeBlock extends PipeBlock implements SimpleWaterloggedBlock, EntityBlock {

    /** Required since 1.20.5 so the block round-trips through the block codec as its real type. */
    public static final MapCodec<HardenedIronPipeBlock> CODEC = simpleCodec(HardenedIronPipeBlock::new);

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * Half-width of the pipe core, in block units. 4/16 spans pixels 4..12, matching the {@code center_node}
     * footprint of the BlockBench model so the collision box lines up with what you see.
     */
    private static final float APOTHEM = 4.0F / 16.0F;

    public HardenedIronPipeBlock(Properties properties) {
        super(APOTHEM, properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    // ------------------------------------------------------------------------------------------------
    // Connection logic
    // ------------------------------------------------------------------------------------------------

    /**
     * Whether this pipe connects toward {@code direction}. Hydraulic pipes are a self-contained pressure
     * network: they connect to other hydraulic pipes (and their relatives, the valve and gauge), and to the
     * {@link AqueductIntakeBlock}'s top face when this pipe sits directly above one. They deliberately do
     * <b>not</b> connect to Create's fluid network or to any {@code IFluidHandler} &ndash; this pipe carries
     * pressure, never fluid.
     */
    protected boolean canConnectTo(BlockGetter level, BlockPos pos, Direction direction) {
        BlockState neighbor = level.getBlockState(pos.relative(direction));
        if (neighbor.getBlock() instanceof HardenedIronPipeBlock) {
            // Pipe-to-pipe (and pipe-to-valve/gauge) is decided from block state alone, so it works even
            // before neighbour BEs exist and during world generation.
            return true;
        }
        if (neighbor.getBlock() instanceof AqueductIntakeBlock) {
            // The Aqueduct Intake outputs pressure on its top face only, so a pipe connects to it solely when
            // the pipe is directly above it (the side being tested points down into the intake).
            return direction == Direction.DOWN;
        }
        if (neighbor.getBlock() instanceof HydraulicPressBlock) {
            // The Hydraulic Press takes its pressure input on its top face, so a pipe connects to it solely
            // when the pipe sits directly above it (the side being tested points down into the press).
            return direction == Direction.DOWN;
        }
        if (neighbor.getBlock() instanceof HydrostaticIntakeBlock) {
            // The Hydrostatic Intake outputs pressure on its bottom face only, so a pipe connects to it solely
            // when the pipe sits directly below it (the side being tested points up into the intake).
            return direction == Direction.UP;
        }
        if (neighbor.getBlock() instanceof CentrifugalCompressorBlock) {
            // The Centrifugal Compressor outputs pressure on its top face only, so a pipe connects to it solely
            // when the pipe sits directly above it (the side being tested points down into the compressor).
            return direction == Direction.DOWN;
        }
        if (neighbor.getBlock() instanceof HydraulicMotorBlock) {
            // The Hydraulic Motor takes its pressure input on its back face (opposite the shaft/FACING). A pipe
            // sits on that back face exactly when the direction from the pipe toward the motor equals the motor's
            // facing, so the pipe connects to the motor only there.
            return direction == neighbor.getValue(HydraulicMotorBlock.FACING);
        }
        if (neighbor.getBlock() instanceof HydraulicFistBlock) {
            // The Hydraulic Fist takes its pressure input on its back face (opposite the punch/FACING). Since the
            // Fist only ever punches downward, that back face is its top — a pipe connects to it solely when it
            // sits directly above (the side being tested points down into the Fist, matching its FACING).
            return direction == neighbor.getValue(HydraulicFistBlock.FACING);
        }
        if (neighbor.getBlock() instanceof HydraulicDrillBlock) {
            // The Hydraulic Drill takes its pressure input on its back face (opposite the breaker/FACING). A pipe
            // sits on that back face exactly when the direction from the pipe toward the drill equals the drill's
            // facing, so the pipe draws an arm to it (and reads pressure) only there.
            return direction == neighbor.getValue(HydraulicDrillBlock.FACING);
        }
        if (neighbor.getBlock() instanceof HydraulicAssemblyUnitBlock) {
            // The Assembly Unit draws pressure from the pipe on its bottom face only — a pipe connects to it
            // solely when the pipe sits directly below it (the side being tested points up into the unit).
            return direction == Direction.UP;
        }
        return false;
    }

    /**
     * Recompute the six connection booleans for this pipe, applying Create's straight-pipe defaulting rule.
     * Preserves all other properties (e.g. {@link #WATERLOGGED}).
     */
    public BlockState getConnectedState(BlockState state, BlockGetter level, BlockPos pos) {
        int connections = 0;
        Direction onlyConnection = null;

        for (Direction dir : Direction.values()) {
            boolean connected = canConnectTo(level, pos, dir);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), connected);
            if (connected) {
                connections++;
                onlyConnection = dir;
            }
        }

        if (connections == 0) {
            // Default appearance: a straight EAST-WEST pipe (west & east arms visible).
            return state.setValue(EAST, true).setValue(WEST, true);
        }
        if (connections == 1) {
            // A single connection becomes a straight pass-through along that axis.
            return state.setValue(PROPERTY_BY_DIRECTION.get(onlyConnection.getOpposite()), true);
        }
        // 2+ connections: keep the real connections (corner / tee / cross).
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean waterlogged = level.getFluidState(pos).getType() == Fluids.WATER;
        BlockState base = defaultBlockState().setValue(WATERLOGGED, waterlogged);
        return getConnectedState(base, level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        // A neighbour changed: recompute every connection so the pipe re-shapes immediately.
        return getConnectedState(state, level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    // ------------------------------------------------------------------------------------------------
    // Block entity wiring
    // ------------------------------------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HardenedIronPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null; // all fluid/pressure logic is server-authoritative
        }
        return createTickerHelper(type, ModBlockEntities.HARDENED_IRON_PIPE.get(), HardenedIronPipeBlockEntity::serverTick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}
