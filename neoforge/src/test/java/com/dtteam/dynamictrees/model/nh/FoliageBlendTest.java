package com.dtteam.dynamictrees.model.nh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FoliageBlendTest {

    @Test
    void mixEnds() {
        assertEquals(0x48B518, FoliageBlend.mix(0x48B518, 0x91BD59, 0));
        assertEquals(0x91BD59, FoliageBlend.mix(0x48B518, 0x91BD59, 1));
        assertEquals(0x6D_B9_39, FoliageBlend.mix(0x48B518, 0x91BD59, 0.5));
    }

    @Test
    void scaleClamps() {
        assertEquals(0xFFFFFF, FoliageBlend.scale(0xF0F0F0, 2.0));
        assertEquals(0x000000, FoliageBlend.scale(0x123456, 0));
    }

    @Test
    void deeperLeavesAreDarkerAndFringeLighter() {
        double outer = FoliageBlend.factor(1, false, 0.035, 0.06, 0, 0);
        double inner = FoliageBlend.factor(5, false, 0.035, 0.06, 0, 0);
        double fringe = FoliageBlend.factor(1, true, 0.035, 0.06, 0, 0);
        assertEquals(1.0, outer, 1e-9);
        assertTrue(inner < outer);
        assertTrue(fringe > outer);
    }

    @Test
    void jitterStaysInItsBand() {
        for (int h = 0; h < 1 << 16; h += 97) {
            double f = FoliageBlend.factor(1, false, 0, 0, 0.04, h);
            assertTrue(f >= 0.96 - 1e-9 && f <= 1.04 + 1e-9, "hash " + h);
        }
    }
}
