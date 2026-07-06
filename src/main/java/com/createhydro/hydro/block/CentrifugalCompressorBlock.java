package com.createhydro.hydro.block;

import com.createhydro.hydro.block.entity.CentrifugalCompressorBlockEntity;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;

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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * The Centrifugal Compressor.
 *
 * <p>A <b>rotation&rarr;pressure converter</b> and the mod's first kinetic-driven pressure source. Unlike the
 * water-fed intakes it is powered by Create rotation: it carries a horizontal shaft on the two black side faces
 * (an {@link HorizontalAxisKineticBlock#HORIZONTAL_AXIS axis}), and while that shaft spins the compressor draws
 * Stress Units off the network and pumps hydraulic pressure (PU) into a Hardened Iron Pipe on its top face.</p>
 *
 * <h2>Orientation</h2>
 * {@link HorizontalAxisKineticBlock#HORIZONTAL_AXIS} (X or Z) is the shaft axis; a shaft connects on both faces
 * along it. The body model is authored with its shaft running north&ndash;south (Z), so the blockstate rotates it
 * 90&deg; for the X axis. Pressure always leaves the top face &ndash; that never changes.
 *
 * <h2>Working fluid</h2>
 * Like the other Hydraulic machines it needs Hydro Fluid to run. Right-clicking with a fluid container fills its
 * one-bucket working-fluid tank; while the tank holds fluid the {@link #FULL} property is {@code true} (swapping
 * the body to its "full" texture) and the compressor is able to work. Empty, it cannot compress. The fluid is
 * held, not consumed &ndash; load it once, like the Press.
 *
 * <h2>Stress</h2>
 * The compressor is a stress <i>consumer</i>, not a generator: it imposes a real load on the rotation network
 * (see {@link CentrifugalCompressorBlockEntity#calculateStressApplied()}), so the faster it is driven the more
 * Stress Units it eats &ndash; and the more pressure it makes. All of that behaviour lives in the block entity.
 *
 * <h2>Model</h2>
 * The static body is the block model; the rotating shaft is drawn by {@code CentrifugalCompressorRenderer} using
 * Create's {@code SHAFT_HALF} partial on both axis faces, so it lines up with ordinary Create shafts.
 */
public class CentrifugalCompressorBlock extends HorizontalAxisKineticBlock implements IBE<CentrifugalCompressorBlockEntity> {

    /** Required since 1.20.5 so the block round-trips through the block codec as its real type. */
    public static final MapCodec<CentrifugalCompressorBlock> CODEC = simpleCodec(CentrifugalCompressorBlock::new);

    /** {@code true} while the working-fluid tank holds fluid; drives the empty/full texture swap. */
    public static final BooleanProperty FULL = BooleanProperty.create("full");

    /**
     * Fitted body outline for the Z (north&ndash;south) shaft axis &ndash; the body spans the block along the shaft
     * and is inset on the other horizontal axis, only {@code 14px} tall. The {@code X} shape is this rotated 90°.
     */
    private static final VoxelShape SHAPE_Z = ModShapes.box(1, 0, 0, 15, 14, 16);
    private static final VoxelShape SHAPE_X = ModShapes.rotateY(SHAPE_Z, 1);

    public CentrifugalCompressorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FULL, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FULL);
        super.createBlockStateDefinition(builder); // adds HORIZONTAL_AXIS
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Keep Create's auto-alignment of the shaft axis to an adjacent shaft; the tank always starts empty.
        return super.getStateForPlacement(context).setValue(FULL, false);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HORIZONTAL_AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
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
        // Load the compressor with Hydro Fluid from a held bucket / container via its fluid-handler capability.
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // ------------------------------------------------------------------------------------------------
    // IBE wiring (provides newBlockEntity + SmartBlockEntityTicker for both sides)
    // ------------------------------------------------------------------------------------------------

    @Override
    public Class<CentrifugalCompressorBlockEntity> getBlockEntityClass() {
        return CentrifugalCompressorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CentrifugalCompressorBlockEntity> getBlockEntityType() {
        return ModBlockEntities.CENTRIFUGAL_COMPRESSOR.get();
    }
}
