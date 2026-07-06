package com.createhydro.hydro.block;

import java.util.EnumMap;
import java.util.Map;

import com.createhydro.hydro.block.entity.HydraulicMotorBlockEntity;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * The Hydraulic Motor.
 *
 * <p>A fluid-powered <b>kinetic generator</b> &ndash; the hydraulic answer to Create's Creative Motor. It
 * is a real {@link DirectionalKineticBlock}, so its front face ({@link #FACING}) carries a shaft that
 * drives a Create rotation network, and it participates in stress exactly like any other generator. All of
 * the behaviour (pressure gating, fuel burn, RPM, stress capacity) lives in
 * {@link HydraulicMotorBlockEntity}.</p>
 *
 * <h2>Orientation</h2>
 * {@link #FACING} (all six directions) is the shaft / output side. Pressure is taken from the opposite
 * face &ndash; the <b>back</b> &ndash; where a Hardened Iron Pipe connects. The blockstate uses the
 * horizontal body model for horizontal facings and the vertical model for up/down, mirroring Create's
 * creative motor.
 *
 * <h2>Fluid</h2>
 * Right-clicking with a fluid container fills the motor's two-bucket working-fluid tank. Whenever the tank
 * holds fluid the {@link #FULL} property is {@code true}, swapping the body texture to its "full" variant.
 */
public class HydraulicMotorBlock extends DirectionalKineticBlock implements IBE<HydraulicMotorBlockEntity> {

    /** Required since 1.20.5 so the block round-trips through the block codec as its real type. */
    public static final MapCodec<HydraulicMotorBlock> CODEC = simpleCodec(HydraulicMotorBlock::new);

    /** {@code true} while the working-fluid tank holds fluid; drives the empty/full texture swap. */
    public static final BooleanProperty FULL = BooleanProperty.create("full");

    /**
     * Fitted body outline per {@link #FACING}. The horizontal body is authored toward {@code NORTH} (shaft on the
     * front face) and rotated about Y for the other three horizontals; up/down use the shorter vertical body model.
     * The BER-drawn shaft nub is intentionally left out of the outline, exactly like Create's own shafts.
     */
    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

    private static Map<Direction, VoxelShape> buildShapes() {
        VoxelShape north = ModShapes.box(1, 0, 1, 15, 14, 16);   // horizontal body (models/block/hydraulic_motor)
        VoxelShape up = ModShapes.box(2, 0, 2, 14, 15, 14);      // vertical body (models/block/hydraulic_motor_vertical)
        EnumMap<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, north);
        map.put(Direction.EAST, ModShapes.rotateY(north, 1));
        map.put(Direction.SOUTH, ModShapes.rotateY(north, 2));
        map.put(Direction.WEST, ModShapes.rotateY(north, 3));
        map.put(Direction.UP, up);
        map.put(Direction.DOWN, ModShapes.rotateX(up, 2));
        return map;
    }

    public HydraulicMotorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FULL, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FULL);
        super.createBlockStateDefinition(builder); // adds FACING
    }

    // ------------------------------------------------------------------------------------------------
    // IRotate — the shaft sits on the FACING face; the rotation axis is that face's axis.
    // ------------------------------------------------------------------------------------------------

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hideStressImpact() {
        // A generator's own impact is meaningless; hide the "0 stress impact" line like the creative motor.
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    // ------------------------------------------------------------------------------------------------
    // Fluid filling
    // ------------------------------------------------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // Load the motor with Hydro Fluid from a held bucket / container via its fluid-handler capability.
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // ------------------------------------------------------------------------------------------------
    // IBE wiring (provides newBlockEntity + SmartBlockEntityTicker for both sides)
    // ------------------------------------------------------------------------------------------------

    @Override
    public Class<HydraulicMotorBlockEntity> getBlockEntityClass() {
        return HydraulicMotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HydraulicMotorBlockEntity> getBlockEntityType() {
        return ModBlockEntities.HYDRAULIC_MOTOR.get();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Keep Create's auto-alignment to an adjacent shaft; the tank always starts empty.
        return super.getStateForPlacement(context).setValue(FULL, false);
    }
}
