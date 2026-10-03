package com.dtteam.dynamictrees.model.nh;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChamferProfileTest {

    @Test
    void thinTwigsStaySquare() {
        for (int r = 1; r <= 2; r++) {
            assertEquals(0, ChamferProfile.cut(r));
            assertEquals(4 * r * r, ChamferProfile.area(r));
        }
    }

    @Test
    void cutCornersButKeepFullWidthAcrossTheMiddle() {
        for (int r = 3; r <= 24; r++) {
            int[][] rs = ChamferProfile.rects(r);
            assertEquals(r, rs[0][0], "widest rect spans the full width in x, r=" + r);
            assertEquals(r, rs[rs.length - 1][1], "deepest rect spans the full width in z, r=" + r);
            assertTrue(ChamferProfile.area(r) < 4 * r * r, "corners are cut, r=" + r);
            for (int i = 1; i < rs.length; i++) {
                assertTrue(rs[i][0] <= rs[i - 1][0] && rs[i][1] >= rs[i - 1][1], "a staircase, r=" + r);
            }
        }
    }

    @Test
    void symmetricAcrossBothAxes() {
        for (int r = 3; r <= 24; r++) {
            int[][] rs = ChamferProfile.rects(r);
            for (int i = 0; i < rs.length; i++) {
                int[] mirror = rs[rs.length - 1 - i];
                assertArrayEquals(new int[]{rs[i][1], rs[i][0]}, mirror, "x and z swap onto each other, r=" + r);
            }
        }
    }

    @Test
    void areaNeverShrinksAsTheLogGrows() {
        for (int r = 2; r <= 24; r++) {
            assertTrue(ChamferProfile.area(r) > ChamferProfile.area(r - 1), "r=" + r);
        }
    }

    @Test
    void capPiecesTileTheSectionWithoutOverlap() {
        for (int r = 1; r <= 24; r++) {
            List<int[]> caps = ChamferProfile.capRects(r);
            boolean[][] seen = new boolean[2 * r][2 * r];
            for (int[] c : caps) {
                for (int x = c[0]; x < c[2]; x++)
                    for (int z = c[1]; z < c[3]; z++) {
                        assertFalse(seen[x + r][z + r], "overlap at " + x + "," + z + " r=" + r);
                        seen[x + r][z + r] = true;
                    }
            }
            int filled = 0;
            for (boolean[] row : seen) for (boolean b : row) filled += b ? 1 : 0;
            assertEquals(ChamferProfile.area(r), filled);
        }
    }

    @Test
    void sideFacesTraceTheOutlineExactly() {
        // every unit edge between a filled pixel and an empty one is covered by exactly one side face
        for (int r = 1; r <= 24; r++) {
            boolean[][] in = new boolean[2 * r + 2][2 * r + 2];
            for (int[] c : ChamferProfile.capRects(r))
                for (int x = c[0]; x < c[2]; x++)
                    for (int z = c[1]; z < c[3]; z++) in[x + r + 1][z + r + 1] = true;
            int edges = 0;
            for (int x = 0; x < in.length; x++)
                for (int z = 0; z < in.length; z++) {
                    if (!in[x][z]) continue;
                    if (x + 1 < in.length && !in[x + 1][z]) edges++;
                    if (!in[x - 1][z]) edges++;
                    if (z + 1 < in.length && !in[x][z + 1]) edges++;
                    if (!in[x][z - 1]) edges++;
                }
            int covered = 0;
            for (int[] f : ChamferProfile.sideFaces(r)) {
                assertTrue(f[3] > f[2], "faces have length, r=" + r);
                for (int a = f[2]; a < f[3]; a++) {
                    int inside, outside;
                    if (f[0] < 2) {
                        int x = f[0] == 0 ? f[1] - 1 : f[1];
                        int ox = f[0] == 0 ? f[1] : f[1] - 1;
                        assertTrue(in[x + r + 1][a + r + 1] && !in[ox + r + 1][a + r + 1], "x face on the boundary, r=" + r);
                    } else {
                        int z = f[0] == 2 ? f[1] - 1 : f[1];
                        int oz = f[0] == 2 ? f[1] : f[1] - 1;
                        assertTrue(in[a + r + 1][z + r + 1] && !in[a + r + 1][oz + r + 1], "z face on the boundary, r=" + r);
                    }
                    covered++;
                }
            }
            assertEquals(edges, covered, "r=" + r);
        }
    }
}
