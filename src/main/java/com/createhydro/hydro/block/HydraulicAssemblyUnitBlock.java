package com.createhydro.hydro.block;

import com.createhydro.hydro.block.entity.HydraulicAssemblyUnitBlockEntity;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

public class HydraulicAssemblyUnitBlock extends HorizontalDirectionalBlock
        implements IBE<HydraulicAssemblyUnitBlockEntity>, IWrenchable {

    // required since 1.20.5 so the block round-trips through the block codec as its actual type
    public static final MapCodec<HydraulicAssemblyUnitBlock> CODEC = simpleCodec(HydraulicAssemblyUnitBlock::new);

    // matches the model's main body (1.5–14.5 px in x/z) so the cursor snaps to visible geometry
    private static final VoxelShape SHAPE = Block.box(1.5, 0, 1.5, 14.5, 16, 14.5);

    public static final BooleanProperty FULL = BooleanProperty.create("full");
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    public HydraulicAssemblyUnitBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FULL, false)
                .setValue(RUNNING, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FULL, RUNNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // face the player; pressure always comes from below regardless of facing
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // fluid container: fill the working-fluid tank
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // any other item: set it as the recipe filter
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, be -> be.setFilter(stack.copyWithCount(1)));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Class<HydraulicAssemblyUnitBlockEntity> getBlockEntityClass() {
        return HydraulicAssemblyUnitBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HydraulicAssemblyUnitBlockEntity> getBlockEntityType() {
        return ModBlockEntities.HYDRAULIC_ASSEMBLY_UNIT.get();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // drops buffered items and the filter item, then clears the BE
        IBE.onRemove(state, level, pos, newState);
    }
}
