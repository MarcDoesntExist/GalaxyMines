package me.marcdoesntexist.galaxymines.luckyblocks;

import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockTokens;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;





public final class LuckyBlockParser {
    private LuckyBlockParser() {
    }

    public static Result parse(final FileConfiguration config) {
        final Map<String, LuckyBlockTable> tables = new LinkedHashMap<>();
        final Map<String, MineLuckyBlocks> mines = new LinkedHashMap<>();
        final List<String> warnings = new ArrayList<>();
        if (config == null) {
            return Result.EMPTY;
        }

        final ConfigurationSection tableSection = config.getConfigurationSection("tables");
        if (tableSection != null) {
            for (final String rawId : tableSection.getKeys(false)) {
                final String id = rawId == null ? "" : rawId.trim().toLowerCase(Locale.ROOT);
                if (id.isEmpty()) {
                    warnings.add("Skipping a lucky-block table with an empty id.");
                    continue;
                }
                final ConfigurationSection table = tableSection.getConfigurationSection(rawId);
                if (table == null) {
                    warnings.add("Lucky-block table '" + rawId + "' is not a section.");
                    continue;
                }
                final int rolls = Math.max(1, Math.min(10, table.getInt("rolls", 1)));
                final List<Map<?, ?>> rewardMaps;
                try {
                    rewardMaps = table.getMapList("rewards");
                } catch (RuntimeException ex) {
                    warnings.add("Lucky-block table '" + id + "' has an invalid rewards list.");
                    continue;
                }
                final List<LuckyBlockTable.Reward> rewards = new ArrayList<>();
                for (int i = 0; i < rewardMaps.size(); ++i) {
                    final Map<?, ?> rewardMap = rewardMaps.get(i);
                    if (rewardMap == null) {
                        warnings.add("Lucky-block table '" + id + "' reward #" + (i + 1)
                                + " is empty.");
                        continue;
                    }
                    final LuckyBlockTable.Reward reward =
                            parseReward(rewardMap, id, i + 1, warnings);
                    if (reward != null && reward.chance() > 0.0D) {
                        rewards.add(reward);
                    }
                }
                if (rewards.isEmpty()) {
                    warnings.add("Lucky-block table '" + id + "' has no usable rewards.");
                    continue;
                }
                tables.put(id, new LuckyBlockTable(id, rolls, rewards));
            }
        }

        final ConfigurationSection mineSection = config.getConfigurationSection("mines");
        if (mineSection != null) {
            for (final String rawMine : mineSection.getKeys(false)) {
                final String mineName = rawMine == null
                        ? "" : rawMine.trim().toLowerCase(Locale.ROOT);
                if (mineName.isEmpty()) {
                    warnings.add("Skipping a lucky-block mine with an empty name.");
                    continue;
                }
                final ConfigurationSection mine = mineSection.getConfigurationSection(rawMine);
                if (mine == null) {
                    warnings.add("Lucky-block mine '" + rawMine + "' is not a section.");
                    continue;
                }
                final ConfigurationSection blockSection = mine.getConfigurationSection("blocks");
                if (blockSection == null) {
                    warnings.add("Lucky-block mine '" + rawMine + "' has no blocks section.");
                    continue;
                }
                final Map<String, String> blocks = new LinkedHashMap<>();
                for (final String rawBlock : blockSection.getKeys(false)) {
                    final String tableName = resolveTableName(blockSection, rawBlock);
                    if (tableName == null || tableName.isBlank()) {
                        warnings.add("Lucky-block mine '" + rawMine + "' block '" + rawBlock
                                + "' has no table.");
                        continue;
                    }
                    final String normalizedTable = tableName.trim().toLowerCase(Locale.ROOT);
                    if (!tables.containsKey(normalizedTable)) {
                        warnings.add("Lucky-block mine '" + rawMine + "' block '" + rawBlock
                                + "' references unknown table '" + tableName + "'.");
                        continue;
                    }
                    final String blockKey = normalizeBlockKey(rawBlock);
                    if (blockKey == null) {
                        warnings.add("Lucky-block mine '" + rawMine + "' block '" + rawBlock
                                + "' is not a valid material or provider token.");
                        continue;
                    }
                    blocks.put(blockKey, normalizedTable);
                }
                if (blocks.isEmpty()) {
                    warnings.add("Lucky-block mine '" + rawMine + "' has no usable blocks.");
                    continue;
                }
                mines.put(mineName, new MineLuckyBlocks(mineName,
                        mine.getBoolean("replace-drops", false), blocks));
            }
        }

        final ConfigurationSection feedbackSection = config.getConfigurationSection("feedback");
        final LuckyBlockFeedback feedback = feedbackSection == null
                ? LuckyBlockFeedback.NONE
                : new LuckyBlockFeedback(feedbackSection.getString("sound", ""),
                feedbackSection.getString("particle", ""),
                feedbackSection.getInt("cooldown-seconds", 0));

        return new Result(Collections.unmodifiableMap(tables),
                Collections.unmodifiableMap(mines), List.copyOf(warnings), feedback);
    }

    private static String resolveTableName(final ConfigurationSection blockSection,
                                           final String rawBlock) {
        final ConfigurationSection nested = blockSection.getConfigurationSection(rawBlock);
        if (nested != null) {
            return nested.getString("table");
        }
        return blockSection.getString(rawBlock);
    }

