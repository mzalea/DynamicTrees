package com.dtteam.dynamictrees.model.nh;

import com.dtteam.dynamictrees.tree.TreeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaves as a plain vanilla cube at the core, with extra geometry only on the outside of the crown: a 2-3 pixel lip
 * where two open faces meet (a ragged silhouette instead of a block edge) and a 3-4 pixel fringe hanging under leaves
 * that face open air below. The extras use tint index 1 so they can be shaded a touch lighter than the leaf itself.
 */
public class FringeLeavesBakedModel extends BakedModelWrapper<BakedModel> {

    /** Bit per direction (by ordinal) that faces open air, plus a 2-bit size variant above bit 6. */
    public static final ModelProperty<Integer> OPEN_SIDES = new ModelProperty<>();
    public static final int FRINGE_TINT = 1;

    private final TextureAtlasSprite leaves;
    private final int tint;
    private final List<BakedQuad>[] fringe;

    /**
     * @param leaves the texture the leaf block shows (its first face), so the fringe is the same leaf
     * @param tinted whether that face is tinted; untinted leaves (pre-coloured textures) get untinted fringe
     */
    @SuppressWarnings("unchecked")
    public FringeLeavesBakedModel(BakedModel base, TextureAtlasSprite leaves, boolean tinted) {
        super(base);
        this.leaves = leaves;
        this.tint = tinted ? FRINGE_TINT : -1;
        this.fringe = new List[64 * 4];
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        ModelData data = super.getModelData(level, pos, state, modelData);
        int mask = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(p.setWithOffset(pos, d));
            if (isOpen(n)) mask |= 1 << d.ordinal();
        }
        int variant = (int) (Mth.getSeed(pos.getX(), pos.getY(), pos.getZ()) >>> 20) & 3;
        return data.derive().with(OPEN_SIDES, mask | variant << 6).build();
    }

    static boolean isOpen(BlockState n) {
        if (n.isAir()) return true;
        if (n.getBlock() instanceof LeavesBlock || TreeHelper.isBranch(n)) return false;
        return !n.canOcclude();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        List<BakedQuad> base = super.getQuads(state, side, rand, extraData, renderType);
        if (side != null || state == null || !NhRenderConfig.on(NhRenderConfig.LEAF_FRINGE) || !Minecraft.useFancyGraphics()) return base;
        Integer open = extraData.get(OPEN_SIDES);
        if (open == null || (open & 63) == 0) return base;
        List<BakedQuad> extra = fringe(open);
        if (extra.isEmpty()) return base;
        List<BakedQuad> out = new ArrayList<>(base.size() + extra.size());
        out.addAll(base);
        out.addAll(extra);
        return out;
    }

    private List<BakedQuad> fringe(int key) {
        List<BakedQuad> l = fringe[key];
        if (l == null) {
            l = build(key & 63, key >> 6, leaves, tint);
            fringe[key] = l;
        }
        return l;
    }

    /** The extra planes for one set of open sides; pixels in block space. */
    static List<BakedQuad> build(int open, int variant, TextureAtlasSprite sprite, int tint) {
        List<BakedQuad> out = new ArrayList<>();
        int lip = 2 + (variant & 1);
        int hang = 3 + (variant >> 1);
        for (Direction f : Direction.values()) {
            if ((open & 1 << f.ordinal()) == 0) continue;
            for (Direction g : Direction.values()) {
                if (g.getAxis() == f.getAxis() || (open & 1 << g.ordinal()) == 0) continue;
                // lip: face f's plane carried past the edge it shares with face g
                float[] lo = {0, 0, 0}, hi = {16, 16, 16};
                int fa = f.getAxis().ordinal(), ga = g.getAxis().ordinal();
                float plane = f.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16 : 0;
                lo[fa] = hi[fa] = plane;
                int len = g == Direction.DOWN ? hang : lip;
                if (g.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    lo[ga] = 16;
                    hi[ga] = 16 + len;
                } else {
                    lo[ga] = -len;
                    hi[ga] = 0;
                }
                QuadMaker.plane(out, f, lo[0], lo[1], lo[2], hi[0], hi[1], hi[2], sprite, tint);
            }
        }
        if ((open & 1 << Direction.DOWN.ordinal()) != 0) {
            // a short curtain under the leaf, crossing at its middle
            QuadMaker.plane(out, Direction.EAST, 8, -hang, 0, 8, 0, 16, sprite, tint);
            QuadMaker.plane(out, Direction.SOUTH, 0, -hang, 8, 16, 0, 8, sprite, tint);
        }
        return List.copyOf(out);
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(Minecraft.useFancyGraphics() ? RenderType.cutoutMipped() : RenderType.solid());
    }
}
