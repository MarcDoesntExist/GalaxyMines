package me.marcdoesntexist.galaxymines.events;

import me.marcdoesntexist.galaxymines.events.MineEventDefinition.EventActions;
import me.marcdoesntexist.galaxymines.events.MineEventDefinition.RareDrop;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockTokens;
import me.marcdoesntexist.galaxymines.utils.ScheduleTimes;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;





public final class MineEventParser {
    private MineEventParser() {
    }

    public static MineEventDefinition parse(final String rawId,
                                            final ConfigurationSection section,
                                            final Consumer<String> warning) {
        if (section == null || !section.getBoolean("enabled", true)) {
            return null;
        }
        final Consumer<String> warn = warning == null ? ignored -> {
        } : warning;
        final String id = rawId.toLowerCase(Locale.ROOT);
        final String configuredType = section.getString("type", "DOUBLE_DROPS");
        final EventType type;
        try {
            type = EventType.valueOf((configuredType == null ? "DOUBLE_DROPS" : configuredType)
                    .trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            warn.accept("Unknown event type for '" + id + "' in events.yml");
            return null;
        }
        final String configuredName = section.getString("display-name", rawId);
        final String displayName = configuredName == null || configuredName.isBlank()
                ? rawId : configuredName.trim();
        final List<LocalTime> times = ScheduleTimes.parse(section.getStringList("times"),
                invalid -> warn.accept(
                        "Invalid event time '" + invalid + "' for '" + id + "'"));
        final List<String> mines = new ArrayList<>();
        for (final String mine : section.getStringList("mines")) {
            if (mine != null && !mine.isBlank()) {
                mines.add(mine.trim().toLowerCase(Locale.ROOT));
            }
        }
        return new MineEventDefinition(id, displayName,
                Math.max(1, section.getInt("duration-seconds", 300)),
                times, mines,
                section.getBoolean("auto-start", false),
                section.getBoolean("broadcast", true),
                text(section, "messages.start"),
                text(section, "messages.stop"),
                commands(section.getStringList("commands.start")),
                commands(section.getStringList("commands.stop")),
                parseActions(section, type, warn, id));
    }

    private static EventActions parseActions(final ConfigurationSection section,
                                             final EventType type,
                                             final Consumer<String> warning,
                                             final String id) {
        final ConfigurationSection actions = section.getConfigurationSection("actions");
        if (actions != null && !actions.getKeys(false).isEmpty()) {
            return new EventActions(
                    actions.getDouble("drop-multiplier", 1.0D),
                    actions.getDouble("sell-multiplier", 1.0D),
                    parseRareDrops(actions, warning, id));
        }
        final double multiplier = Math.max(1.0D, section.getDouble("multiplier", 2.0D));
        return switch (type) {
            case DOUBLE_DROPS -> new EventActions(multiplier, 1.0D, List.of());
            case SELL_BONUS -> new EventActions(1.0D, multiplier, List.of());
            case RARE_BLOCK -> {
                final Material material = Material.matchMaterial(
                        section.getString("material", "DIAMOND"));
                if (material == null) {
                    warning.accept("Unknown rare-block material for event '" + id + "'");
                    yield EventActions.NONE;
                }
                yield new EventActions(1.0D, 1.0D, List.of(new RareDrop(material,
                        section.getDouble("chance", 5.0D),
                        Math.max(1, section.getInt("amount", 1)))));
            }
            case CUSTOM -> EventActions.NONE;
        };
    }

    private static List<RareDrop> parseRareDrops(final ConfigurationSection actions,
                                                 final Consumer<String> warning,
                                                 final String id) {
        final List<RareDrop> drops = new ArrayList<>();
        final List<Map<?, ?>> configuredDrops;
        try {
            configuredDrops = actions.getMapList("rare-drops");
        } catch (RuntimeException ex) {
            warning.accept("Invalid rare-drops list for event '" + id + "'");
            return drops;
        }
        for (final Map<?, ?> entry : configuredDrops) {
            if (entry == null) {
                continue;
            }
            final int amount = (int) Math.max(1.0D, asDouble(entry.get("amount"), 1.0D));
            final Object configuredItem = entry.get("item");
            if (configuredItem != null && !String.valueOf(configuredItem).isBlank()) {
                final String token = CustomBlockTokens.normalize(String.valueOf(configuredItem));
                if (token == null) {
                    warning.accept("Unknown rare-drop provider item '" + configuredItem
                            + "' for event '" + id + "'");
                    continue;
                }
                drops.add(new RareDrop(null, token, asDouble(entry.get("chance"), 5.0D), amount));
                continue;
            }
            final Object configuredMaterial = entry.get("material");
            final Material material = configuredMaterial == null ? null
                    : Material.matchMaterial(String.valueOf(configuredMaterial));
            if (material == null || material == Material.AIR
                    || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
                warning.accept("Unknown rare-drop material for event '" + id + "'");
                continue;
            }
            drops.add(new RareDrop(material, null, asDouble(entry.get("chance"), 5.0D), amount));
        }
        return drops;
    }

    private static double asDouble(final Object value, final double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static String text(final ConfigurationSection section, final String path) {
        final String value = section.getString(path);
        return value == null || value.isBlank() ? null : value;
    }

    private static List<String> commands(final List<String> raw) {
        final List<String> commands = new ArrayList<>();
        for (final String command : raw) {
            if (command != null && !command.isBlank()) {
                commands.add(command.trim());
            }
        }
        return commands;
    }

    private enum EventType {
        DOUBLE_DROPS,
        SELL_BONUS,
        RARE_BLOCK,
        CUSTOM
    }
}
