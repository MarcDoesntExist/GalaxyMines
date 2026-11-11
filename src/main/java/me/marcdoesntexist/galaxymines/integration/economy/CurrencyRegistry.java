package me.marcdoesntexist.galaxymines.integration.economy;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class CurrencyRegistry {
    private static final Map<String, Currency> CURRENCIES = new ConcurrentHashMap<>();
    private static volatile String defaultId = VaultCurrency.ID;

    static {
        installBuiltIns();
    }

    private CurrencyRegistry() {
    }


    public static void reset() {
        CURRENCIES.clear();
        defaultId = VaultCurrency.ID;
        installBuiltIns();
    }

    private static void installBuiltIns() {
        CURRENCIES.put(VaultCurrency.ID, new VaultCurrency());
    }


    public static boolean register(final Currency currency) {
        if (currency == null || currency.id() == null || currency.id().isBlank()) {
            return false;
        }
        final String id = normalize(currency.id());
        if (id.isEmpty() || id.equals(VaultCurrency.ID)) {
            return false;
        }
        final Currency normalized = new Currency() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String displayName() {
                return currency.displayName();
            }

            @Override
            public boolean available() {
                return currency.available();
            }

            @Override
            public boolean deposit(final Player player, final double amount) {
                return currency.deposit(player, amount);
            }

            @Override
            public String format(final double amount) {
                return currency.format(amount);
            }
        };
        return CURRENCIES.putIfAbsent(id, normalized) == null;
    }


    public static boolean unregister(final String id) {
        if (id == null || normalize(id).equals(VaultCurrency.ID)) {
            return false;
        }
        return CURRENCIES.remove(normalize(id)) != null;
    }

    public static Currency get(final String id) {
        return id == null ? null : CURRENCIES.get(normalize(id));
    }


    public static Currency resolve(final String id) {
        final Currency currency = get(id);
        return currency == null ? defaultCurrency() : currency;
    }

    public static Currency defaultCurrency() {
        return CURRENCIES.get(defaultId);
    }

    public static String defaultId() {
        return defaultId;
    }


    public static boolean setDefault(final String id) {
        if (get(id) == null) {
            return false;
        }
        defaultId = normalize(id);
        return true;
    }

    public static boolean available(final String id) {
        final Currency currency = get(id);
        return currency != null && currency.available();
    }

    public static List<Currency> all() {
        return List.copyOf(CURRENCIES.values());
    }

    public static List<String> ids() {
        return new ArrayList<>(CURRENCIES.keySet());
    }


    public static String format(final String id, final double amount) {
        Currency currency = get(id);
        if (currency == null) {
            currency = defaultCurrency();
        }
        if (currency == null) {
            return String.format(Locale.ROOT, "%.2f", amount);
        }
        return currency.format(amount);
    }

    private static String normalize(final String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
