package com.createhydro.hydro.block;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.block.entity.FlowGaugeBlockEntity;
import com.createhydro.hydro.block.entity.HardenedIronPipeBlockEntity;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluids;

/**
 * The Flow Gauge.
 *
 * <p>A pressure pipe segment with a dial that reads the hydraulic pressure running through it. Unlike a plain
 * {@link HardenedIronPipeBlock}, the gauge is a <b>horizontal inline device</b>:</p>
 * <ul>
 *   <li><b>A fixed pass-through axis.</b> A {@link #FACING} property picks the horizontal axis the gauge sits
 *       on; its two pass-through stubs are <i>always</i> drawn along that axis (like a length of pipe), and the
 *       dial body sits to the side. It connects &mdash; and lets pressure cross &mdash; only along that axis.</li>
 *   <li><b>It orients on placement.</b> {@link #getStateForPlacement} rotates the gauge so a stub points at the
 *       pipe you clicked (or aligns with an adjacent pipe / your facing), so it drops straight into a run
 *       instead of landing sideways.</li>
 *   <li><b>It indicates.</b> Its block entity turns pressure into a needle angle; the needle is drawn and
 *       rotated by {@code FlowGaugeRenderer}, which is why the block model leaves the needle out.</li>
 * </ul>
 */
public class FlowGaugeBlock extends HardenedIronPipeBlock implements IWrenchable {

    public static final MapCodec<FlowGaugeBlock> CODEC = simpleCodec(FlowGaugeBlock::new);

    /** The horizontal axis the gauge's pass-through runs along; also drives the model's rotation. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public FlowGaugeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    // ------------------------------------------------------------------------------------------------
    // Orientation & connection — a fixed horizontal pass-through along FACING, nothing on the other axes
    // ------------------------------------------------------------------------------------------------

    /**
     * The gauge's shape never depends on its neighbours: it always shows both pass-through stubs along its
     * {@link #FACING} axis (driving collision) and nothing on the other four faces. Rendering is keyed on
     * {@code FACING} (the blockstate rotates a single model), so the boolean stub values here only feed the
     * collision shape.
     */
    @Override
    public BlockState getConnectedState(BlockState state, BlockGetter level, BlockPos pos) {
        Direction.Axis axis = state.getValue(FACING).getAxis();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), dir.getAxis() == axis);
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean waterlogged = level.getFluidState(pos).getType() == Fluids.WATER;
        BlockState base = defaultBlockState()
                .setValue(WATERLOGGED, waterlogged)
                .setValue(FACING, chooseFacing(context));
        return getConnectedState(base, level, pos);
    }

    /**
     * Pick the horizontal axis the gauge should sit on so it connects cleanly:
     * <ol>
     *   <li>If you clicked the side of a pipe, point a stub straight back at it.</li>
     *   <li>Otherwise, align with any adjacent pipe so it joins the run.</li>
     *   <li>Failing that, fall back to the way you're facing.</li>
     * </ol>
     */
    private Direction chooseFacing(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        Direction towardClicked = context.getClickedFace().getOpposite();
        if (towardClicked.getAxis().isHorizontal() && canConnectTo(level, pos, towardClicked)) {
            return towardClicked;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (canConnectTo(level, pos, dir)) {
                return dir;
            }
        }
        return context.getHorizontalDirection();
    }

    // ------------------------------------------------------------------------------------------------
    // Block entity wiring
    // ------------------------------------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FlowGaugeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null; // pressure logic is server-authoritative; the needle reads synced state
        }
        return createTickerHelper(type, ModBlockEntities.FLOW_GAUGE.get(), HardenedIronPipeBlockEntity::serverTick);
    }
}
