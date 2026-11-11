package me.marcdoesntexist.galaxymines.integration.economy;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public final class PriceFileEditor {
    private static final String SECTION = "prices:";

    private PriceFileEditor() {
    }


    public static boolean set(final File file, final String key, final double value)
            throws IOException {
        if (file == null || key == null || key.isBlank()
                || !Double.isFinite(value) || value <= 0.0D) {
            return false;
        }
        final String normalized = normalize(key);
        if (normalized.isEmpty()) {
            return false;
        }
        final List<String> lines = file.isFile()
                ? new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8))
                : new ArrayList<>(List.of(SECTION));
        final String entry = "  " + normalized + ": " + format(value);
        final int section = findSection(lines);
        if (section < 0) {
            lines.add(SECTION);
            lines.add(entry);
            return write(file, lines);
        }
        for (int index = section + 1; index < lines.size(); ++index) {
            if (endsSection(lines.get(index))) {
                lines.add(index, entry);
                return write(file, lines);
            }
            if (matches(lines.get(index), normalized)) {
                lines.set(index, entry);
                return write(file, lines);
            }
        }
        lines.add(entry);
        return write(file, lines);
    }


    public static boolean remove(final File file, final String key) throws IOException {
        if (file == null || key == null || key.isBlank() || !file.isFile()) {
            return false;
        }
        final String normalized = normalize(key);
        final List<String> lines = new ArrayList<>(
                Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        final int section = findSection(lines);
        if (section < 0) {
            return false;
        }
        for (int index = section + 1; index < lines.size(); ++index) {
            if (endsSection(lines.get(index))) {
                return false;
            }
            if (matches(lines.get(index), normalized)) {
                lines.remove(index);
                return write(file, lines);
            }
        }
        return false;
    }


    public static String normalize(final String key) {
        final String trimmed = key == null ? "" : key.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.indexOf(':') >= 0) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private static String format(final double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static boolean write(final File file, final List<String> lines) throws IOException {
        final File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            return false;
        }
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        return true;
    }

    private static int findSection(final List<String> lines) {
        for (int index = 0; index < lines.size(); ++index) {
            if (SECTION.equals(lines.get(index).trim()) && !lines.get(index).startsWith(" ")) {
                return index;
            }
        }
        return -1;
    }

    private static boolean endsSection(final String line) {
        if (line.isBlank() || line.trim().startsWith("#")) {
            return false;
        }
        return !line.startsWith(" ");
    }

    private static boolean matches(final String line, final String normalized) {
        final String trimmed = line.trim();
        final String prefix = normalized + ":";
        if (trimmed.length() < prefix.length()
                || !trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return false;
        }
        return trimmed.length() == prefix.length()
                || Character.isWhitespace(trimmed.charAt(prefix.length()));
    }
}
