package com.createhydro.hydro.block;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.block.entity.AqueductIntakeBlockEntity;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Aqueduct Intake.
 *
 * <p>A hydraulic <b>pressure source</b>. When fully submerged &ndash; all six neighbouring blocks are water
 * (a waterlogged Hardened Iron Pipe on top counts as water, which is how the top output face is satisfied)
 * &ndash; it draws water in from below and pressurizes the pipe network on its top face to
 * {@link AqueductIntakeBlockEntity#OUTPUT_PRESSURE} PU (filling at 100 PU/second). The instant any face
 * stops touching water it shuts off and the unfed line depressurizes on its own.</p>
 *
 * <h2>Connection</h2>
 * Pressure leaves <b>only</b> from the top face, into a {@link HardenedIronPipeBlock} placed directly above.
 * The intake never connects on any other face: the pipe's own rule
 * ({@link HardenedIronPipeBlock#canConnectTo}) recognises the intake only when the pipe sits above it, and the
 * block entity only ever feeds the segment directly overhead.
 *
 * <h2>Blockstate</h2>
 * A single {@link #ACTIVE} boolean drives the active/inactive model swap (set by the block entity from the
 * submersion check each tick). The block never rotates &ndash; its output is always up. It is also
 * {@link SimpleWaterloggedBlock waterloggable}, so when placed underwater it holds water and renders as
 * submerged (the model is {@code noOcclusion}, so water shows through its open volume).
 */
public class AqueductIntakeBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {

    /** Required since 1.20.5 so the block round-trips through the block codec as its real type. */
    public static final MapCodec<AqueductIntakeBlock> CODEC = simpleCodec(AqueductIntakeBlock::new);

    /** {@code true} while the intake is fully submerged and pressurizing the line; drives the model swap. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** Whether the block contains a water source, so it renders submerged and the water tracks neighbours. */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Fitted body outline: full-width but inset front-to-back, matching the model. The block never rotates. */
    private static final VoxelShape SHAPE = ModShapes.box(0, 0, 3, 16, 16, 13);

    public AqueductIntakeBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(ACTIVE, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, WATERLOGGED);
    }

    // ------------------------------------------------------------------------------------------------
    // Waterlogging
    // ------------------------------------------------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean waterlogged = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState().setValue(WATERLOGGED, waterlogged);
    }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // ------------------------------------------------------------------------------------------------
    // Block entity wiring
    // ------------------------------------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AqueductIntakeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null; // submersion check and pressure output are server-authoritative
        }
        return createTickerHelper(type, ModBlockEntities.AQUEDUCT_INTAKE.get(), AqueductIntakeBlockEntity::serverTick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}
