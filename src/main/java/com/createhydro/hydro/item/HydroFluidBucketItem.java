package com.createhydro.hydro.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * Bucket of Hydro Fluid.
 *
 * <p>It still behaves like a normal bucket for the one thing that matters &ndash; <b>filling machine
 * tanks</b>: NeoForge gives every {@link BucketItem} a fluid-handler capability, so {@link FluidUtil} can
 * empty it into any block that accepts Hydro Fluid (e.g. the Hydraulic Press). What it deliberately
 * <b>cannot</b> do is pour the fluid out into the world: Hydro Fluid is a sealed working fluid, not a
 * pool you place. Right-clicking anything that is not a fluid handler simply does nothing, instead of the
 * vanilla behaviour of emptying a source block onto the ground.</p>
 */
public class HydroFluidBucketItem extends BucketItem {

    public HydroFluidBucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Only ever interact with a block's fluid handler (a machine tank). Never place a fluid source block.
        // (Right-clicking our own machines is already handled by their useItemOn; this covers everything else.)
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() == HitResult.Type.BLOCK
                && FluidUtil.interactWithFluidHandler(player, hand, level, hit.getBlockPos(), hit.getDirection())) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }
}
