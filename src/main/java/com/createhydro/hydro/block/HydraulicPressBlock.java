package com.createhydro.hydro.block;

import org.jetbrains.annotations.Nullable;

import com.createhydro.hydro.block.entity.HydraulicPressBlockEntity;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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
 * The Hydraulic Press.
 *
 * <p>The fluid-powered analogue of Create's Mechanical Press &ndash; a "hydraulic reskin" of that
 * machine. It is placed above a Create {@code DepotBlock} and stamps the depot's item into a
 * {@link com.createhydro.hydro.recipe.HydraulicPressRecipe} result. All the behaviour lives in
 * {@link HydraulicPressBlockEntity}.</p>
 *
 * <h2>Orientation</h2>
 * The press always works <b>downward</b> onto the depot below, with its top face reserved for the
 * pressure-pipe input &ndash; that vertical arrangement never changes. The {@link #FACING} property is
 * therefore purely horizontal (cosmetic): it only spins the casing about the vertical axis so the
 * hazard stripes face the way the player placed it, exactly like Create's press uses its horizontal
 * facing. The renderer rotates the moving head by the same facing to keep it aligned.
 *
 * <h2>Fluid</h2>
 * Right-clicking with a fluid container fills the press's working-fluid tank (see
 * {@link HydraulicPressBlockEntity}). Whenever the tank holds fluid the {@link #FULL} property is
 * {@code true}, which swaps the model texture from {@code hydraulic_press_empty} to
 * {@code hydraulic_press_full}.
 */
public class HydraulicPressBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable {

    /** Required since 1.20.5 so the block round-trips through the block codec as its real type. */
    public static final MapCodec<HydraulicPressBlock> CODEC = simpleCodec(HydraulicPressBlock::new);

    /** {@code true} while the working-fluid tank holds fluid; drives the empty/full texture swap. */
    public static final BooleanProperty FULL = BooleanProperty.create("full");

    /**
     * Fitted casing outline. The casing footprint is the full horizontal cross-section but starts {@code 4.5px} up
     * (it straddles the depot below), so the outline is a full block minus that gap. {@link #FACING} is cosmetic
     * (it only spins the casing about Y), so the shape is symmetric and independent of it.
     */
    private static final VoxelShape SHAPE = ModShapes.box(0, 4.5, 0, 16, 16, 16);

    public HydraulicPressBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(FULL, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, FULL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Face the player, like most Create machines. The press still acts downward regardless.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        // Fill (or drain) the press's tank from a held bucket / fluid container via its fluid-handler
        // capability. This is how players load the press with Hydro Fluid; the tank's isFluidValid rejects
        // anything that isn't Hydro Fluid, and filling it flips the model to hydraulic_press_full.
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
        return new HydraulicPressBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Tick on both sides: the server drives the press logic, the client advances the head animation.
        return createTickerHelper(type, ModBlockEntities.HYDRAULIC_PRESS.get(), HydraulicPressBlockEntity::tick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}
