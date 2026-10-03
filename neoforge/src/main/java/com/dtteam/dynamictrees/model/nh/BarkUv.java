package com.dtteam.dynamictrees.model.nh;

import net.minecraft.core.Direction;

/**
 * The bark rule: texture coordinates come from where a vertex sits, exactly as vanilla lays out a face whose
 * element has no explicit uv. One texel is one pixel everywhere, so bark runs on unbroken across block seams, taper
 * steps and chamfer faces, and each texel row lines up all the way up a trunk.
 */
public final class BarkUv {

    private BarkUv() {}

    /** u, v in pixels (0..16) for a vertex at x, y, z pixels inside its 16-pixel cell, on a face looking {@code face}. */
    public static float[] uv(Direction face, float x, float y, float z) {
        return switch (face) {
            case DOWN -> new float[]{x, 16f - z};
            case UP -> new float[]{x, z};
            case NORTH -> new float[]{16f - x, 16f - y};
            case SOUTH -> new float[]{x, 16f - y};
            case WEST -> new float[]{z, 16f - y};
            case EAST -> new float[]{16f - z, 16f - y};
        };
    }
}
