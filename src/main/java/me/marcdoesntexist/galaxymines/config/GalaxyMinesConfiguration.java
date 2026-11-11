package me.marcdoesntexist.galaxymines.config;

import org.bukkit.configuration.file.FileConfiguration;

public class GalaxyMinesConfiguration {
    public static boolean forceDefaultBroadcastMessage;
    public static FileConfiguration guiConfig;
    public static boolean forceDefaultHolograms;
    public static boolean useHologramMineSync;
    public static long hologramRefreshRate;
    public static FileConfiguration messagesConfig;
    public static boolean useHolograms;


    static {
        GalaxyMinesConfiguration.forceDefaultBroadcastMessage = true;
        GalaxyMinesConfiguration.forceDefaultHolograms = false;
        GalaxyMinesConfiguration.useHologramMineSync = true;
        GalaxyMinesConfiguration.useHolograms = false;
    }
}
