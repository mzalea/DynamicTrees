package com.dtteam.dynamictrees.model.nh;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-section of a branch or trunk: a square of half-width {@code r} pixels with its corners cut back in pixel
 * steps, so a log reads as round while every edge stays on the 1/16 grid and every face stays axis-aligned (vanilla
 * texels stay square). The section is the union of {@link #rects} — rectangles centred on the axis, widest in x first.
 * Pure arithmetic in pixels, centred on 0.
 */
public final class ChamferProfile {

    private ChamferProfile() {}

    /** How far the corner is cut back, in pixels, along each side. Thin twigs stay square. */
    public static int cut(int r) {
        if (r < 3) return 0;
        return Math.min(8, Math.max(1, Math.round(r * 0.3f)));
    }

    /** Number of pixel steps the cut is made in; more steps on wider logs read rounder. */
    public static int steps(int r) {
        int c = cut(r);
        if (c == 0) return 0;
        return Math.min(c, r >= 9 ? 3 : (c >= 2 ? 2 : 1));
    }

    /** Half-extents {hx, hz} of the rectangles whose union is the section; hx falls and hz rises along the list. */
    public static int[][] rects(int r) {
        int c = cut(r), k = steps(r);
        if (k == 0) return new int[][]{{r, r}};
        int[][] out = new int[k + 1][];
        for (int i = 0; i <= k; i++) {
            int d = Math.round(c * i / (float) k);
            out[i] = new int[]{r - d, r - (c - d)};
        }
        return out;
    }

    /** The section split into rectangles that do not overlap, as {x0, z0, x1, z1}: caps and ring ends use these. */
    public static List<int[]> capRects(int r) {
        return capRects(rects(r));
    }

    /** Square section (no chamfer) as rects, for when the chamfer is switched off. */
    public static int[][] rects(int r, boolean chamfer) {
        return chamfer ? rects(r) : new int[][]{{r, r}};
    }

    public static List<int[]> capRects(int[][] rs) {
        int k = rs.length - 1;
        List<int[]> out = new ArrayList<>();
        out.add(new int[]{-rs[k][0], -rs[k][1], rs[k][0], rs[k][1]});
        for (int i = k - 1; i >= 0; i--) {
            int inner = rs[i + 1][0], outer = rs[i][0], hz = rs[i][1];
            if (outer == inner) continue;
            out.add(new int[]{inner, -hz, outer, hz});
            out.add(new int[]{-outer, -hz, -inner, hz});
        }
        return out;
    }

    /**
     * The outline as side faces {side, plane, from, to}: side 0 faces +x, 1 faces -x, 2 faces +z, 3 faces -z; plane
     * is the face's x (or z), and from..to its extent along the other horizontal axis.
     */
    public static List<int[]> sideFaces(int r) {
        return sideFaces(rects(r));
    }

    public static List<int[]> sideFaces(int[][] rs) {
        int k = rs.length - 1;
        List<int[]> out = new ArrayList<>();
        // faces looking along x: the widest rect's flat face, then one step face per corner per step
        out.add(new int[]{0, rs[0][0], -rs[0][1], rs[0][1]});
        out.add(new int[]{1, -rs[0][0], -rs[0][1], rs[0][1]});
        for (int i = 1; i <= k; i++) {
            if (rs[i][1] == rs[i - 1][1]) continue;
            for (int s = 0; s < 2; s++) {
                int plane = s == 0 ? rs[i][0] : -rs[i][0];
                out.add(new int[]{s, plane, rs[i - 1][1], rs[i][1]});
                out.add(new int[]{s, plane, -rs[i][1], -rs[i - 1][1]});
            }
        }
        // faces looking along z: the deepest rect's flat face, then the steps
        out.add(new int[]{2, rs[k][1], -rs[k][0], rs[k][0]});
        out.add(new int[]{3, -rs[k][1], -rs[k][0], rs[k][0]});
        for (int i = k - 1; i >= 0; i--) {
            if (rs[i][0] == rs[i + 1][0]) continue;
            for (int s = 2; s < 4; s++) {
                int plane = s == 2 ? rs[i][1] : -rs[i][1];
                out.add(new int[]{s, plane, rs[i + 1][0], rs[i][0]});
                out.add(new int[]{s, plane, -rs[i][0], -rs[i + 1][0]});
            }
        }
        return out;
    }

    /** Area of the section in square pixels. */
    public static int area(int r) {
        int a = 0;
        for (int[] c : capRects(r)) a += (c[2] - c[0]) * (c[3] - c[1]);
        return a;
    }
}
