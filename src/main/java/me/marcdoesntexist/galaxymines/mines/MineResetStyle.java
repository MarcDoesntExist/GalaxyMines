package me.marcdoesntexist.galaxymines.mines;

import java.util.Locale;





public enum MineResetStyle {
    INSTANT,
    WAVE;

    public static MineResetStyle parse(final String raw) {
        if (raw == null || raw.isBlank()) {
            return INSTANT;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return INSTANT;
        }
    }
}
