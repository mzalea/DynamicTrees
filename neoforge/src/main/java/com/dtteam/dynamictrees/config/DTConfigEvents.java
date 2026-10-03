package com.dtteam.dynamictrees.config;

import com.dtteam.dynamictrees.DynamicTrees;
import com.dtteam.dynamictrees.event.handler.OptionalHandlers;
import com.dtteam.dynamictrees.systems.season.SeasonCompatibilityHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

@EventBusSubscriber(modid = DynamicTrees.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DTConfigEvents {

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
        if (isRenderConfig(event)) return;
        OptionalHandlers.configReload();
        SeasonCompatibilityHandler.reloadSeasonManager();
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        if (isRenderConfig(event)) return;
        OptionalHandlers.configReload();
        SeasonCompatibilityHandler.reloadSeasonManager();
    }

    /** The New Haven render config loads on its own schedule (possibly before common) and needs none of this. */
    private static boolean isRenderConfig(ModConfigEvent event) {
        return event.getConfig().getFileName().endsWith("dynamictrees-nh-client.toml");
    }

}
