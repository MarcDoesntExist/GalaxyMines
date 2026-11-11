package me.marcdoesntexist.galaxymines.config;

import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;


public final class MenuConfig {
    private static FileConfiguration config;

    private MenuConfig() {
    }

    public static void load(final FileConfiguration guiConfig) {
        MenuConfig.config = guiConfig;
    }

    public static String title(final String menu, final String fallback) {
        final ConfigurationSection section = menuSection(menu);
        if (section != null && section.isString("title")) {
            return section.getString("title", fallback);
        }
        if ("main".equals(menu) && MenuConfig.config != null) {
            final String legacy = MenuConfig.config.getString("mainMenu.name");
            if (legacy != null) {
                return legacy;
            }
        }
        return fallback;
    }

    public static int rows(final String menu, final int fallback) {
        final ConfigurationSection section = menuSection(menu);
        int rows = section == null ? fallback : section.getInt("rows", fallback);
        if (section == null && "main".equals(menu) && MenuConfig.config != null) {
            rows = MenuConfig.config.getInt("mainMenu.totalLanes", fallback);
        }
        return Math.max(1, Math.min(6, rows));
    }

    public static int slot(final String menu, final String key, final int fallback) {
        final ConfigurationSection item = itemSection(menu, key);
        return item == null ? fallback : item.getInt("slot", fallback);
    }


    public static int[] range(final String menu, final String key, final int fallbackFirst,
                              final int fallbackLast) {
        final ConfigurationSection item = itemSection(menu, key);
        final String raw = item == null ? null : item.getString("slots");
        if (raw == null || raw.trim().isEmpty()) {
            return new int[]{fallbackFirst, fallbackLast};
        }
        final String[] parts = raw.split("-", 2);
        try {
            final int first = Integer.parseInt(parts[0].trim());
            final int second =
                    parts.length > 1 ? Integer.parseInt(parts[1].trim()) : first;
            return new int[]{Math.min(first, second), Math.max(first, second)};
        } catch (NumberFormatException ex) {
            return new int[]{fallbackFirst, fallbackLast};
        }
    }

    public static String action(final String menu, final String key, final String fallback) {
        final ConfigurationSection item = itemSection(menu, key);
        final String action = item == null ? null : item.getString("action");
        return action == null || action.isBlank() ? fallback : action;
    }

    public static String actionArgument(final String menu, final String key, final String fallback) {
        final ConfigurationSection item = itemSection(menu, key);
        if (item == null) {
            return fallback;
        }
        String argument = item.getString("action-arg");
        if (argument == null) {
            argument = item.getString("action-argument");
        }
        return argument == null ? fallback : argument;
    }


    public static ActionDefinition actionDefinition(final String id) {
        if (MenuConfig.config == null || id == null || id.isBlank()) {
            return null;
        }
        final ConfigurationSection actions = MenuConfig.config.getConfigurationSection("actions");
        if (actions == null) {
            return null;
        }
        for (final String key : actions.getKeys(false)) {
            if (!key.equalsIgnoreCase(id)) {
                continue;
            }
            return readDefinition(actions, key);
        }
        return null;
    }


