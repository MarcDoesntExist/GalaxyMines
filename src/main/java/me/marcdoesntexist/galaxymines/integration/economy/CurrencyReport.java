package me.marcdoesntexist.galaxymines.integration.economy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;








public final class CurrencyReport {
    private CurrencyReport() {
    }



    public static List<Entry> snapshot() {
        final List<Entry> entries = new ArrayList<>();
        for (final Currency currency : CurrencyRegistry.all()) {
            if (currency == null || currency.id() == null) {
                continue;
            }
            entries.add(new Entry(currency.id(), currency.displayName(),
                    currency.available(), currency.id().equals(CurrencyRegistry.defaultId())));
        }
        entries.sort(Comparator.comparing(Entry::id));
        return entries;
    }

    public static String state(final Entry entry) {
        return entry.available() ? "available" : "unavailable";
    }

    public record Entry(String id, String displayName, boolean available, boolean isDefault) {
    }
}
