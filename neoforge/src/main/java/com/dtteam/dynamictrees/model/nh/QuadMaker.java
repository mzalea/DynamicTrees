package com.dtteam.dynamictrees.model.nh;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

import java.util.List;

/**
 * Bakes axis-aligned rectangles into quads. Geometry is described in a canonical frame where the log runs along y
 * (pixels, block centre at 8), then turned onto the real axis; textures are laid on by {@link BarkUv}, so bark grain
 * always follows the log and every texel is one pixel.
 */
public final class QuadMaker {

    /** The log's real axis: Y (upright), Z (north-south) or X (west-east). Canonical +y maps to up, south, east. */
    public enum LogAxis { Y, Z, X }

    /** How texture coordinates are found from a vertex position. */
    public enum UvMode {
        /** {@link BarkUv}: vanilla's position rule, wrapped every 16 pixels (faces are split on block lines). */
        WORLD,
        /** The 3x3-block thick ring sprite: the whole 48-pixel span maps onto the sprite. */
        THICK_RINGS
    }

    private QuadMaker() {}

    /**
     * Adds the rectangle facing {@code face} (canonical) with the given canonical bounds, one bound flat along the
     * face's axis. With {@link UvMode#WORLD} it is split on every 16-pixel line so each piece maps inside the sprite.
     */
    public static void rect(List<BakedQuad> out, Direction face, float x0, float y0, float z0, float x1, float y1, float z1,
                            TextureAtlasSprite sprite, int tint, UvMode mode, LogAxis axis, boolean shade) {
        if (mode == UvMode.WORLD) {
            float[] xs = cuts(x0, x1), ys = cuts(y0, y1), zs = cuts(z0, z1);
            boolean fx = face.getAxis() == Direction.Axis.X, fy = face.getAxis() == Direction.Axis.Y, fz = face.getAxis() == Direction.Axis.Z;
            int nx = fx ? 1 : xs.length - 1, ny = fy ? 1 : ys.length - 1, nz = fz ? 1 : zs.length - 1;
            for (int i = 0; i < nx; i++)
                for (int j = 0; j < ny; j++)
                    for (int k = 0; k < nz; k++) {
                        float ax = fx ? x0 : xs[i], bx = fx ? x1 : xs[i + 1];
                        float ay = fy ? y0 : ys[j], by = fy ? y1 : ys[j + 1];
                        float az = fz ? z0 : zs[k], bz = fz ? z1 : zs[k + 1];
                        if (ax == bx && !fx || ay == by && !fy || az == bz && !fz) continue;
                        out.add(bake(face, ax, ay, az, bx, by, bz, sprite, tint, mode, axis, shade));
                    }
        } else {
            out.add(bake(face, x0, y0, z0, x1, y1, z1, sprite, tint, mode, axis, shade));
        }
    }

    /**
     * Both sides of a thin plane (fringe leaves): the face and its reverse. Unshaded, so a lip seen edge-on or from
     * below does not draw as a dark seam along the crown.
     */
    public static void plane(List<BakedQuad> out, Direction face, float x0, float y0, float z0, float x1, float y1, float z1,
                             TextureAtlasSprite sprite, int tint) {
        rect(out, face, x0, y0, z0, x1, y1, z1, sprite, tint, UvMode.WORLD, LogAxis.Y, false);
        rect(out, face.getOpposite(), x0, y0, z0, x1, y1, z1, sprite, tint, UvMode.WORLD, LogAxis.Y, false);
    }

    private static float[] cuts(float a, float b) {
        float lo = Math.min(a, b), hi = Math.max(a, b);
        int first = (int) Math.floor(lo / 16f) + 1, last = (int) Math.ceil(hi / 16f) - 1;
        int n = Math.max(0, last - first + 1);
        float[] out = new float[n + 2];
        out[0] = lo;
        for (int i = 0; i < n; i++) out[i + 1] = (first + i) * 16f;
        out[n + 1] = hi;
        return out;
    }

