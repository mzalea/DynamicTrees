package com.dtteam.dynamictrees.systems.cell;

import com.dtteam.dynamictrees.api.cell.Cell;
import com.dtteam.dynamictrees.api.cell.CellKit;
import com.dtteam.dynamictrees.api.cell.CellNull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the pad automaton around one twig until it settles, the way leaves solve their hydration in the world. */
class PadCellKitTest {

    private static Map<BlockPos, Integer> settle(CellKit kit) {
        BlockPos twig = BlockPos.ZERO;
        Map<BlockPos, Integer> val = new HashMap<>();
        for (int round = 0; round < 40; round++) {
            Map<BlockPos, Integer> next = new HashMap<>();
            for (int x = -8; x <= 8; x++)
                for (int y = -4; y <= 4; y++)
                    for (int z = -8; z <= 8; z++) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (p.equals(twig)) continue;
                        Cell[] cells = new Cell[6];
                        for (Direction d : Direction.values()) {
                            BlockPos q = p.relative(d);
                            Integer v = val.get(q);
                            cells[d.ordinal()] = q.equals(twig) ? kit.getCellForBranch(1, 0) : v == null ? CellNull.NULL_CELL : kit.getCellForLeaves(v);
                        }
                        int r = kit.getCellSolver().solve(cells);
                        if (r > 0) next.put(p, r);
                    }
            if (next.equals(val)) return val;
            val = next;
        }
        fail("pad automaton did not settle");
        return val;
    }

    @Test
    void padIsWideFlatAndOpenUnderneath() {
        Map<BlockPos, Integer> leaves = settle(CellKits.PAD);
        int minY = leaves.keySet().stream().mapToInt(BlockPos::getY).min().orElseThrow();
        int maxY = leaves.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow();
        int maxX = leaves.keySet().stream().mapToInt(BlockPos::getX).max().orElseThrow();
        assertEquals(0, minY, "nothing grows below the twig");
        assertEquals(1, maxY, "two layers deep");
        assertEquals(3, maxX, "7 across, so crowns keep about their old footprint");
        assertTrue(leaves.size() < 60, "lighter than a deciduous cluster (80), got " + leaves.size());
    }

    @Test
    void worldgenClusterMatchesWhatTheLeavesGrowInto() {
        Map<BlockPos, Integer> leaves = settle(CellKits.PAD);
        BlockPos c = new BlockPos(3, 0, 3);
        int count = 0;
        for (int y = 0; y < 2; y++)
            for (int z = 0; z < 7; z++)
                for (int x = 0; x < 7; x++) {
                    int want = leaves.getOrDefault(new BlockPos(x - c.getX(), y - c.getY(), z - c.getZ()), 0);
                    assertEquals(want, LeafClusters.PAD.getVoxel(x - c.getX(), y - c.getY(), z - c.getZ()), "at " + x + "," + y + "," + z);
                    count += want > 0 ? 1 : 0;
                }
        assertEquals(leaves.size(), count, "cluster covers every leaf");
    }

    @Test
    void padLeavesDecayWithoutTheirTwig() {
        // no rule keeps a hydration level alive on its own, so a pad cut off from its twig rots away
        CellKit kit = CellKits.PAD;
        Cell[] cells = new Cell[6];
        for (int v = 1; v <= 7; v++) {
            for (Direction d : Direction.values()) cells[d.ordinal()] = kit.getCellForLeaves(v);
            assertTrue(kit.getCellSolver().solve(cells) < v, "hydration " + v + " must fall");
        }
    }
}