    public static boolean hasActionDefinition(final String id) {
        if (MenuConfig.config == null || id == null || id.isBlank()) {
            return false;
        }
        final ConfigurationSection actions = MenuConfig.config.getConfigurationSection("actions");
        if (actions == null) {
            return false;
        }
        for (final String key : actions.getKeys(false)) {
            if (key.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }


    public static Map<String, ActionDefinition> actionDefinitions() {
        final Map<String, ActionDefinition> definitions = new LinkedHashMap<>();
        if (MenuConfig.config == null) {
            return definitions;
        }
        final ConfigurationSection actions = MenuConfig.config.getConfigurationSection("actions");
        if (actions == null) {
            return definitions;
        }
        for (final String key : actions.getKeys(false)) {
            final ActionDefinition definition = readDefinition(actions, key);
            if (definition != null) {
                definitions.put(key, definition);
            }
        }
        return definitions;
    }

    private static ActionDefinition readDefinition(final ConfigurationSection actions,
                                                   final String key) {
        final ConfigurationSection section = actions.getConfigurationSection(key);
        if (section == null) {
            final String type = normalize(actions.getString(key));
            return type == null ? null
                    : new ActionDefinition(type, null, null, false, "stop", List.of());
        }
        final String type = normalize(section.getString("type"));
        String argument = section.getString("argument");
        if (argument == null) {
            argument = section.getString("action-arg");
        }
        String permission = null;
        if (section.contains("permission")) {
            final String raw = section.getString("permission");
            permission = raw == null || raw.isBlank() || "none".equalsIgnoreCase(raw.trim())
                    ? "" : raw.trim();
        }
        final List<ActionStep> steps = readSteps(section);
        if (type == null && steps.isEmpty()) {
            return null;
        }
        final String sequenceMode = section.getString("sequence-mode",
                section.getString("sequenceMode", "stop"));
        return new ActionDefinition(type, argument, permission,
                section.getBoolean("confirm", false), sequenceMode, steps);
    }

    private static List<ActionStep> readSteps(final ConfigurationSection section) {
        final List<?> raw = section.getList("sequence");
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        final List<ActionStep> steps = new ArrayList<>();
        for (final Object entry : raw) {
            if (entry instanceof String text) {
                final String type = normalize(text);
                if (type != null) {
                    steps.add(new ActionStep(type, null));
                }
                continue;
            }
            String type = null;
            String argument = null;
            if (entry instanceof ConfigurationSection step) {
                type = normalize(step.getString("type"));
                argument = step.getString("argument");
                if (argument == null) {
                    argument = step.getString("action-arg");
                }
            } else if (entry instanceof Map<?, ?> map) {
                type = normalize(map.get("type") == null ? null : String.valueOf(map.get("type")));
                final Object rawArgument = map.get("argument") != null
                        ? map.get("argument") : map.get("action-arg");
                argument = rawArgument == null ? null : String.valueOf(rawArgument);
            }
            if (type != null) {
                steps.add(new ActionStep(type, argument));
            }
        }
        return steps;
    }

    private static String normalize(final String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeSequenceMode(final String value) {
        return "continue".equalsIgnoreCase(value == null ? null : value.trim())
                ? "continue" : "stop";
    }


    public static long confirmationTimeoutMillis() {
        if (MenuConfig.config == null) {
            return 10_000L;
        }
        final long seconds = MenuConfig.config.getLong("settings.confirmTimeoutSeconds",
                MenuConfig.config.getLong("settings.confirm-timeout-seconds", 10L));
        return Math.max(1L, seconds) * 1000L;
    }

    public static ItemStack item(final String menu, final String key) {
        final ConfigurationSection item = itemSection(menu, key);
        final String reference = item == null || !item.isString("item") ? key
                : item.getString("item", key);
        return item(reference);
    }


    public static ItemStack item(final String key) {
        if (MenuConfig.config == null || key == null) {
            return null;
        }
        ConfigurationSection section =
                MenuConfig.config.getConfigurationSection("globalItems." + key);
        if (section == null) {
            section = MenuConfig.config.getConfigurationSection("mainMenu.icons." + key);
        }
        return section == null ? null : ItemUtils.createItem(section);
    }

    public static ItemStack fill(final String menu) {
        final ConfigurationSection section = menuSection(menu);
        final String key = section == null ? "empty" : section.getString("fill", "empty");
        final ItemStack item = item(menu, key);
        return item != null ? item : item(key);
    }


    public static List<Double> percentageSteps(final String menu, final double[] fallback) {
        final ConfigurationSection section = menuSection(menu);
        final List<Double> configured =
                section == null ? Collections.emptyList() : section.getDoubleList("percentageSteps");
        final List<Double> valid = new ArrayList<>();
        for (final Double value : configured) {
            if (value != null && Double.isFinite(value) && value > 0.0D) {
                valid.add(value);
            }
        }
        if (valid.isEmpty()) {
            final List<Double> defaults = new ArrayList<>();
            for (final double value : fallback) {
                defaults.add(value);
            }
            return defaults;
        }
        return valid;
    }


    public static List<String> validate() {
        final List<String> warnings = new ArrayList<>();
        if (MenuConfig.config == null) {
            return warnings;
        }
        final ConfigurationSection actions = MenuConfig.config.getConfigurationSection("actions");
        if (actions != null) {
            for (final String key : actions.getKeys(false)) {
                if (readDefinition(actions, key) == null) {
                    warnings.add("Action '" + key + "' has no type or sequence.");
                }
            }
        }
        final ConfigurationSection menus = MenuConfig.config.getConfigurationSection("menus");
        if (menus == null) {
            return warnings;
        }
        for (final String menu : menus.getKeys(false)) {
            final ConfigurationSection section = menus.getConfigurationSection(menu);
            if (section == null) {
                warnings.add("Menu '" + menu + "' is not a section.");
                continue;
            }
            final int rows = section.getInt("rows", 6);
            if (rows < 1 || rows > 6) {
                warnings.add("Menu '" + menu + "' rows " + rows
                        + " is outside 1-6 and will be clamped.");
            }
            final int maxSlot = Math.max(1, Math.min(6, rows)) * 9 - 1;
            validateItems(menu, section, maxSlot, warnings);
            validatePercentageSteps(menu, section, warnings);
            validateFill(menu, section, warnings);
        }
        return warnings;
    }

    private static void validateItems(final String menu, final ConfigurationSection section,
                                      final int maxSlot, final List<String> warnings) {
        final ConfigurationSection items = section.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (final String key : items.getKeys(false)) {
            final ConfigurationSection item = items.getConfigurationSection(key);
            if (item == null) {
                continue;
            }
            if (item.contains("slot")) {
                final int slot = item.getInt("slot", -1);
                if (slot < 0 || slot > maxSlot) {
                    warnings.add("Menu '" + menu + "' item '" + key + "' slot " + slot
                            + " is outside 0-" + maxSlot + ".");
                }
            }
            if (item.contains("slots")) {
                final String raw = item.getString("slots");
                final int[] range = parseRange(raw);
                if (range == null) {
                    warnings.add("Menu '" + menu + "' item '" + key
                            + "' has an invalid slots value '" + raw + "'.");
                } else if (range[0] < 0 || range[1] > maxSlot) {
                    warnings.add("Menu '" + menu + "' item '" + key + "' slots '" + raw
                            + "' are outside 0-" + maxSlot + ".");
                }
            }
            if (item.contains("item")) {
                final String reference = normalize(item.getString("item"));
                if (reference != null && !itemReferenceExists(reference)) {
                    warnings.add("Menu '" + menu + "' item '" + key
                            + "' references unknown item '" + reference + "'.");
                }
            }
        }
    }

    private static void validatePercentageSteps(final String menu,
                                                final ConfigurationSection section,
                                                final List<String> warnings) {
        for (final Double value : section.getDoubleList("percentageSteps")) {
            if (value == null || !Double.isFinite(value) || value <= 0.0D) {
                warnings.add("Menu '" + menu + "' percentageSteps contains invalid value "
                        + value + "; it will be ignored.");
            }
        }
    }

    private static void validateFill(final String menu, final ConfigurationSection section,
                                     final List<String> warnings) {
        final String fill = normalize(section.getString("fill"));
        if (fill != null && !itemReferenceExists(fill)) {
            warnings.add("Menu '" + menu + "' fill references unknown item '" + fill + "'.");
        }
    }

    private static boolean itemReferenceExists(final String reference) {
        return MenuConfig.config.getConfigurationSection("globalItems." + reference) != null
                || MenuConfig.config.getConfigurationSection(
                "mainMenu.icons." + reference) != null;
    }

    private static int[] parseRange(final String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        final String[] parts = raw.split("-", 2);
        try {
            final int first = Integer.parseInt(parts[0].trim());
            final int second = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : first;
            return new int[]{Math.min(first, second), Math.max(first, second)};
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static List<String> menuNames() {
        if (MenuConfig.config == null) {
            return List.of();
        }
        final ConfigurationSection menus = MenuConfig.config.getConfigurationSection("menus");
        return menus == null ? List.of() : new ArrayList<>(menus.getKeys(false));
    }

    public static List<String> itemKeys(final String menu) {
        final ConfigurationSection section = menuSection(menu);
        if (section == null) {
            return List.of();
        }
        final ConfigurationSection items = section.getConfigurationSection("items");
        return items == null ? List.of() : new ArrayList<>(items.getKeys(false));
    }

    public static String string(final String path, final String fallback) {
        return MenuConfig.config == null ? fallback : MenuConfig.config.getString(path, fallback);
    }

    private static ConfigurationSection menuSection(final String menu) {
        return MenuConfig.config == null ? null
                : MenuConfig.config.getConfigurationSection("menus." + menu);
    }

    private static ConfigurationSection itemSection(final String menu, final String key) {
        final ConfigurationSection section = menuSection(menu);
        return section == null ? null : section.getConfigurationSection("items." + key);
    }


    public record ActionStep(String type, String argument) {
        public ActionStep {
            type = normalize(type);
        }
    }


    public record ActionDefinition(String type, String argument, String permission,
                                   boolean confirm, String sequenceMode, List<ActionStep> steps) {
        public ActionDefinition {
            type = normalize(type);
            sequenceMode = normalizeSequenceMode(sequenceMode);
            steps = steps == null ? List.of() : List.copyOf(steps);
        }

        public boolean isEmpty() {
            return this.type == null && this.steps.isEmpty();
        }

        public boolean continueOnError() {
            return "continue".equals(this.sequenceMode);
        }
    }
}
