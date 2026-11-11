package me.marcdoesntexist.galaxymines.stats;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;


public enum StatsPeriod {
    DAILY,
    WEEKLY,
    ALL_TIME;

    public String key(final LocalDate date) {
        return switch (this) {
            case DAILY -> date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            case WEEKLY -> {
                final WeekFields fields = WeekFields.ISO;
                final int week = date.get(fields.weekOfWeekBasedYear());
                final int year = date.get(fields.weekBasedYear());
                yield year + "-W" + String.format(Locale.ROOT, "%02d", week);
            }
            case ALL_TIME -> "ALL";
        };
    }
}
