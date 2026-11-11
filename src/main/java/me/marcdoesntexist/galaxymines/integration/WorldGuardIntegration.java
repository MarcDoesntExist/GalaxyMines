package me.marcdoesntexist.galaxymines.integration;

import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;





public final class WorldGuardIntegration {
    private static MineRegionProtector protector = MineRegionProtector.NOOP;

    private WorldGuardIntegration() {
    }

    public static void load(final Plugin plugin) {
        unload();
        final Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (worldGuard == null || !worldGuard.isEnabled()) {
            return;
        }
        try {
            final Class<?> implementation = Class.forName(
                    "me.marcdoesntexist.galaxymines.integration.WorldGuardRegionProtector");
            final Object instance = implementation.getConstructor(Plugin.class)
                    .newInstance(plugin);
            if (instance instanceof MineRegionProtector loaded) {
                protector = loaded;
                loaded.reload();
                plugin.getLogger().info("WorldGuard integration enabled.");
            }
        } catch (Throwable t) {
            Logger.getLogger("GalaxyMines").log(Level.WARNING,
                    "WorldGuard is present but its API could not be used; mine regions are not "
                            + "managed by WorldGuard.", t);
            protector = MineRegionProtector.NOOP;
        }
    }

    public static void unload() {
        protector.close();
        protector = MineRegionProtector.NOOP;
    }

    public static boolean isEnabled() {
        return protector != MineRegionProtector.NOOP;
    }

    public static void onMineRegistered(final Mine mine) {
        protector.onMineRegistered(mine);
    }

    public static void onMineDeleted(final Mine mine) {
        protector.onMineDeleted(mine);
    }

    public static void onMineRenamed(final Mine mine, final String oldName) {
        protector.onMineRenamed(mine, oldName);
    }
}