    private static BakedQuad bake(Direction face, float x0, float y0, float z0, float x1, float y1, float z1,
                                  TextureAtlasSprite sprite, int tint, UvMode mode, LogAxis axis, boolean shade) {
        float[][] v = corners(face, x0, y0, z0, x1, y1, z1);
        // the cell this piece sits in, so the position rule wraps inside one sprite
        float cx = (float) Math.floor(Math.min(x0, x1) / 16f + 1e-4f) * 16f;
        float cy = (float) Math.floor(Math.min(y0, y1) / 16f + 1e-4f) * 16f;
        float cz = (float) Math.floor(Math.min(z0, z1) / 16f + 1e-4f) * 16f;
        Direction real = turn(face, axis);
        QuadBakingVertexConsumer b = new QuadBakingVertexConsumer();
        b.setSprite(sprite);
        b.setDirection(real);
        b.setTintIndex(tint);
        b.setShade(shade);
        b.setHasAmbientOcclusion(true);
        for (float[] p : v) {
            float u, w;
            if (mode == UvMode.THICK_RINGS) {
                u = (p[0] + 16f) / 48f * 16f;
                w = face == Direction.DOWN ? 16f - (p[2] + 16f) / 48f * 16f : (p[2] + 16f) / 48f * 16f;
            } else {
                float[] uv = BarkUv.uv(face, p[0] - cx, p[1] - cy, p[2] - cz);
                u = uv[0];
                w = uv[1];
            }
            float[] q = turn(p, axis);
            b.addVertex(q[0] / 16f, q[1] / 16f, q[2] / 16f);
            b.setColor(255, 255, 255, 255);
            b.setUv(sprite.getU(clamp(u) / 16f), sprite.getV(clamp(w) / 16f));
            b.setUv2(0, 0);
            b.setNormal(real.getStepX(), real.getStepY(), real.getStepZ());
        }
        return b.bakeQuad();
    }

    private static float clamp(float f) {
        return Math.max(0f, Math.min(16f, f));
    }

    /** Corners in vanilla's order for each face (counter-clockwise seen from outside). */
    static float[][] corners(Direction face, float x0, float y0, float z0, float x1, float y1, float z1) {
        float ax = Math.min(x0, x1), bx = Math.max(x0, x1), ay = Math.min(y0, y1), by = Math.max(y0, y1), az = Math.min(z0, z1), bz = Math.max(z0, z1);
        return switch (face) {
            case UP -> new float[][]{{ax, by, az}, {ax, by, bz}, {bx, by, bz}, {bx, by, az}};
            case DOWN -> new float[][]{{ax, ay, bz}, {ax, ay, az}, {bx, ay, az}, {bx, ay, bz}};
            case NORTH -> new float[][]{{bx, by, az}, {bx, ay, az}, {ax, ay, az}, {ax, by, az}};
            case SOUTH -> new float[][]{{ax, by, bz}, {ax, ay, bz}, {bx, ay, bz}, {bx, by, bz}};
            case WEST -> new float[][]{{ax, by, az}, {ax, ay, az}, {ax, ay, bz}, {ax, by, bz}};
            case EAST -> new float[][]{{bx, by, bz}, {bx, ay, bz}, {bx, ay, az}, {bx, by, az}};
        };
    }

    /** Canonical position (pixels) to the real one: a quarter turn about the block centre for Z and X logs. */
    static float[] turn(float[] p, LogAxis axis) {
        float cx = p[0] - 8f, cy = p[1] - 8f, cz = p[2] - 8f;
        return switch (axis) {
            case Y -> new float[]{p[0], p[1], p[2]};
            case Z -> new float[]{cx + 8f, -cz + 8f, cy + 8f};
            case X -> new float[]{cy + 8f, -cx + 8f, cz + 8f};
        };
    }

    public static Direction turn(Direction d, LogAxis axis) {
        if (axis == LogAxis.Y) return d;
        float[] q = turn(new float[]{d.getStepX() + 8f, d.getStepY() + 8f, d.getStepZ() + 8f}, axis);
        return Direction.getNearest(q[0] - 8f, q[1] - 8f, q[2] - 8f);
    }
}
