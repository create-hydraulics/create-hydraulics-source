package com.createhydro.hydro.contraption;

import com.createhydro.hydro.block.HydraulicDrillBlock;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Makes a Hydraulic Drill act as a contraption block-breaker, mirroring Create's {@code DrillMovementBehaviour}.
 *
 * <p>Once assembled onto a moving contraption the drill breaks the blocks it advances into and &mdash; through the
 * inherited {@link BlockBreakingMovementBehaviour}/{@code MovementBehaviour#collectOrDropItem} &mdash; funnels the
 * drops into the contraption's storage when there is room, or drops them into the world otherwise. The break is
 * driven by the contraption's motion (not the drill's own pressure line), exactly like the Mechanical Drill: a
 * pipe network can't travel with the contraption, so on a moving rig the drill simply works.</p>
 *
 * <p><b>One-tap snap, not a grind.</b> Create's stock {@link BlockBreakingMovementBehaviour} chews a block away
 * gradually &mdash; accumulating destroy progress over many ticks and rendering the cracking overlay &mdash; the way
 * the Mechanical Drill mines. The stationary Hydraulic Drill instead drives its breaker head out and <i>snaps</i> the
 * block in a single hit. To keep the two consistent, {@link #visitNewPosition} below shatters the visited block
 * instantly (one tap) rather than starting the gradual progress, so a contraption-mounted drill reads the same as
 * its stationary self.</p>
 */
public class HydraulicDrillMovementBehaviour extends BlockBreakingMovementBehaviour {

    @Override
    public boolean isActive(MovementContext context) {
        // Only bite while advancing into the block in front of the drill — not when it is being dragged backwards
        // (the same guard Create's drill uses, so a drill mounted "trailing edge" doesn't churn pointlessly).
        return super.isActive(context)
                && !VecHelper.isVecPointingTowards(context.relativeMotion,
                        context.state.getValue(HydraulicDrillBlock.FACING).getOpposite());
    }

    /**
     * Break the block the drill advances into in a single hit, instead of Create's multi-tick grind. Mirrors the
     * structure of {@link BlockBreakingMovementBehaviour#visitNewPosition} (entity damage + the redstone-conductor
     * and {@link #canBreak} guards), but where the parent would record a {@code BreakingPos} and stall to ramp the
     * destroy progress, this destroys the block at once and routes the drops into the contraption's storage via the
     * inherited {@code collectOrDropItem}. {@code onBlockBroken} is still called so falling-block columns are
     * handled the same way the parent handles them.
     */
    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        Level world = context.world;
        BlockState state = world.getBlockState(pos);

        if (!state.isRedstoneConductor(world, pos)) {
            damageEntities(context, pos, world);
        }
        if (world.isClientSide) {
            return;
        }
        if (!canBreak(world, pos, state)) {
            return;
        }
        // One-tap "snap" like the stationary drill: shatter the block now and bank/scatter its drops. No stall, no
        // gradual progress overlay.
        destroyBlock(context, pos);
        onBlockBroken(context, pos, state);
    }

    @Override
    public Vec3 getActiveAreaOffset(MovementContext context) {
        // The breaking probe sits just past the drill's front face, in the direction it points.
        return Vec3.atLowerCornerOf(context.state.getValue(HydraulicDrillBlock.FACING).getNormal()).scale(0.65f);
    }

    @Override
    public boolean canBreak(Level world, BlockPos breakingPos, BlockState state) {
        // Only solid blocks, matching the Mechanical Drill (skip pass-through decorations and unbreakables).
        return super.canBreak(world, breakingPos, state)
                && !state.getCollisionShape(world, breakingPos).isEmpty();
    }
}
