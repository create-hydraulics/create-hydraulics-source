package com.createhydro.hydro.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared helpers for building fitted block selection/collision shapes.
 *
 * <p>The mod's machines are custom (non-cube) Blockbench models, so without a shape override they would use the
 * default full-block outline. These helpers let each block declare a body {@link #box(double, double, double,
 * double, double, double) box} in the model's own 0&ndash;16 pixel space and rotate it to the placed orientation
 * with the <b>same</b> rotations the blockstate applies, so the selection box always hugs the model.</p>
 *
 * <ul>
 *   <li>{@link #rotateY(VoxelShape, int)} &ndash; clockwise about Y, matching a blockstate {@code "y"} value
 *       ({@code 90/180/270} → {@code times = 1/2/3}).</li>
 *   <li>{@link #rotateX(VoxelShape, int)} &ndash; about X, matching a blockstate {@code "x"} value.</li>
 * </ul>
 */
public final class ModShapes {

    private ModShapes() {}

    /** A box given in the model's 0&ndash;16 pixel coordinates (converted to the 0&ndash;1 block space). */
    public static VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return Shapes.box(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
    }

    /** Rotate {@code shape} by {@code times}×90° clockwise about the Y axis (mirrors a blockstate {@code "y"}). */
    public static VoxelShape rotateY(VoxelShape shape, int times) {
        VoxelShape result = shape;
        for (int i = 0, n = Math.floorMod(times, 4); i < n; i++) {
            result = rotateY90(result);
        }
        return result;
    }

    /** Rotate {@code shape} by {@code times}×90° about the X axis (mirrors a blockstate {@code "x"}). */
    public static VoxelShape rotateX(VoxelShape shape, int times) {
        VoxelShape result = shape;
        for (int i = 0, n = Math.floorMod(times, 4); i < n; i++) {
            result = rotateX90(result);
        }
        return result;
    }

    // (x, z) -> (1 - z, x): one clockwise quarter-turn about Y, matching blockstate "y": 90.
    private static VoxelShape rotateY90(VoxelShape shape) {
        List<VoxelShape> parts = new ArrayList<>();
        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                parts.add(Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1)));
        return or(parts);
    }

    // (y, z) -> (z, 1 - y): one quarter-turn about X, matching blockstate "x": 90.
    private static VoxelShape rotateX90(VoxelShape shape) {
        List<VoxelShape> parts = new ArrayList<>();
        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                parts.add(Shapes.box(x0, z0, 1 - y1, x1, z1, 1 - y0)));
        return or(parts);
    }

    private static VoxelShape or(List<VoxelShape> parts) {
        VoxelShape result = Shapes.empty();
        for (VoxelShape part : parts) {
            result = Shapes.or(result, part);
        }
        return result;
    }
}
