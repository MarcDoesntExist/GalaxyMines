package me.marcdoesntexist.galaxymines.mines;

import java.util.Locale;


public enum MineWaveDirection {
    TOP_DOWN,
    BOTTOM_UP;

    public static MineWaveDirection parse(final String raw) {
        if (raw == null || raw.isBlank()) {
            return TOP_DOWN;
        }
        final String value = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (value) {
            case "BOTTOM", "BOTTOM_UP", "UP", "BOTTOMUP" -> BOTTOM_UP;
            default -> TOP_DOWN;
        };
    }

    public boolean descending() {
        return this == TOP_DOWN;
    }
}
