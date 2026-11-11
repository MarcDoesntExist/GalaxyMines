package me.marcdoesntexist.galaxymines.utils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


public final class ScheduleTimes {
    private ScheduleTimes() {
    }

    public static List<LocalTime> parse(final List<String> entries,
                                        final java.util.function.Consumer<String> invalid) {
        final List<LocalTime> parsed = new ArrayList<>();
        if (entries == null) {
            return parsed;
        }
        for (final String entry : entries) {
            if (entry == null) {
                continue;
            }
            final String[] parts = entry.trim().split(":", 2);
            if (parts.length != 2) {
                invalid.accept(entry);
                continue;
            }
            try {
                final int hour = Integer.parseInt(parts[0]);
                final int minute = Integer.parseInt(parts[1]);
                if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                    invalid.accept(entry);
                    continue;
                }
                parsed.add(LocalTime.of(hour, minute));
            } catch (NumberFormatException ex) {
                invalid.accept(entry);
            }
        }
        return parsed;
    }

    public static boolean matches(final List<LocalTime> times, final LocalTime now) {
        for (final LocalTime time : times) {
            if (time.getHour() == now.getHour() && time.getMinute() == now.getMinute()) {
                return true;
            }
        }
        return false;
    }
}
