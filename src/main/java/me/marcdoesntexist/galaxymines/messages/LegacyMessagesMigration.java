package me.marcdoesntexist.galaxymines.messages;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.BasicMessageManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;





public final class LegacyMessagesMigration {
    private LegacyMessagesMigration() {
    }

    public static void migrate(final File file) {
        if (!file.isFile()) {
            return;
        }
        final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean changed = false;
        for (final String key : config.getKeys(true)) {
            if (!config.isString(key)) {
                continue;
            }
            final String original = config.getString(key);
            if (original == null) {
                continue;
            }
            final String converted = convertAmpersands(original);
            if (!converted.equals(original)) {
                config.set(key, converted);
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        final Path path = file.toPath();
        final Path backup = path.resolveSibling(file.getName() + ".bak-"
                + System.currentTimeMillis());
        try {
            Files.copy(path, backup, StandardCopyOption.COPY_ATTRIBUTES);
            config.save(file);
            GalaxyMines.getInstance().getLogger()
                    .info("Messages.yml converted to MiniMessage; backup in " + backup);
        } catch (IOException ex) {
            GalaxyMines.getInstance().getLogger()
                    .log(Level.SEVERE, "Could not migrate Messages.yml", ex);
        }
    }

    static String convertAmpersands(final String input) {
        final StringBuilder out = new StringBuilder(input.length() + 8);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < input.length()
                    && "0123456789abcdefklmnor".indexOf(Character.toLowerCase(input.charAt(i + 1)))
                    >= 0) {
                out.append(BasicMessageManager.tagFor(input.charAt(++i)));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
