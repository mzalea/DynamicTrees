package com.dtteam.dynamictrees.model.nh;

import com.dtteam.dynamictrees.model.nh.QuadMaker.LogAxis;
import com.dtteam.dynamictrees.model.nh.QuadMaker.UvMode;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The New Haven branch and trunk look: chamfered logs with bark laid on by position, plus render-only buttress roots,
 * dead stubs and moss. The fixed pieces are baked once per model; the per-tree details are baked on first use and kept.
 * Nothing here changes a block, so drops and log counts are untouched.
 */
public final class BranchQuads {

    private static final BakedQuad[] NONE = new BakedQuad[0];
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final TextureAtlasSprite bark, rings, moss;
    @Nullable
    private final TextureAtlasSprite thickRings;

    // [chamfer][axis][radius 1..8]
    private final BakedQuad[][][] coreSides = new BakedQuad[2][3 * 9][];
    // [chamfer][axis * 9 + r][end low/high * 2 + bark/rings]
    private final BakedQuad[][][] coreCaps = new BakedQuad[2][3 * 9 * 4][];
    // [chamfer][dir * 9 * 9 + r * 9 + start]
    private final BakedQuad[][][] sleeves = new BakedQuad[2][6 * 9 * 9][];
    private final BakedQuad[][] twigEnds = new BakedQuad[6][];
    // thick, [chamfer][r 9..24]
    private final BakedQuad[][][] thickSides = new BakedQuad[2][25][];
    private final BakedQuad[][][] thickTopBark = new BakedQuad[2][25][], thickBotBark = new BakedQuad[2][25][];
    private final BakedQuad[][][] thickTopRings = new BakedQuad[2][25][], thickBotRings = new BakedQuad[2][25][];

    private final Map<Long, BakedQuad[]> details = new ConcurrentHashMap<>();

    public BranchQuads(TextureAtlasSprite bark, TextureAtlasSprite rings, TextureAtlasSprite moss, @Nullable TextureAtlasSprite thickRings) {
        this.bark = bark;
        this.rings = rings;
        this.moss = moss;
        this.thickRings = thickRings;
        for (int ch = 0; ch < 2; ch++) {
            boolean c = ch == 1;
            for (LogAxis axis : LogAxis.values()) {
                int a = axis.ordinal();
                for (int r = 1; r <= 8; r++) {
                    List<BakedQuad> l = new ArrayList<>();
                    LogShapes.sides(l, r, c, 8 - r, 8 + r, bark, axis);
                    coreSides[ch][a * 9 + r] = arr(l);
                    for (int end = 0; end < 2; end++) {
                        for (int tex = 0; tex < 2; tex++) {
                            l = new ArrayList<>();
                            LogShapes.cap(l, r, c, end == 0 ? 8 - r : 8 + r, end == 1, tex == 0 ? bark : rings, UvMode.WORLD, axis);
                            coreCaps[ch][(a * 9 + r) * 4 + end * 2 + tex] = arr(l);
                        }
                    }
                }
            }
            for (Direction dir : Direction.values()) {
                LogAxis axis = axisOf(dir.getAxis());
                boolean pos = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                for (int r = 1; r <= 8; r++) {
                    for (int start = 0; start <= 8; start++) {
                        List<BakedQuad> l = new ArrayList<>();
                        if (start < 8) {
                            LogShapes.sides(l, r, c, pos ? 8 + start : 0, pos ? 16 : 8 - start, bark, axis);
                        }
                        sleeves[ch][(dir.ordinal() * 9 + r) * 9 + start] = arr(l);
                    }
                }
            }
            for (int r = 9; r <= 24; r++) {
                List<BakedQuad> l = new ArrayList<>();
                LogShapes.sides(l, r, c, 0, 16, bark, LogAxis.Y);
                thickSides[ch][r] = arr(l);
                l = new ArrayList<>();
                LogShapes.cap(l, r, c, 16, true, bark, UvMode.WORLD, LogAxis.Y);
                thickTopBark[ch][r] = arr(l);
                l = new ArrayList<>();
                LogShapes.cap(l, r, c, 0, false, bark, UvMode.WORLD, LogAxis.Y);
                thickBotBark[ch][r] = arr(l);
                TextureAtlasSprite tr = thickRings != null ? thickRings : rings;
                UvMode mode = thickRings != null ? UvMode.THICK_RINGS : UvMode.WORLD;
                l = new ArrayList<>();
                LogShapes.cap(l, r, c, 16, true, tr, mode, LogAxis.Y);
                thickTopRings[ch][r] = arr(l);
                l = new ArrayList<>();
                LogShapes.cap(l, r, c, 0, false, tr, mode, LogAxis.Y);
                thickBotRings[ch][r] = arr(l);
            }
        }
        for (Direction dir : Direction.values()) {
            List<BakedQuad> l = new ArrayList<>();
            boolean pos = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            LogShapes.cap(l, 1, false, pos ? 16 : 0, pos, bark, UvMode.WORLD, axisOf(dir.getAxis()));
            twigEnds[dir.ordinal()] = arr(l);
        }
    }

