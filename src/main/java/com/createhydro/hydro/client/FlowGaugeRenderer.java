package com.createhydro.hydro.client;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.FlowGaugeBlock;
import com.createhydro.hydro.block.entity.FlowGaugeBlockEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the Flow Gauge's needle, rotated to reflect the hydraulic pressure in the line.
 *
 * <p>This is the visible half of the indication system. The static part of the gauge (body, dial housing,
 * stubs) is drawn by the normal block model; this renderer draws only the needle &ndash; an additional model,
 * {@link #NEEDLE_MODEL} &ndash; and spins it about the dial's pivot by the angle the block entity computes
 * from pressure ({@link FlowGaugeBlockEntity#getNeedleAngleDegrees()}). We can't visually verify it yet, so
 * the maths is kept simple and the code is defensive (a missing needle model is skipped, never fatal).</p>
 */
public class FlowGaugeRenderer implements BlockEntityRenderer<FlowGaugeBlockEntity> {

    /** The standalone needle model, registered via {@code ModelEvent.RegisterAdditional} and looked up here. */
    public static final ModelResourceLocation NEEDLE_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(CreateHydraulics.MODID, "block/flow_gauge_needle"));

    // Dial pivot in block space (the needle model's rotation origin [2, 7.5, 9.5] in pixels, divided by 16).
    private static final float PIVOT_X = 2.0F / 16.0F;
    private static final float PIVOT_Y = 7.5F / 16.0F;
    private static final float PIVOT_Z = 9.5F / 16.0F;

    private final BlockRenderDispatcher blockRenderer;

    public FlowGaugeRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(FlowGaugeBlockEntity gauge, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BakedModel needle = Minecraft.getInstance().getModelManager().getModel(NEEDLE_MODEL);
        if (needle == null) {
            return; // model not loaded (e.g. registration skipped) — draw nothing rather than crash
        }

        BlockState state = gauge.getBlockState();
        float angleDegrees = gauge.getRenderNeedleAngle();

        pose.pushPose();

        // 1) Match the blockstate's FACING rotation so the needle stays attached to the (rotated) body.
        //    Blockstate "y":R corresponds to a -R rotation about +Y in pose space; we use the same R values.
        float facingYRot = facingRotation(state);
        if (facingYRot != 0.0F) {
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-facingYRot));
            pose.translate(-0.5, -0.5, -0.5);
        }

        // 2) Rotate the needle about the dial axis (X), pivoting around the gauge's hub.
        pose.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        pose.mulPose(Axis.XP.rotationDegrees(angleDegrees));
        pose.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());
        blockRenderer.getModelRenderer().renderModel(
                pose.last(), consumer, state, needle, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);

        pose.popPose();
    }

    /** The blockstate "y" rotation (degrees) for the gauge's facing — must match blockstates/flow_gauge.json. */
    private static float facingRotation(BlockState state) {
        if (!state.hasProperty(FlowGaugeBlock.FACING)) {
            return 0.0F;
        }
        return switch (state.getValue(FlowGaugeBlock.FACING)) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F; // NORTH (and any non-horizontal, which never occurs here)
        };
    }
}
