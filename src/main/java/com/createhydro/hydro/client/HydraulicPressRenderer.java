package com.createhydro.hydro.client;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.HydraulicPressBlock;
import com.createhydro.hydro.block.entity.HydraulicPressBlockEntity;
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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Renders the Hydraulic Press's moving head.
 *
 * <p>The casing is static and drawn by the normal block model via the blockstate &ndash; this renderer
 * never touches it. It draws only the <b>press head</b> (a standalone model registered as an additional
 * model, see {@link #HEAD_MODEL}) and slides it down the Y axis each frame by the block entity's stroke
 * progress, exactly the way Create's {@code MechanicalPressRenderer} offsets its head:
 * {@code translate(0, -offset, 0)}. At progress {@code 0} the head sits in its modelled rest position;
 * at progress {@code 1} it has slid {@link #HEAD_TRAVEL} of a block down toward the depot below. The
 * progress is interpolated across the partial tick by the block entity, so the motion is smooth and
 * never snaps.</p>
 *
 * <p>Two head models exist so the yellow hazard stripes follow the body's empty/full texture state:
 * {@link #HEAD_MODEL} (no fluid) and {@link #HEAD_MODEL_FULL} (fluid loaded), selected by the
 * {@link HydraulicPressBlock#FULL} blockstate.</p>
 */
public class HydraulicPressRenderer implements BlockEntityRenderer<HydraulicPressBlockEntity> {

    /** The standalone head model (empty texture), registered via {@code ModelEvent.RegisterAdditional}. */
    public static final ModelResourceLocation HEAD_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(CreateHydraulics.MODID, "block/hydraulic_press_head"));
    /** The standalone head model with the "full" (fluid-loaded) texture. */
    public static final ModelResourceLocation HEAD_MODEL_FULL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(CreateHydraulics.MODID, "block/hydraulic_press_head_full"));

    /**
     * How far (in blocks) the head slides down at full stroke. The head's foot rests at the bottom of the
     * press block (y=0); the Depot one block below is 13px tall, so its surface sits 3px (0.1875 block)
     * under this block. We dip a touch less than that so the foot stops <em>just above</em> the depot's
     * surface and the item on it &mdash; pressing them without the head clipping down into the depot.
     */
    private static final float HEAD_TRAVEL = 0.16F;

    /**
     * How far the overstress flash mixes the press toward pure red/green at its peak (0 = none, 1 = fully
     * saturated like Create). Held below 1 so the flash reads as a tint rather than an oversaturated block.
     */
    private static final float TINT_STRENGTH = 0.75F;

    private final BlockRenderDispatcher blockRenderer;

    public HydraulicPressRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(HydraulicPressBlockEntity press, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = press.getBlockState();
        boolean full = state.hasProperty(HydraulicPressBlock.FULL) && state.getValue(HydraulicPressBlock.FULL);

        BakedModel head = Minecraft.getInstance().getModelManager().getModel(full ? HEAD_MODEL_FULL : HEAD_MODEL);
        if (head == null) {
            return; // model not loaded — draw nothing rather than crash
        }

        // Overstress flash (ported from Create's KineticBlockEntityRenderer): mix white→red while the press is
        // stalled and white→green briefly as it recovers. The model faces carry tintindex 0 so renderModel's
        // colour reaches every quad; effect is 0 the rest of the time, leaving a plain white (untinted) press.
        float effect = press.getRenderOverstress(partialTick);
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

        // During a flash, tint the static casing too so the whole machine glows, not just the head. The casing
        // is normally drawn white by the vanilla block pipeline; we redraw it a hair larger (1.02×) so the
        // tinted copy sits just in front without z-fighting. Skipped entirely when there is no flash.
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

        float headOffset = press.getRenderedHeadOffset(partialTick) * HEAD_TRAVEL;

        pose.pushPose();

        // 1) Match the blockstate's FACING rotation so the head stays aligned with the (rotated) casing.
        //    Blockstate "y":R corresponds to a -R rotation about +Y in pose space (same values as the model).
        float facingYRot = facingRotation(state);
        if (facingYRot != 0.0F) {
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-facingYRot));
            pose.translate(-0.5, -0.5, -0.5);
        }

        // 2) Slide the head straight down by the stroke offset (Y is unaffected by the Y rotation above).
        pose.translate(0, -headOffset, 0);

        blockRenderer.getModelRenderer().renderModel(
                pose.last(), consumer, state, head, r, g, b, packedLight, packedOverlay);

        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HydraulicPressBlockEntity press) {
        // The head sits slightly above the casing at rest and dips down into the depot while pressing —
        // expand the bounds a block in each vertical direction so it is never culled mid-stroke.
        BlockPos pos = press.getBlockPos();
        return new AABB(pos).expandTowards(0, -1, 0).expandTowards(0, 1, 0);
    }

    /** The blockstate "y" rotation (degrees) for the press's facing — must match blockstates/hydraulic_press.json. */
    private static float facingRotation(BlockState state) {
        if (!state.hasProperty(HydraulicPressBlock.FACING)) {
            return 0.0F;
        }
        return switch (state.getValue(HydraulicPressBlock.FACING)) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F; // NORTH (and any non-horizontal, which never occurs here)
        };
    }
}
