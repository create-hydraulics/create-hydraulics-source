package com.createhydro.hydro.block;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.block.entity.HardenedIronPipeBlockEntity;
import com.createhydro.hydro.block.entity.PressureValveBlockEntity;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Pressure Valve.
 *
 * <p>Identical to the Hardened Iron Pipe for connection and rendering purposes, with one
 * addition: a {@code POWERED} boolean block-state property driven by redstone. When powered
 * ({@code true}), the valve seals off all pressure flow across itself (see
 * {@code PressureValveBlockEntity#isFlowBlocked()}), forming an impassable wall in the line.</p>
 *
 * <h2>Lever / button placement</h2>
 * {@link #getSupportShape} returns a full unit cube so face-attached items (levers, buttons,
 * banners) can always be placed on any face, even when all six stubs are connected.
 */
public class PressureValveBlock extends HardenedIronPipeBlock {

    public static final MapCodec<PressureValveBlock> CODEC = simpleCodec(PressureValveBlock::new);

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public PressureValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(NORTH, false)
                .setValue(EAST,  false)
                .setValue(SOUTH, false)
                .setValue(WEST,  false)
                .setValue(UP,    false)
                .setValue(DOWN,  false)
                .setValue(WATERLOGGED, false)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }

    // ------------------------------------------------------------------------------------------------
    // Block-state wiring
    // ------------------------------------------------------------------------------------------------

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos  = context.getClickedPos();
        boolean powered = level.hasNeighborSignal(pos);
        return super.getStateForPlacement(context).setValue(POWERED, powered);
    }

    // ------------------------------------------------------------------------------------------------
    // Redstone
    // ------------------------------------------------------------------------------------------------

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            boolean powered = level.hasNeighborSignal(pos);
            if (state.getValue(POWERED) != powered) {
                level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Support shape — allows levers/buttons on every face regardless of stub layout
    // ------------------------------------------------------------------------------------------------

    /** Returns a full cube so all six faces are always considered sturdy for attachment purposes. */
    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    // ------------------------------------------------------------------------------------------------
    // Block entity wiring
    // ------------------------------------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PressureValveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.PRESSURE_VALVE.get(),
                HardenedIronPipeBlockEntity::serverTick);
    }
}
