package me.marcdoesntexist.galaxymines.messages;

import me.marcdoesntexist.galaxymines.api.chat.Text;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;







public final class Messages {
    private static FileConfiguration config;

    private Messages() {
    }

    public static void load(final FileConfiguration messagesConfig) {
        Messages.config = messagesConfig;
    }

    public static String get(final String key, final String fallback, final Object... replacements) {
        String raw = Messages.config == null ? null : Messages.config.getString(key);
        if (raw == null) {
            raw = fallback;
        }
        if (raw == null) {
            return "";
        }
        if (replacements != null) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                raw = raw.replace(String.valueOf(replacements[i]),
                        Text.escape(String.valueOf(replacements[i + 1])));
            }
        }
        return GalaxyMineMessageManager.getInstance().translateAll(raw);
    }

    public static List<String> getList(final String key) {
        if (Messages.config == null) {
            return Collections.emptyList();
        }
        final List<String> raw = Messages.config.getStringList(key);
        if (raw.isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> translated = new ArrayList<>();
        for (final String line : raw) {
            translated.add(GalaxyMineMessageManager.getInstance().translateAll(line));
        }
        return translated;
    }
}
