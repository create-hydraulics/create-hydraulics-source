package com.createhydro.hydro.block;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.block.entity.HydraulicFistBlockEntity;
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
 * The Hydraulic Fist.
 *
 * <p>A pressure-powered punching machine &ndash; the hydraulic counterpart to Create's Deployer, but applying
 * raw mechanical force rather than mimicking a player's hand. It is a <b>vertical-only</b> machine: its
 * {@link #FACING} is always {@link Direction#DOWN}, so it punches straight down onto whatever sits below it, and
 * pressure (PU) is taken from a Hardened Iron Pipe on the opposite (<b>top</b>) face. All of the behaviour lives
 * in {@link HydraulicFistBlockEntity}.</p>
 *
 * <h2>Orientation &amp; model</h2>
 * Because the Fist only ever faces down, it is placed facing {@code DOWN} regardless of how the player is aiming
 * ({@link #getStateForPlacement}) and a wrench cannot rotate it off vertical ({@link #getRotatedBlockState}) —
 * making it foolproof to drop onto a depot table or belt. The blockstate still keeps {@link #FACING} so the
 * existing body model (and the renderer) stay aligned; {@link #FULL} swaps the empty&harr;fluid-loaded texture.
 * The moving fist (and its telescoping pole) is drawn by {@code HydraulicFistRenderer}.
 *
 * <h2>Fluid</h2>
 * Right-clicking with a fluid container fills the Fist's working-fluid tank. Whenever the tank holds fluid the
 * {@link #FULL} property is {@code true}. The fluid is the working medium &ndash; loaded once, required to be
 * present, but <b>not</b> consumed per punch.
 */
public class HydraulicFistBlock extends DirectionalBlock implements EntityBlock, IWrenchable {

    /** Required since 1.20.5 so the block round-trips through the block codec as its real type. */
    public static final MapCodec<HydraulicFistBlock> CODEC = simpleCodec(HydraulicFistBlock::new);

    /** {@code true} while the working-fluid tank holds fluid; drives the empty/full texture swap. */
    public static final BooleanProperty FULL = BooleanProperty.create("full");

    /**
     * Fitted casing outline. The Fist is always {@link Direction#DOWN vertical}, so a single shape suffices: the
     * casing body fills the block from {@code 4px} up (the bottom gap is where the BER-drawn pole/hand punches out).
     */
    private static final VoxelShape SHAPE = ModShapes.box(0, 4, 0, 16, 16, 16);

    public HydraulicFistBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.DOWN)
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
        // The Fist is a vertical-only machine: it always punches straight down (onto a depot/belt below) and takes
        // its pressure from a pipe on its top face. So it is placed facing DOWN no matter where the player aims,
        // which makes dropping it onto a depot table or belt foolproof.
        return defaultBlockState().setValue(FACING, Direction.DOWN);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        // Vertical-only: a wrench must never twist the Fist onto a horizontal facing. Keep it pointing DOWN so the
        // punch stays aimed at the depot/belt below and the pressure port stays on top.
        return originalState;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        // Load the Fist with Hydro Fluid from a held bucket / container via its fluid-handler capability. The
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
        return new HydraulicFistBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Tick on both sides: the server drives the punch logic, the client advances the fist animation.
        return createTickerHelper(type, ModBlockEntities.HYDRAULIC_FIST.get(), HydraulicFistBlockEntity::tick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}
