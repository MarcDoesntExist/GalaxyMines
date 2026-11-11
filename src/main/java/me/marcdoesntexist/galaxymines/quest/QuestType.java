package me.marcdoesntexist.galaxymines.quest;

import java.util.Locale;


public enum QuestType {
    BLOCKS_MINED,
    ITEMS_SOLD,
    MONEY_EARNED;

    public static QuestType parse(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final String value = raw.trim().toUpperCase(Locale.ROOT);
        for (final QuestType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
