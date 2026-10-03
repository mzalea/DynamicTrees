package com.dtteam.dynamictrees.model.nh;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BarkUvTest {

    @Test
    void oneTexelPerPixelOnEveryFace() {
        for (Direction d : Direction.values()) {
            float[] a = BarkUv.uv(d, 3, 4, 5);
            float[] bx = BarkUv.uv(d, 4, 4, 5), by = BarkUv.uv(d, 3, 5, 5), bz = BarkUv.uv(d, 3, 4, 6);
            // moving one pixel along any in-plane axis moves exactly one texel in u or v
            for (float[] b : new float[][]{bx, by, bz}) {
                float du = Math.abs(b[0] - a[0]), dv = Math.abs(b[1] - a[1]);
                assertTrue(du + dv == 0 || du + dv == 1, d + " moves " + du + "," + dv);
            }
        }
    }

    @Test
    void barkRunsOnAcrossABlockSeamUpATrunk() {
        // the top of one block (y = 16) and the bottom of the next (y = 0) meet on adjacent texel rows: v 0 and v 16
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            assertEquals(0f, BarkUv.uv(d, 4, 16, 4)[1]);
            assertEquals(16f, BarkUv.uv(d, 4, 0, 4)[1]);
        }
    }

    @Test
    void sameColumnWhateverTheRadius() {
        // a taper step only moves the face in or out; the texel column at a given position does not change
        assertEquals(BarkUv.uv(Direction.EAST, 12, 5, 7)[0], BarkUv.uv(Direction.EAST, 11, 5, 7)[0]);
        assertEquals(BarkUv.uv(Direction.SOUTH, 6, 5, 12)[0], BarkUv.uv(Direction.SOUTH, 6, 5, 11)[0]);
    }
}