    private static LuckyBlockTable.Reward parseReward(final Map<?, ?> map, final String tableId,
                                                      final int index,
                                                      final List<String> warnings) {
        final String rawType = string(map.get("type"));
        final LuckyBlockTable.Type type;
        try {
            type = LuckyBlockTable.Type.valueOf(
                    (rawType == null ? "ITEM" : rawType).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            warnings.add("Lucky-block table '" + tableId + "' reward #" + index
                    + " has unknown type '" + rawType + "'.");
            return null;
        }

        double chance = 100.0D;
        final Object rawChance = map.get("chance");
        if (rawChance != null) {
            final Double parsed = number(rawChance);
            if (parsed == null) {
                warnings.add("Lucky-block table '" + tableId + "' reward #" + index
                        + " has an invalid chance.");
                return null;
            }
            chance = parsed;
        }
        if (chance <= 0.0D) {
            warnings.add("Lucky-block table '" + tableId + "' reward #" + index
                    + " has a chance of zero and will never fire.");
            return null;
        }

        final String message = string(map.get("message"));
        return switch (type) {
            case ITEM -> {
                final AmountRange amounts = amountRange(map.get("amount"), tableId, index, warnings);
                final String configuredItem = string(map.get("item"));
                if (configuredItem != null && !configuredItem.isBlank()) {
                    final String token = CustomBlockTokens.normalize(configuredItem);
                    if (token == null) {
                        warnings.add("Lucky-block table '" + tableId + "' reward #" + index
                                + " has an invalid provider item id.");
                        yield null;
                    }
                    yield new LuckyBlockTable.Reward(type, null, amounts.min(), amounts.max(),
                            chance, "", message, token);
                }
                final Material material = material(string(map.get("material")));
                if (material == null || isAir(material)) {
                    warnings.add("Lucky-block table '" + tableId + "' reward #" + index
                            + " has an invalid item material.");
                    yield null;
                }
                yield new LuckyBlockTable.Reward(type, material, amounts.min(), amounts.max(),
                        chance, "", message, null);
            }
            case COMMAND -> {
                final String command = string(map.get("command"));
                if (command == null || command.isBlank()) {
                    warnings.add("Lucky-block table '" + tableId + "' reward #" + index
                            + " has an empty command.");
                    yield null;
                }
                yield new LuckyBlockTable.Reward(type, null, 1, 1, chance, command, message, null);
            }
            case MESSAGE -> {
                if (message == null || message.isBlank()) {
                    warnings.add("Lucky-block table '" + tableId + "' reward #" + index
                            + " has an empty message.");
                    yield null;
                }
                yield new LuckyBlockTable.Reward(type, null, 1, 1, chance, "", message, null);
            }
            case NOTHING -> new LuckyBlockTable.Reward(type, null, 1, 1, chance, "", message, null);
        };
    }

    private static AmountRange amountRange(final Object raw, final String tableId, final int index,
                                           final List<String> warnings) {
        if (raw == null) {
            return new AmountRange(1, 1);
        }
        if (raw instanceof Number number) {
            final int value = clampAmount(number.intValue());
            return new AmountRange(value, value);
        }
        final String text = String.valueOf(raw).trim();
        if (text.contains("-")) {
            final String[] parts = text.split("-", 2);
            try {
                final int first = clampAmount(Integer.parseInt(parts[0].trim()));
                final int second = clampAmount(Integer.parseInt(parts[1].trim()));
                return new AmountRange(Math.min(first, second), Math.max(first, second));
            } catch (NumberFormatException ex) {
                warnings.add("Lucky-block table '" + tableId + "' reward #" + index
                        + " has an invalid amount range.");
                return new AmountRange(1, 1);
            }
        }
        try {
            final int value = clampAmount(Integer.parseInt(text));
            return new AmountRange(value, value);
        } catch (NumberFormatException ex) {
            warnings.add("Lucky-block table '" + tableId + "' reward #" + index
                    + " has an invalid amount.");
            return new AmountRange(1, 1);
        }
    }

    private static int clampAmount(final int value) {
        return Math.max(1, Math.min(64, value));
    }

    private static Double number(final Object raw) {
        if (raw instanceof Number number) {
            final double value = number.doubleValue();
            return Double.isFinite(value) ? value : null;
        }
        try {
            final double value = Double.parseDouble(String.valueOf(raw).trim());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String normalizeBlockKey(final String raw) {
        final String token = CustomBlockTokens.normalize(raw);
        if (token != null) {
            return token;
        }
        final Material material = material(raw);
        return material == null ? null : material.name();
    }

    private static Material material(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.trim().toUpperCase(Locale.ROOT);
        if (name.startsWith("MINECRAFT:")) {
            name = name.substring("MINECRAFT:".length());
        }
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isAir(final Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR
                || material == Material.VOID_AIR;
    }

    private static String string(final Object raw) {
        return raw == null ? null : String.valueOf(raw).trim();
    }

    public record Result(Map<String, LuckyBlockTable> tables,
                         Map<String, MineLuckyBlocks> mines,
                         List<String> warnings,
                         LuckyBlockFeedback feedback) {
        public static final Result EMPTY =
                new Result(Map.of(), Map.of(), List.of(), LuckyBlockFeedback.NONE);
    }

    private record AmountRange(int min, int max) {
    }
}
