package com.dtteam.dynamictrees.model.nh;

/** RGB arithmetic for leaf tint: mixing toward the ground's grass, brightness steps and per-block variation. */
public final class FoliageBlend {

    private FoliageBlend() {}

    /** {@code a} mixed toward {@code b} by share t (0 = a, 1 = b), per channel, rounded. */
    public static int mix(int a, int b, double t) {
        int r = (int) Math.round(ch(a, 16) + (ch(b, 16) - ch(a, 16)) * t);
        int g = (int) Math.round(ch(a, 8) + (ch(b, 8) - ch(a, 8)) * t);
        int bl = (int) Math.round(ch(a, 0) + (ch(b, 0) - ch(a, 0)) * t);
        return rgb(r, g, bl);
    }

    /** Every channel times f, clamped to 0..255. */
    public static int scale(int c, double f) {
        return rgb((int) Math.round(ch(c, 16) * f), (int) Math.round(ch(c, 8) * f), (int) Math.round(ch(c, 0) * f));
    }

    /**
     * The brightness factor for a leaf: darker by {@code shade} per hydration step above 1 (deeper in the crown),
     * lighter by {@code lighten} for fringe geometry, and a fixed jitter in -jitter..+jitter from the position hash.
     */
    public static double factor(int hydration, boolean fringe, double shade, double lighten, double jitter, int hash) {
        double f = 1.0 - shade * Math.max(0, hydration - 1);
        if (fringe) f += lighten;
        double j = ((hash >>> 8) & 0xFF) / 255.0 * 2.0 - 1.0;
        return f * (1.0 + jitter * j);
    }

    private static int ch(int c, int shift) {
        return (c >> shift) & 0xFF;
    }

    private static int rgb(int r, int g, int b) {
        return (Math.max(0, Math.min(255, r)) << 16) | (Math.max(0, Math.min(255, g)) << 8) | Math.max(0, Math.min(255, b));
    }
}
