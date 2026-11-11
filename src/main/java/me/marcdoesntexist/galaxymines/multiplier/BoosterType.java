package me.marcdoesntexist.galaxymines.multiplier;

import java.util.Locale;





public enum BoosterType {
    SELL,
    DROP;




    public static BoosterType parse(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final String value = raw.trim().toUpperCase(Locale.ROOT);
        for (final BoosterType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
