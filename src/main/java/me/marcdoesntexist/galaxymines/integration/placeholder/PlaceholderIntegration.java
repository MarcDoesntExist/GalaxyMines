package me.marcdoesntexist.galaxymines.integration.placeholder;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;


public final class PlaceholderIntegration {
    private static Object expansion;

    private PlaceholderIntegration() {
    }

    public static void setup(final Plugin plugin) {
        unload();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            final Class<?> expansionClass = Class.forName(
                    "me.marcdoesntexist.galaxymines.integration.placeholder."
                            + "GalaxyMinesPlaceholderExpansion");
            final Object instance = expansionClass.getConstructor().newInstance();
            final Class<?> base = Class.forName(
                    "me.clip.placeholderapi.expansion.PlaceholderExpansion");
            final Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            api.getMethod("registerExpansion", base).invoke(null, instance);
            expansion = instance;
            plugin.getLogger().info("PlaceholderAPI expansion registered.");
        } catch (Throwable t) {
            GalaxyMines.getInstance().printException(
                    "PlaceholderAPI is present but the expansion could not be registered.", t);
        }
    }

    public static void unload() {
        if (expansion == null) {
            return;
        }
        try {
            expansion.getClass().getMethod("unregister").invoke(expansion);
        } catch (Throwable ignored) {
        }
        expansion = null;
    }
}
