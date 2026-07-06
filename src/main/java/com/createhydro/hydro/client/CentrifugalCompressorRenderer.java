package com.createhydro.hydro.client;

import com.createhydro.hydro.block.entity.CentrifugalCompressorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Renders the Centrifugal Compressor's rotating shaft.
 *
 * <p>The compressor body is a static block model drawn by the normal blockstate pipeline; this renderer adds
 * only the spinning shaft. The shaft runs through the two black side faces along the block's
 * {@link HorizontalAxisKineticBlock#HORIZONTAL_AXIS}, so a {@code SHAFT_HALF} partial is drawn poking out of each
 * of them. Both halves are turned by the same rotation math the game's own kinetic blocks use
 * ({@link KineticBlockEntityRenderer#renderRotatingBuffer}), which reads the compressor's real speed and axis, so
 * the shaft turns at the compressor's actual RPM, flashes under overstress, and lines up seamlessly with
 * adjacent Create shafts.</p>
 */
public class CentrifugalCompressorRenderer implements BlockEntityRenderer<CentrifugalCompressorBlockEntity> {

    public CentrifugalCompressorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CentrifugalCompressorBlockEntity be, float partialTick, PoseStack ms,
                       MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Axis axis = state.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);

        // A half-shaft out of each face along the axis (e.g. EAST + WEST for the X axis), spun by the BE's speed.
        for (AxisDirection axisDir : AxisDirection.values()) {
            Direction face = Direction.fromAxisAndDirection(axis, axisDir);
            SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, face);
            KineticBlockEntityRenderer.renderRotatingBuffer(be, shaft, ms, buffer.getBuffer(RenderType.solid()), light);
        }
    }

    @Override
    public AABB getRenderBoundingBox(CentrifugalCompressorBlockEntity be) {
        // Shafts poke a full block out of both side faces; inflate so they are never culled mid-rotation.
        return new AABB(be.getBlockPos()).inflate(1.0);
    }
}
