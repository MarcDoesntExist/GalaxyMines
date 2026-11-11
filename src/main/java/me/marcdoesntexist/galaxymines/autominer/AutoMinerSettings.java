package me.marcdoesntexist.galaxymines.autominer;

import org.bukkit.configuration.ConfigurationSection;


public record AutoMinerSettings(boolean enabled, int intervalSeconds, int radius,
                                int maxBlocksPerCycle) {

    public static AutoMinerSettings defaults() {
        return new AutoMinerSettings(false, 5, 2, 4);
    }

    public static AutoMinerSettings of(final ConfigurationSection config) {
        if (config == null) {
            return defaults();
        }
        return new AutoMinerSettings(
                config.getBoolean("enabled", false),
                clamp(config.getInt("interval-seconds", 5), 1, 300),
                clamp(config.getInt("radius", 2), 1, 8),
                clamp(config.getInt("max-blocks-per-cycle", 4), 1, 64));
    }

    private static int clamp(final int value, final int minimum, final int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
