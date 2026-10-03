package com.dtteam.dynamictrees.model.nh;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client switches for the New Haven tree look (dynamictrees-nh-client.toml). Every piece can be turned off on its
 * own; a change re-renders loaded chunks, so the before and after can be compared on the spot.
 */
public final class NhRenderConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CHAMFER;
    public static final ModConfigSpec.BooleanValue ROOT_FLARE;
    public static final ModConfigSpec.BooleanValue STUBS;
    public static final ModConfigSpec.BooleanValue MOSS;
    public static final ModConfigSpec.BooleanValue LEAF_FRINGE;
    public static final ModConfigSpec.DoubleValue LEAF_DEPTH_SHADE;
    public static final ModConfigSpec.DoubleValue FRINGE_LIGHTEN;
    public static final ModConfigSpec.DoubleValue FOLIAGE_GRASS_BLEND;
    public static final ModConfigSpec.DoubleValue FOLIAGE_JITTER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("trunks");
        CHAMFER = b.comment("Cut the corners of logs back in pixel steps so they read round.").define("chamfer", true);
        ROOT_FLARE = b.comment("Buttress roots at the foot of grown trunks.").define("rootFlare", true);
        STUBS = b.comment("Short dead branch stubs on bare stretches of trunk.").define("stubs", true);
        MOSS = b.comment("Moss on the lowest blocks of thick trunks.").define("moss", true);
        b.pop();
        b.push("leaves");
        LEAF_FRINGE = b.comment("Ragged overhangs on the outer leaves and a short hanging fringe underneath (Fancy graphics only).").define("fringe", true);
        LEAF_DEPTH_SHADE = b.comment("How much darker each step deeper into the crown is (0 = flat).").defineInRange("depthShade", 0.035, 0.0, 0.2);
        FRINGE_LIGHTEN = b.comment("How much lighter the fringe is than the leaf it hangs from.").defineInRange("fringeLighten", 0.06, 0.0, 0.3);
        FOLIAGE_GRASS_BLEND = b.comment("Share of the ground's grass colour mixed into biome-tinted leaves, so trees and ground match.").defineInRange("grassBlend", 0.35, 0.0, 1.0);
        FOLIAGE_JITTER = b.comment("Random brightness variation per leaf block.").defineInRange("jitter", 0.04, 0.0, 0.2);
        b.pop();
        SPEC = b.build();
    }

    private NhRenderConfig() {}

    /** Config values are only safe to read once loaded; before that (early model baking) the defaults stand. */
    public static boolean on(ModConfigSpec.BooleanValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static double num(ModConfigSpec.DoubleValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != SPEC || event.getConfig().getType() != ModConfig.Type.CLIENT) return;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
        });
    }
}
