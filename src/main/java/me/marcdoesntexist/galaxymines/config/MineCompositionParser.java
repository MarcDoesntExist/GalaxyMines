package me.marcdoesntexist.galaxymines.config;

import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockTokens;

import java.util.ArrayList;
import java.util.List;





public final class MineCompositionParser {
    private MineCompositionParser() {
    }




    public static String format(final String materialName, final double percentage) {
        return materialName + "=" + percentage;
    }

    public static Result parse(final List<String> lines) {
        final List<Entry> entries = new ArrayList<>();
        final List<String> invalid = new ArrayList<>();
        if (lines == null) {
            return new Result(entries, invalid);
        }
        for (final String line : lines) {
            if (line == null) {
                invalid.add("null");
                continue;
            }
            final String[] split = line.split("=", 2);
            if (split.length < 2) {
                invalid.add(line);
                continue;
            }
            final String key = split[0].trim();
            if (key.isEmpty()) {
                invalid.add(line);
                continue;
            }
            final double percentage;
            try {
                percentage = Double.parseDouble(split[1].trim());
            } catch (final NumberFormatException ex) {
                invalid.add(line);
                continue;
            }
            if (!Double.isFinite(percentage) || percentage < 0.0D) {
                invalid.add(line);
                continue;
            }
            final String customToken = CustomBlockTokens.normalize(key);
            if (customToken != null) {
                entries.add(new Entry(line, customToken, null, 0, percentage));
                continue;
            }
            final String[] materialAndData = key.split(":", 2);
            int data = 0;
            if (materialAndData.length > 1) {
                try {
                    data = Integer.parseInt(materialAndData[1].trim());
                } catch (final NumberFormatException ignored) {
                    data = 0;
                }
            }
            entries.add(new Entry(line, null, materialAndData[0].trim(), data, percentage));
        }
        return new Result(entries, invalid);
    }

    public record Entry(String raw, String customToken, String materialName, int data,
                        double percentage) {
    }

    public record Result(List<Entry> entries, List<String> invalidEntries) {
    }
}
