package me.marcdoesntexist.galaxymines.integration.economy;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;


public final class PriceEditorService {

    public static final double MAX_PRICE = 1_000_000_000.0D;

    private static final Pattern PROVIDER = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

    private PriceEditorService() {
    }

    public static String validateKey(final String raw) {
        final String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty() || trimmed.chars().anyMatch(Character::isWhitespace)) {
            return null;
        }
        if (trimmed.indexOf(':') >= 0) {
            final String normalized = trimmed.toLowerCase(Locale.ROOT);
            return PROVIDER.matcher(normalized).matches() ? normalized : null;
        }
        final String normalized = trimmed.toUpperCase(Locale.ROOT);
        return MaterialCatalog.isKnown(normalized) ? normalized : null;
    }

    public static Double validatePrice(final String raw) {
        final String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        final double value;
        try {
            value = Double.parseDouble(trimmed);
        } catch (NumberFormatException exception) {
            return null;
        }
        if (!Double.isFinite(value) || value <= 0.0D || value > MAX_PRICE) {
            return null;
        }
        return value;
    }

    public static Outcome set(final File file, final String key, final String rawPrice) {
        final String normalized = validateKey(key);
        if (normalized == null) {
            return new Outcome(Result.INVALID_KEY, key, 0.0D);
        }
        final Double price = validatePrice(rawPrice);
        if (price == null) {
            return new Outcome(Result.INVALID_PRICE, normalized, 0.0D);
        }
        try {
            if (!PriceFileEditor.set(file, normalized, price)) {
                return new Outcome(Result.WRITE_FAILED, normalized, price);
            }
        } catch (IOException exception) {
            return new Outcome(Result.WRITE_FAILED, normalized, price);
        }
        return new Outcome(Result.OK, normalized, price);
    }

    public static Outcome remove(final File file, final String key) {
        final String normalized = validateKey(key);
        if (normalized == null) {
            return new Outcome(Result.INVALID_KEY, key, 0.0D);
        }
        try {
            if (!PriceFileEditor.remove(file, normalized)) {
                return new Outcome(Result.NOT_FOUND, normalized, 0.0D);
            }
        } catch (IOException exception) {
            return new Outcome(Result.WRITE_FAILED, normalized, 0.0D);
        }
        return new Outcome(Result.OK, normalized, 0.0D);
    }

    public static boolean isRemoval(final String raw) {
        final String value = raw == null ? "" : raw.trim();
        return value.equalsIgnoreCase("remove") || value.equalsIgnoreCase("delete")
                || value.equalsIgnoreCase("rimuovi");
    }

    public enum Result {
        OK, INVALID_KEY, INVALID_PRICE, NOT_FOUND, WRITE_FAILED
    }

    public record Outcome(Result result, String key, double value) {
        public boolean ok() {
            return this.result == Result.OK;
        }
    }
}
