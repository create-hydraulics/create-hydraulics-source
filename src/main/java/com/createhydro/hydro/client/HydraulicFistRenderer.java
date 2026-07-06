package com.createhydro.hydro.client;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.Config;
import com.createhydro.hydro.block.HydraulicFistBlock;
import com.createhydro.hydro.block.entity.HydraulicFistBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Renders the Hydraulic Fist's moving fist and its telescoping pole.
 *
 * <p>The body (casing) is static and drawn by the normal block model via the blockstate &ndash; this renderer
 * never touches it except to tint it during an overstress flash. It draws the two moving pieces:</p>
 * <ul>
 *   <li>the <b>hand</b> (the fist itself), translated outward along the {@link HydraulicFistBlock#FACING facing}
 *       by the punch's current extension, and</li>
 *   <li>the <b>pole</b> (the piston rod), scaled along the facing axis so it stretches to bridge the body and the
 *       extended hand &ndash; a true telescoping rod rather than a fixed stub that would float away from the
 *       body. This is what lets the fist reach several blocks out while still looking connected.</li>
 * </ul>
 *
 * <p>The pole and hand are authored once, pointing along their local <b>+Z</b> punch axis (the horizontal art).
 * For every facing this renderer rotates that local +Z to point along {@code FACING} &ndash; spinning about Y for
 * the four horizontal facings and tipping about X for {@link Direction#UP up}/{@link Direction#DOWN down}
 * &ndash; exactly the same rotation the blockstate applies to the static casing, so hand, pole and casing stay
 * glued together for all six orientations. Two texture variants (empty vs fluid-loaded, selected by
 * {@link HydraulicFistBlock#FULL}) keep the moving parts matching the body's look.</p>
 */
public class HydraulicFistRenderer implements BlockEntityRenderer<HydraulicFistBlockEntity> {

    // Head pieces (authored pointing +Z), empty + fluid-loaded.
    public static final ModelResourceLocation POLE = standalone("block/hydraulic_fist_pole");
    public static final ModelResourceLocation POLE_FULL = standalone("block/hydraulic_fist_pole_full");
    public static final ModelResourceLocation HAND = standalone("block/hydraulic_fist_hand");
    public static final ModelResourceLocation HAND_FULL = standalone("block/hydraulic_fist_hand_full");

    /** Modelled length of the pole along its +Z punch axis, in blocks (10px), used to derive the stretch factor. */
    private static final float REST_POLE_LENGTH = 10.0F / 16.0F;
    /** Pole anchor on the +Z punch axis: the rod's fixed back end at z = 2px. */
    private static final float POLE_ANCHOR_Z = 2.0F / 16.0F;
    /** As authored (zero extension), the fingertip pokes this far past the front face, in blocks (~5px). */
    private static final float FINGERTIP_REST_POKE = 5.0F / 16.0F;
    /**
     * Where the fingertip parks at idle: a touch (1px) <em>inside</em> the front face, so on this vertical-only
     * machine it never clips the depot/belt sitting directly below. A punch then drives it down only as far as the
     * block entity's {@link HydraulicFistBlockEntity#getPunchTravel() punch depth} — i.e. onto the target's surface,
     * ~3px (depot) / ~4px (belt) below the face — instead of plunging a half-block down through it.
     */
    private static final float REST_FINGERTIP_DEPTH = -1.0F / 16.0F;

    /** How far the overstress flash mixes the fist toward pure red/green at its peak (held below 1 for a tint). */
    private static final float TINT_STRENGTH = 0.75F;

    private final BlockRenderDispatcher blockRenderer;

    public HydraulicFistRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(HydraulicFistBlockEntity fist, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = fist.getBlockState();
        Direction facing = state.getValue(HydraulicFistBlock.FACING);
        boolean full = state.hasProperty(HydraulicFistBlock.FULL) && state.getValue(HydraulicFistBlock.FULL);

        BakedModel pole = model(full ? POLE_FULL : POLE);
        BakedModel hand = model(full ? HAND_FULL : HAND);
        if (pole == null || hand == null) {
            return; // models not loaded — draw nothing rather than crash
        }

        // Overstress flash (ported from Create's KineticBlockEntityRenderer): white→red while stalled,
        // white→green briefly as it recovers. The head models carry tintindex 0 so renderModel's colour reaches
        // every quad; effect is 0 otherwise, leaving the fist its plain (white) self.
        float effect = fist.getRenderOverstress(partialTick) * TINT_STRENGTH;
        float r = 1.0F, g = 1.0F, b = 1.0F;
        if (effect > 0.0F) { // stalled → red
            g = 1.0F - effect;
            b = 1.0F - effect;
        } else if (effect < 0.0F) { // recovering → green
            float w = -effect;
            r = 1.0F - w;
            b = 1.0F - 0.5F * w;
        }

        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());

        // During a flash, tint the static casing too so the whole machine glows. Drawn a hair larger (1.02×) so
        // the tinted copy sits just in front of the vanilla-drawn casing without z-fighting. Skipped when calm.
        // getBlockModel(state) already carries the blockstate's facing rotation, so no extra rotation is needed.
        if (effect != 0.0F) {
            BakedModel casing = blockRenderer.getBlockModel(state);
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(1.02F, 1.02F, 1.02F);
            pose.translate(-0.5, -0.5, -0.5);
            blockRenderer.getModelRenderer().renderModel(
                    pose.last(), consumer, state, casing, r, g, b, packedLight, packedOverlay);
            pose.popPose();
        }

        // Idle, the fingertip rests just inside the front face (so it clears the depot/belt below); a punch eases it
        // down only as far as the target's surface (getPunchTravel, blocks below the face) and back. The hand model
        // is authored poking FINGERTIP_REST_POKE past the face, so the extension applied to it is the gap between
        // that authored poke and where the fingertip should actually sit this frame.
        float reach = fist.getRenderedReach(partialTick);
        float fingertipDepth = REST_FINGERTIP_DEPTH + reach * (fist.getPunchTravel() - REST_FINGERTIP_DEPTH);
        float extension = fingertipDepth - FINGERTIP_REST_POKE;

        pose.pushPose();

        // Rotate the local +Z punch axis to point along FACING — the same rotation the blockstate gives the
        // casing, so the moving parts stay glued to it. Horizontal facings spin about Y; up/down tip about X.
        pose.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            default -> pose.mulPose(Axis.YP.rotationDegrees(-facingRotation(facing)));
        }
        pose.translate(-0.5, -0.5, -0.5);

        // We are now in the head model's local space, whose punch axis is +Z.
        float scale = (REST_POLE_LENGTH + extension) / REST_POLE_LENGTH;

        // Pole: stretch along +Z, anchored at its fixed (body-side) end.
        pose.pushPose();
        pose.translate(0, 0, POLE_ANCHOR_Z);
        pose.scale(1, 1, scale);
        pose.translate(0, 0, -POLE_ANCHOR_Z);
        blockRenderer.getModelRenderer().renderModel(
                pose.last(), consumer, state, pole, r, g, b, packedLight, packedOverlay);
        pose.popPose();

        // Hand: slide rigidly along +Z by the full extension.
        pose.pushPose();
        pose.translate(0, 0, extension);
        blockRenderer.getModelRenderer().renderModel(
                pose.last(), consumer, state, hand, r, g, b, packedLight, packedOverlay);
        pose.popPose();

        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HydraulicFistBlockEntity fist) {
        // The fist telescopes several blocks out of one face; inflate generously so it is never culled mid-punch.
        double reach = Config.FIST_REACH.get() + 2.0;
        return new AABB(fist.getBlockPos()).inflate(reach);
    }

    private static BakedModel model(ModelResourceLocation rl) {
        return Minecraft.getInstance().getModelManager().getModel(rl);
    }

    private static ModelResourceLocation standalone(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateHydraulics.MODID, path));
    }

    /**
     * The blockstate "y" rotation (degrees) for the Fist's horizontal facing — must mirror
     * blockstates/hydraulic_fist.json. The body model is authored pointing +Z (south), so south=0.
     */
    private static float facingRotation(Direction facing) {
        return switch (facing) {
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F; // SOUTH (and any non-horizontal, handled separately)
        };
    }
}