    private static BakedQuad[] arr(List<BakedQuad> l) {
        return l.isEmpty() ? NONE : l.toArray(NONE);
    }

    static LogAxis axisOf(Direction.Axis axis) {
        return switch (axis) {
            case Y -> LogAxis.Y;
            case Z -> LogAxis.Z;
            case X -> LogAxis.X;
        };
    }

    private static boolean positive(Direction d) {
        return d.getAxisDirection() == Direction.AxisDirection.POSITIVE;
    }

    /**
     * Quads for a branch of radius 1..8.
     *
     * @param conn        connection radius per side (DUNSWE)
     * @param sourceDir   the side the branch grows from (decides the core's axis), or null
     * @param ringDir     the side that shows rings (a cut or terminating end), or null
     * @param soilDepth   branch blocks down to the rooty soil, -1 if unknown
     */
    public List<BakedQuad> branch(int r, int[] conn, @Nullable Direction sourceDir, @Nullable Direction ringDir, int soilDepth, int hash) {
        boolean c = NhRenderConfig.on(NhRenderConfig.CHAMFER);
        int ch = c ? 1 : 0;
        Direction.Axis coreAxis = sourceDir == null ? Direction.Axis.Y : sourceDir.getAxis();
        LogAxis axis = axisOf(coreAxis);
        int a = axis.ordinal();
        List<BakedQuad> out = new ArrayList<>(32);
        boolean upright = coreAxis == Direction.Axis.Y;
        int depth = upright ? soilDepth : -1;

        if (r == 8 && upright && depth >= 0 && depth <= 2 && NhRenderConfig.on(NhRenderConfig.MOSS)) {
            add(out, mossSides(8, c, 8 - 8, 8 + 8, depth, hash));
        } else {
            add(out, coreSides[ch][a * 9 + r]);
        }
        Direction low = Direction.fromAxisAndDirection(coreAxis, Direction.AxisDirection.NEGATIVE);
        Direction high = low.getOpposite();
        for (int end = 0; end < 2; end++) {
            Direction d = end == 0 ? low : high;
            if (conn[d.ordinal()] != r) {
                add(out, coreCaps[ch][(a * 9 + r) * 4 + end * 2 + (d == ringDir ? 1 : 0)]);
            }
        }
        int cut = c ? ChamferProfile.cut(r) : 0;
        for (Direction d : Direction.values()) {
            int s = conn[d.ordinal()];
            if (s <= 0) continue;
            s = Math.min(s, 8);
            int start = d.getAxis() == coreAxis ? r : (s <= r - cut ? r : Math.max(0, r - cut));
            if (start < 8) add(out, sleeves[ch][(d.ordinal() * 9 + s) * 9 + start]);
            if (s == 1) add(out, twigEnds[d.ordinal()]);
        }
        if (upright) addDetails(out, r, c, conn, depth, hash);
        return out;
    }

    /** Quads for a thick trunk block (radius 9..24); its geometry spills over the 3x3 blocks around it. */
    public List<BakedQuad> thick(int r, int[] conn, int twigRadius, @Nullable Direction ringDir, int soilDepth, int hash) {
        boolean c = NhRenderConfig.on(NhRenderConfig.CHAMFER);
        int ch = c ? 1 : 0;
        List<BakedQuad> out = new ArrayList<>(48);
        if (ringDir == Direction.DOWN) {
            conn[Direction.DOWN.ordinal()] = 0;
            add(out, thickBotRings[ch][r]);
        }
        if (soilDepth >= 0 && soilDepth <= 2 && NhRenderConfig.on(NhRenderConfig.MOSS)) {
            add(out, mossSides(r, c, 0, 16, soilDepth, hash));
        } else {
            add(out, thickSides[ch][r]);
        }
        boolean branchesAround = conn[2] + conn[3] + conn[4] + conn[5] != 0;
        for (Direction d : new Direction[]{Direction.DOWN, Direction.UP}) {
            if (ringDir == Direction.DOWN && d == Direction.DOWN) continue;
            int cr = conn[d.ordinal()];
            if (cr < twigRadius && !branchesAround) {
                add(out, d == Direction.UP ? thickTopRings[ch][r] : thickBotRings[ch][r]);
            } else if (cr < r) {
                add(out, d == Direction.UP ? thickTopBark[ch][r] : thickBotBark[ch][r]);
            }
        }
        addDetails(out, r, c, conn, soilDepth, hash);
        return out;
    }

