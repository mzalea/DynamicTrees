package com.dtteam.dynamictrees.model.nh;

import com.dtteam.dynamictrees.model.nh.QuadMaker.LogAxis;
import com.dtteam.dynamictrees.model.nh.QuadMaker.UvMode;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/** Log geometry in the canonical frame (log along y, block centre 8): chamfered prisms, end caps and ring ends. */
public final class LogShapes {

    private LogShapes() {}

    /** Profile half-width rectangles, square when the chamfer is off. */
    public static int[][] rects(int r, boolean chamfer) {
        return ChamferProfile.rects(r, chamfer);
    }

    /** Sides of a prism of half-width r from y0 to y1, centred on the block. */
    public static void sides(List<BakedQuad> out, int r, boolean chamfer, float y0, float y1, TextureAtlasSprite bark, LogAxis axis) {
        if (y1 <= y0) return;
        for (int[] f : ChamferProfile.sideFaces(rects(r, chamfer))) {
            face(out, f, y0, y1, bark, axis);
        }
    }

    static void face(List<BakedQuad> out, int[] f, float y0, float y1, TextureAtlasSprite sprite, LogAxis axis) {
        float plane = 8 + f[1], a = 8 + f[2], b = 8 + f[3];
        switch (f[0]) {
            case 0 -> QuadMaker.rect(out, Direction.EAST, plane, y0, a, plane, y1, b, sprite, -1, UvMode.WORLD, axis, true);
            case 1 -> QuadMaker.rect(out, Direction.WEST, plane, y0, a, plane, y1, b, sprite, -1, UvMode.WORLD, axis, true);
            case 2 -> QuadMaker.rect(out, Direction.SOUTH, a, y0, plane, b, y1, plane, sprite, -1, UvMode.WORLD, axis, true);
            default -> QuadMaker.rect(out, Direction.NORTH, a, y0, plane, b, y1, plane, sprite, -1, UvMode.WORLD, axis, true);
        }
    }

    /** End cap of a prism at height y, facing up or down. */
    public static void cap(List<BakedQuad> out, int r, boolean chamfer, float y, boolean up, TextureAtlasSprite sprite, UvMode mode, LogAxis axis) {
        for (int[] c : ChamferProfile.capRects(rects(r, chamfer))) {
            QuadMaker.rect(out, up ? Direction.UP : Direction.DOWN, 8 + c[0], y, 8 + c[1], 8 + c[2], y, 8 + c[3], sprite, -1, mode, axis, true);
        }
    }

    /** A box in the canonical frame (pixels), every face but the ones listed in {@code skip}. */
    public static void box(List<BakedQuad> out, float x0, float y0, float z0, float x1, float y1, float z1, TextureAtlasSprite sprite,
                           LogAxis axis, Direction... skip) {
        List<Direction> skipped = List.of(skip);
        for (Direction d : Direction.values()) {
            if (skipped.contains(d)) continue;
            switch (d) {
                case UP -> QuadMaker.rect(out, d, x0, y1, z0, x1, y1, z1, sprite, -1, UvMode.WORLD, axis, true);
                case DOWN -> QuadMaker.rect(out, d, x0, y0, z0, x1, y0, z1, sprite, -1, UvMode.WORLD, axis, true);
                case NORTH -> QuadMaker.rect(out, d, x0, y0, z0, x1, y1, z0, sprite, -1, UvMode.WORLD, axis, true);
                case SOUTH -> QuadMaker.rect(out, d, x0, y0, z1, x1, y1, z1, sprite, -1, UvMode.WORLD, axis, true);
                case WEST -> QuadMaker.rect(out, d, x0, y0, z0, x0, y1, z1, sprite, -1, UvMode.WORLD, axis, true);
                case EAST -> QuadMaker.rect(out, d, x1, y0, z0, x1, y1, z1, sprite, -1, UvMode.WORLD, axis, true);
            }
        }
    }

    public static List<BakedQuad> list() {
        return new ArrayList<>(8);
    }
}
