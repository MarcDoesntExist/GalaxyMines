package me.marcdoesntexist.galaxymines.multiplier;

import java.util.Locale;





public final class BoosterParser {
    private BoosterParser() {
    }



    public static int durationSeconds(final String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        final String value = raw.trim().toLowerCase(Locale.ROOT);
        final char last = value.charAt(value.length() - 1);
        long factor = 1L;
        String number = value;
        if (last == 's') {
            number = value.substring(0, value.length() - 1);
        } else if (last == 'm') {
            factor = 60L;
            number = value.substring(0, value.length() - 1);
        } else if (last == 'h') {
            factor = 3600L;
            number = value.substring(0, value.length() - 1);
        } else if (last == 'd') {
            factor = 86400L;
            number = value.substring(0, value.length() - 1);
        } else if (last == 'w') {
            factor = 604800L;
            number = value.substring(0, value.length() - 1);
        }
        try {
            final long amount = Long.parseLong(number);
            if (amount <= 0L) {
                return -1;
            }
            final long seconds = amount * factor;
            return seconds > Integer.MAX_VALUE ? -1 : (int) seconds;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }



    public static double multiplier(final String raw) {
        if (raw == null || raw.isBlank()) {
            return -1.0D;
        }
        try {
            final double value = Double.parseDouble(raw.trim());
            return Double.isFinite(value) && value > 1.0D ? value : -1.0D;
        } catch (NumberFormatException exception) {
            return -1.0D;
        }
    }

    public static String formatDuration(final int seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        final StringBuilder builder = new StringBuilder();
        int remaining = seconds;
        final int days = remaining / 86400;
        remaining -= days * 86400;
        final int hours = remaining / 3600;
        remaining -= hours * 3600;
        final int minutes = remaining / 60;
        remaining -= minutes * 60;
        if (days > 0) {
            builder.append(days).append('d');
        }
        if (hours > 0) {
            builder.append(builder.isEmpty() ? "" : " ").append(hours).append('h');
        }
        if (minutes > 0) {
            builder.append(builder.isEmpty() ? "" : " ").append(minutes).append('m');
        }
        if (remaining > 0 && days == 0) {
            builder.append(builder.isEmpty() ? "" : " ").append(remaining).append('s');
        }
        return builder.isEmpty() ? "0s" : builder.toString();
    }
}