    /** Ring end shown on its own (the cut face of a felled log). */
    public List<BakedQuad> ringOnly(int r, Direction ringDir) {
        boolean c = NhRenderConfig.on(NhRenderConfig.CHAMFER);
        LogAxis axis = axisOf(ringDir.getAxis());
        boolean pos = positive(ringDir);
        List<BakedQuad> out = new ArrayList<>();
        LogShapes.cap(out, Math.min(r, 8), c, pos ? 8 + Math.min(r, 8) : 8 - Math.min(r, 8), pos, rings, UvMode.WORLD, axis);
        return out;
    }

    private static void add(List<BakedQuad> out, BakedQuad[] qs) {
        if (qs != null) out.addAll(Arrays.asList(qs));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Render-only details on upright trunks

    private void addDetails(List<BakedQuad> out, int r, boolean c, int[] conn, int depth, int hash) {
        if (depth == 0 && r >= 4 && NhRenderConfig.on(NhRenderConfig.ROOT_FLARE)) {
            int variant = hash & 15;
            add(out, details.computeIfAbsent(key(1, r, variant, c ? 1 : 0), k -> arr(buttresses(r, c, variant))));
        }
        boolean bare = conn[2] + conn[3] + conn[4] + conn[5] == 0 && conn[0] > 0 && conn[1] > 0;
        if (bare && r >= 3 && (depth < 0 || depth >= 2) && Math.floorMod(hash >>> 8, 5) == 0 && NhRenderConfig.on(NhRenderConfig.STUBS)) {
            int variant = (hash >>> 4) & 63;
            add(out, details.computeIfAbsent(key(2, r, variant, c ? 1 : 0), k -> arr(stub(r, c, variant))));
        }
    }

    private static long key(int kind, int r, int variant, int extra) {
        return ((long) kind << 40) | ((long) r << 24) | ((long) variant << 8) | extra;
    }

    /** 3 or 4 stepped buttresses on the cardinal sides, sized by the trunk. */
    List<BakedQuad> buttresses(int r, boolean c, int variant) {
        List<BakedQuad> out = new ArrayList<>();
        int cut = c ? ChamferProfile.cut(r) : 0;
        int length = clamp(Math.round(r * 0.55f), 2, 10);
        int height = clamp(Math.round(r * 1.1f), 3, 15);
        int width = clamp(Math.round(r * 0.5f), 2, 7);
        int count = 3 + (variant & 1);
        int skip = (variant >> 1) & 3;
        for (int k = 0, made = 0; k < 4 && made < count; k++) {
            Direction d = HORIZONTALS[k];
            if (count == 3 && k == skip) continue;
            made++;
            int h = (variant * 31 + k * 17) & 7;                      // 0..7 per flare
            int len = Math.max(2, length + (h % 3) - 1);
            int ht = Math.max(3, height + (h % 5) - 2);
            int across = ((h >> 1) % 3 - 1) * Math.max(0, r - cut - width) / 2;
            int lo = across - width / 2, hi = lo + width;
            int start = Math.max(0, r - cut - 1);
            int prev = start;
            for (int j = 0; j < 3; j++) {
                int a = r + Math.max(1, Math.round(len * (j + 1) / 3f));
                int top = Math.max(1, Math.round(ht * (3 - j) / 3f));
                int nextTop = j == 2 ? 0 : Math.max(1, Math.round(ht * (2 - j) / 3f));
                flareBox(out, d, prev, a, top, nextTop, lo, hi);
                prev = a;
            }
        }
        return out;
    }

    /** One step of a buttress: top, both sides, and the part of its outer face above the next step. */
    private void flareBox(List<BakedQuad> out, Direction d, int along0, int along1, int top, int nextTop, int lo, int hi) {
        float[] p = place(d, along0, lo), q = place(d, along1, hi);
        float x0 = Math.min(p[0], q[0]), x1 = Math.max(p[0], q[0]), z0 = Math.min(p[1], q[1]), z1 = Math.max(p[1], q[1]);
        QuadMaker.rect(out, Direction.UP, x0, top, z0, x1, top, z1, bark, -1, UvMode.WORLD, LogAxis.Y, true);
        Direction left = d.getCounterClockWise(), right = d.getClockWise();
        sideFace(out, left, x0, z0, x1, z1, 0, top);
        sideFace(out, right, x0, z0, x1, z1, 0, top);
        if (top > nextTop) sideFace(out, d, x0, z0, x1, z1, nextTop, top);
    }

    private void sideFace(List<BakedQuad> out, Direction face, float x0, float z0, float x1, float z1, float y0, float y1) {
        sideFace(out, face, x0, z0, x1, z1, y0, y1, bark);
    }

    private static void sideFace(List<BakedQuad> out, Direction face, float x0, float z0, float x1, float z1, float y0, float y1, TextureAtlasSprite sprite) {
        switch (face) {
            case EAST -> QuadMaker.rect(out, face, x1, y0, z0, x1, y1, z1, sprite, -1, UvMode.WORLD, LogAxis.Y, true);
            case WEST -> QuadMaker.rect(out, face, x0, y0, z0, x0, y1, z1, sprite, -1, UvMode.WORLD, LogAxis.Y, true);
            case SOUTH -> QuadMaker.rect(out, face, x0, y0, z1, x1, y1, z1, sprite, -1, UvMode.WORLD, LogAxis.Y, true);
            default -> QuadMaker.rect(out, face, x0, y0, z0, x1, y1, z0, sprite, -1, UvMode.WORLD, LogAxis.Y, true);
        }
    }

    /** (along, across) from the block centre, on side d, to block pixels (x, z). */
    private static float[] place(Direction d, float along, float across) {
        return switch (d) {
            case EAST -> new float[]{8 + along, 8 + across};
            case WEST -> new float[]{8 - along, 8 - across};
            case SOUTH -> new float[]{8 - across, 8 + along};
            default -> new float[]{8 + across, 8 - along};
        };
    }

    /** A short dead branch stub: a 2-3 pixel box out of the bark with a ring end. */
    List<BakedQuad> stub(int r, boolean c, int variant) {
        List<BakedQuad> out = new ArrayList<>();
        int cut = c ? ChamferProfile.cut(r) : 0;
        Direction d = HORIZONTALS[variant & 3];
        int w = 2 + ((variant >> 2) & 1);
        int len = 3 + ((variant >> 3) & 1);
        int y0 = 4 + ((variant >> 4) % 7 + (variant >> 2)) % 7;
        int room = Math.max(0, r - cut - w);
        int lo = -w / 2 + ((variant >> 4) % 3 - 1) * room / 2, hi = lo + w;
        int start = Math.max(0, r - cut - 1), end = r + len;
        float[] p = place(d, start, lo), q = place(d, end, hi);
        float x0 = Math.min(p[0], q[0]), x1 = Math.max(p[0], q[0]), z0 = Math.min(p[1], q[1]), z1 = Math.max(p[1], q[1]);
        QuadMaker.rect(out, Direction.UP, x0, y0 + w, z0, x1, y0 + w, z1, bark, -1, UvMode.WORLD, LogAxis.Y, true);
        QuadMaker.rect(out, Direction.DOWN, x0, y0, z0, x1, y0, z1, bark, -1, UvMode.WORLD, LogAxis.Y, true);
        sideFace(out, d.getCounterClockWise(), x0, z0, x1, z1, y0, y0 + w);
        sideFace(out, d.getClockWise(), x0, z0, x1, z1, y0, y0 + w);
        sideFace(out, d, x0, z0, x1, z1, y0, y0 + w, rings);
        return out;
    }

    /** Trunk sides with moss growing up from the ground in ragged columns, thicker low down and on the north. */
    private BakedQuad[] mossSides(int r, boolean c, float y0, float y1, int depth, int hash) {
        int variant = hash & 15;
        return details.computeIfAbsent(key(3, r, variant * 4 + depth, c ? 1 : 0), k -> {
            List<BakedQuad> out = new ArrayList<>();
            int col = r >= 9 ? 3 : 2;
            int base = depth == 0 ? 9 : depth == 1 ? 4 : 1;
            int fi = 0;
            for (int[] f : ChamferProfile.sideFaces(LogShapes.rects(r, c))) {
                fi++;
                int north = f[0] == 3 ? 3 : f[0] == 1 ? 1 : 0;
                for (int a = f[2]; a < f[3]; a += col) {
                    int b = Math.min(f[3], a + col);
                    int h = ((variant * 73856093) ^ (fi * 19349663) ^ (a * 83492791)) >>> 3;
                    int top = clamp(base + north + (h % 9) - 4 - (depth > 0 ? 2 : 0), 0, (int) (y1 - y0));
                    int[] piece = {f[0], f[1], a, b};
                    if (top > 0) LogShapes.face(out, piece, y0, y0 + top, moss, LogAxis.Y);
                    if (y0 + top < y1) LogShapes.face(out, piece, y0 + top, y1, bark, LogAxis.Y);
                }
            }
            return arr(out);
        });
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
