package com.dtteam.dynamictrees.model.nh;

import com.dtteam.dynamictrees.block.leaves.LeavesProperties;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Leaf tint on top of Dynamic Trees' own: biome-tinted leaves lean toward the grass under them (both carry the biome
 * blend and Serene Seasons' seasonal colour, so they move together), leaves deeper in the crown are a little darker,
 * fringe geometry a little lighter, and each block varies slightly.
 */
public final class NhFoliage {

    private NhFoliage() {}

    public static int adjust(int base, LeavesProperties props, BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        // Leaves whose own block has no colour handler come back white; tinted geometry on them (palm fronds) would
        // stay grey, so give it the biome's foliage colour like any other leaf.
        int c = (base & 0xFFFFFF) == 0xFFFFFF ? BiomeColors.getAverageFoliageColor(level, pos) : base;
        double blend = NhRenderConfig.num(NhRenderConfig.FOLIAGE_GRASS_BLEND);
        if (blend > 0 && (followsBiome(props) || (base & 0xFFFFFF) == 0xFFFFFF)) {
            c = FoliageBlend.mix(c, BiomeColors.getAverageGrassColor(level, pos), blend);
        }
        int hydration = state.hasProperty(LeavesBlock.DISTANCE) ? state.getValue(LeavesBlock.DISTANCE) : 1;
        double f = FoliageBlend.factor(hydration, tintIndex == FringeLeavesBakedModel.FRINGE_TINT,
                NhRenderConfig.num(NhRenderConfig.LEAF_DEPTH_SHADE), NhRenderConfig.num(NhRenderConfig.FRINGE_LIGHTEN),
                NhRenderConfig.num(NhRenderConfig.FOLIAGE_JITTER), (int) Mth.getSeed(pos.getX(), pos.getY(), pos.getZ()));
        return FoliageBlend.scale(c, f);
    }

    /** Leaves tinted by the biome (not birch or spruce, whose fixed colours are part of their look). */
    static boolean followsBiome(LeavesProperties props) {
        if (props.hasOwnColor()) return false;
        BlockState prim = props.getPrimitiveLeaves();
        return !prim.is(Blocks.BIRCH_LEAVES) && !prim.is(Blocks.SPRUCE_LEAVES);
    }
}
