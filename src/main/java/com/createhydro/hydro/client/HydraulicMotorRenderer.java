package com.createhydro.hydro.client;

import com.createhydro.hydro.block.entity.HydraulicMotorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Renders the Hydraulic Motor's rotating shaft.
 *
 * <p>The motor body is a static block model drawn by the normal blockstate pipeline; this renderer adds
 * only the spinning shaft. It reuses Create's {@code SHAFT_HALF} partial model and the same rotation math
 * the game's own kinetic blocks use ({@link KineticBlockEntityRenderer#renderRotatingBuffer}), so the
 * shaft turns at the motor's actual speed, flashes red/green under overstress, and lines up seamlessly
 * with adjacent Create shafts. {@code partialFacing} orients the shaft to the motor's {@code FACING}.</p>
 *
 * <p>Unlike {@link KineticBlockEntityRenderer} this renderer is <b>not</b> gated on Flywheel
 * visualization: the motor registers no Flywheel visual of its own, so drawing the shaft here is what
 * makes it appear whether or not the Flywheel backend is active &ndash; the BER route the design asked
 * for.</p>
 */
public class HydraulicMotorRenderer implements BlockEntityRenderer<HydraulicMotorBlockEntity> {

    public HydraulicMotorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HydraulicMotorBlockEntity be, float partialTick, PoseStack ms,
                       MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state);
        KineticBlockEntityRenderer.renderRotatingBuffer(be, shaft, ms, buffer.getBuffer(RenderType.solid()), light);
    }

    @Override
    public AABB getRenderBoundingBox(HydraulicMotorBlockEntity be) {
        // The shaft pokes a full block out of the front; inflate so it is never culled mid-rotation.
        return new AABB(be.getBlockPos()).inflate(1.0);
    }
}
