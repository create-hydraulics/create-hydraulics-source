package com.createhydro.hydro.client;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.HydraulicDrillBlock;
import com.createhydro.hydro.block.entity.HydraulicDrillBlockEntity;
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
 * Renders the Hydraulic Drill's reciprocating breaker head.
 *
 * <p>The casing is static and drawn by the normal block model via the blockstate &ndash; this renderer never
 * touches it except to tint it during an overstress flash. It draws only the <b>breaker head</b> (a standalone
 * model, see {@link #HEAD}) and slides it out of the front face each frame by the block entity's stroke progress,
 * exactly the way the Hydraulic Press/Fist offset their moving parts.</p>
 *
 * <p>The head is authored once, pointing along the casing's local <b>-Z</b> drill axis (the same north-facing
 * orientation as the casing export). For every facing this renderer applies the same rotation the blockstate gives
 * the casing &ndash; spinning about Y for the four horizontal facings and tipping about X for
 * {@link Direction#UP up}/{@link Direction#DOWN down} &ndash; so head and casing stay glued together for all six
 * orientations. Two texture variants (empty vs fluid-loaded, selected by {@link HydraulicDrillBlock#FULL}) keep the
 * head matching the body's look.</p>
 */
public class HydraulicDrillRenderer implements BlockEntityRenderer<HydraulicDrillBlockEntity> {

    /** The standalone breaker-head model (empty texture), registered via {@code ModelEvent.RegisterAdditional}. */
    public static final ModelResourceLocation HEAD = standalone("block/hydraulic_drill_head");
    /** The standalone breaker-head model with the "full" (fluid-loaded) texture. */
    public static final ModelResourceLocation HEAD_FULL = standalone("block/hydraulic_drill_head_full");

    /**
     * How far (in blocks) the head slides out at full extension &ndash; 4px. The bit rests ~2.5px inside the front
     * face, so a full stroke drives the tip ~1.5px into the block being broken before snapping back.
     */
    public static final float HEAD_TRAVEL = 4.0F / 16.0F;

    /** How far the overstress flash mixes the drill toward pure red/green at its peak (held below 1 for a tint). */
    private static final float TINT_STRENGTH = 0.75F;

    private final BlockRenderDispatcher blockRenderer;

    public HydraulicDrillRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(HydraulicDrillBlockEntity drill, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = drill.getBlockState();
        Direction facing = state.getValue(HydraulicDrillBlock.FACING);
        boolean full = state.hasProperty(HydraulicDrillBlock.FULL) && state.getValue(HydraulicDrillBlock.FULL);

        BakedModel head = model(full ? HEAD_FULL : HEAD);
        if (head == null) {
            return; // model not loaded — draw nothing rather than crash
        }

        // Overstress flash (ported from Create's KineticBlockEntityRenderer): white→red while stalled,
        // white→green briefly as it recovers. The head/casing faces carry tintindex 0 so renderModel's colour
        // reaches every quad; effect is 0 otherwise, leaving the drill its plain (white) self.
        float effect = drill.getRenderOverstress(partialTick) * TINT_STRENGTH;
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

        // During a flash, tint the static casing too so the whole machine glows. Drawn a hair larger (1.02×) so the
        // tinted copy sits just in front of the vanilla-drawn casing without z-fighting. Skipped when calm.
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

        float extension = drill.getRenderedExtension(partialTick) * HEAD_TRAVEL;

        pose.pushPose();

        // Rotate the local -Z drill axis to point along FACING — the same rotation the blockstate gives the casing,
        // so the head stays glued to it. Horizontal facings spin about Y; up/down tip about X.
        pose.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            default -> pose.mulPose(Axis.YP.rotationDegrees(-facingRotation(facing)));
        }
        pose.translate(-0.5, -0.5, -0.5);

        // We are now in the head model's local space, whose drill axis is -Z. Slide the head out along -Z.
        pose.translate(0, 0, -extension);
        blockRenderer.getModelRenderer().renderModel(
                pose.last(), consumer, state, head, r, g, b, packedLight, packedOverlay);

        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HydraulicDrillBlockEntity drill) {
        // The head pokes a little out of one face while drilling; inflate a block so it is never culled mid-stroke.
        return new AABB(drill.getBlockPos()).inflate(1.0);
    }

    private static BakedModel model(ModelResourceLocation rl) {
        return Minecraft.getInstance().getModelManager().getModel(rl);
    }

    private static ModelResourceLocation standalone(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateHydraulics.MODID, path));
    }

    /**
     * The blockstate "y" rotation (degrees) for the Drill's horizontal facing — must mirror
     * blockstates/hydraulic_drill.json. The casing model is authored pointing -Z (north), so north=0.
     */
    private static float facingRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F; // NORTH (and any non-horizontal, handled separately)
        };
    }
}
