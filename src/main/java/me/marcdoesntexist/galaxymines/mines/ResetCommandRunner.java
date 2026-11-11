package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import org.bukkit.Bukkit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;











public final class ResetCommandRunner {
    private ResetCommandRunner() {
    }



    public static void run(final String phase, final Mine mine, final List<String> commands) {
        if (mine == null || commands == null || commands.isEmpty()
                || !PluginSettings.get().reset.commandsEnabled) {
            return;
        }
        final Map<String, String> placeholders = placeholders(mine);
        for (final String raw : commands) {
            final String command = expand(raw, placeholders);
            if (command.isEmpty()) {
                continue;
            }
            execute(command, mine, phase);
        }
    }




    static String expand(final String template, final Map<String, String> placeholders) {
        if (template == null) {
            return "";
        }
        String result = template;
        if (placeholders != null) {
            for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace("%" + entry.getKey() + "%",
                        entry.getValue() == null ? "" : entry.getValue());
            }
        }
        result = result.trim();
        while (result.startsWith("/")) {
            result = result.substring(1).trim();
        }
        return result;
    }



    static Map<String, String> placeholders(final Mine mine) {
        final Map<String, String> values = new LinkedHashMap<>();
        values.put("mine", mine.getName());
        values.put("alias", mine.getAlias() == null ? mine.getName() : mine.getAlias());
        values.put("world", mine.getWorld() == null ? "" : mine.getWorld().getName());
        values.put("volume", Integer.toString(mine.getVolume()));
        values.put("remaining", Integer.toString(mine.getRemainingBlocks()));
        values.put("percent", String.format(Locale.ROOT, "%.2f", mine.getPercentageRemaining()));
        return values;
    }

    private static void execute(final String command, final Mine mine, final String phase) {
        if (!Bukkit.isPrimaryThread()) {
            GalaxyMines.getInstance().debug("Skipped the " + phase + " reset command of mine "
                    + mine.getName() + " because it was not on the main thread: " + command, true);
            return;
        }
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (RuntimeException ex) {
            GalaxyMines.getInstance().debug("The " + phase + " reset command of mine "
                    + mine.getName() + " failed: " + ex.getMessage(), true);
        }
    }
}
