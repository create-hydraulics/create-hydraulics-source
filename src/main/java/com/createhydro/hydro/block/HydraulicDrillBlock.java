package com.createhydro.hydro.block;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.block.entity.HydraulicDrillBlockEntity;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * The Hydraulic Drill.
 *
 * <p>The fluid-powered cousin of Create's Mechanical Drill: instead of spinning under rotational force, a
 * hydraulic breaker head reciprocates &ndash; it drives {@code 4px} out of the front face, one-taps the block in
 * front of it and retracts. It is {@link DirectionalBlock directional} (all six faces): pressure (PU) is taken
 * from a Hardened Iron Pipe on its <b>back</b> face ({@link #FACING}{@code .getOpposite()}) and it carries a small
 * tank of <b>Hydro Fluid</b> as its working medium. All of the behaviour lives in
 * {@link HydraulicDrillBlockEntity}.</p>
 *
 * <h2>Contraptions</h2>
 * Like the Mechanical Drill, when assembled onto a moving contraption it breaks blocks it passes into and routes
 * the drops straight into the contraption's storage (or drops them in the world when there is no room) &ndash; see
 * {@code HydraulicDrillMovementBehaviour}.
 *
 * <h2>Model</h2>
 * The casing is authored pointing {@code -Z} (north). The blockstate rotates it to {@link #FACING}; the moving
 * breaker head is drawn separately by {@code HydraulicDrillRenderer}. {@link #FULL} swaps the empty&harr;fluid
 * texture while the tank holds fluid.
 */
public class HydraulicDrillBlock extends DirectionalBlock implements EntityBlock, IWrenchable {

    /** Required since 1.20.5 so the block round-trips through the block codec as its real type. */
    public static final MapCodec<HydraulicDrillBlock> CODEC = simpleCodec(HydraulicDrillBlock::new);

    /** {@code true} while the working-fluid tank holds fluid; drives the empty/full texture swap. */
    public static final BooleanProperty FULL = BooleanProperty.create("full");

    /**
     * Fitted casing outline per {@link #FACING}. The casing is authored pointing {@code NORTH} (breaker face toward
     * {@code -Z}); the shape is rotated about Y for the horizontal facings and tipped about X for up/down, matching
     * the blockstate. The BER-drawn reciprocating head is not part of the outline.
     */
    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

    private static Map<Direction, VoxelShape> buildShapes() {
        VoxelShape north = ModShapes.box(0, 0, 5, 16, 16, 16); // casing body (front face at z=5, back slab to z=16)
        EnumMap<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, north);
        map.put(Direction.EAST, ModShapes.rotateY(north, 1));
        map.put(Direction.SOUTH, ModShapes.rotateY(north, 2));
        map.put(Direction.WEST, ModShapes.rotateY(north, 3));
        map.put(Direction.DOWN, ModShapes.rotateX(north, 1)); // blockstate x:90
        map.put(Direction.UP, ModShapes.rotateX(north, 3));   // blockstate x:270
        return map;
    }

    public HydraulicDrillBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FULL, false));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FULL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Point the drill into the block face the player is looking at, exactly like Create's Mechanical Drill
        // (a DirectionalKineticBlock): the breaker head then faces away from the player, into whatever they aimed
        // at, and the pressure port ends up on the opposite (back) face.
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
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
        // Load the Drill with Hydro Fluid from a held bucket / container via its fluid-handler capability. The
        // tank's isFluidValid rejects anything that isn't Hydro Fluid; filling it flips the model to its full look.
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // ------------------------------------------------------------------------------------------------
    // Block entity wiring
    // ------------------------------------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HydraulicDrillBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Tick on both sides: the server drives the drill logic, the client advances the head animation.
        return createTickerHelper(type, ModBlockEntities.HYDRAULIC_DRILL.get(), HydraulicDrillBlockEntity::tick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}
