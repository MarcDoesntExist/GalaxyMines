package me.marcdoesntexist.galaxymines.integration.customblocks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public final class CustomBlockRegistry {
    private static final Map<String, CustomBlockProvider> PROVIDERS = new LinkedHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static Plugin plugin;

    private CustomBlockRegistry() {
    }

    public static void load(final Plugin owner) {
        unload();
        plugin = owner;
        register(new NexoCustomBlockProvider(owner));
        register(new ItemsAdderCustomBlockProvider(owner));
        register(new OraxenCustomBlockProvider(owner));
        ProviderVersions.verify(owner);
        if (!PROVIDERS.isEmpty()) {
            owner.getLogger().info("Custom block providers enabled: "
                    + String.join(", ", PROVIDERS.keySet()));
        }
    }

    public static void unload() {
        PROVIDERS.clear();
        WARNED.clear();
        plugin = null;
    }


    static void register(final CustomBlockProvider provider) {
        if (provider.available()) {
            PROVIDERS.put(provider.namespace(), provider);
        }
    }

    public static boolean isAvailable(final String namespace) {
        return namespace != null
                && PROVIDERS.containsKey(namespace.toLowerCase(Locale.ROOT));
    }

    public static boolean place(final String token, final Location location) {
        final String normalized = CustomBlockTokens.normalize(token);
        if (normalized == null || location == null) {
            return false;
        }
        final CustomBlockProvider provider =
                PROVIDERS.get(CustomBlockTokens.namespace(normalized));
        if (provider == null) {
            warnOnce(normalized, "provider not installed");
            return false;
        }
        try {
            if (provider.place(CustomBlockTokens.id(normalized), location)) {
                return true;
            }
            warnOnce(normalized, "placement failed");
        } catch (final Throwable t) {
            warnOnce(normalized, "placement error: " + t.getClass().getSimpleName());
        }
        return false;
    }


    public static ItemStack item(final String token) {
        final String normalized = CustomBlockTokens.normalize(token);
        if (normalized == null) {
            return null;
        }
        final CustomBlockProvider provider =
                PROVIDERS.get(CustomBlockTokens.namespace(normalized));
        if (provider == null) {
            return null;
        }
        try {
            final ItemStack item = provider.item(CustomBlockTokens.id(normalized));
            return item == null || item.getType().isAir() ? null : item;
        } catch (final Throwable t) {
            warnOnce(normalized, "item lookup error: " + t.getClass().getSimpleName());
            return null;
        }
    }


    public static String itemId(final ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        for (final CustomBlockProvider provider : PROVIDERS.values()) {
            try {
                final String raw = provider.itemId(item);
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                final String token = CustomBlockTokens.qualify(provider.namespace(), raw);
                if (token != null) {
                    return token;
                }
            } catch (final Throwable t) {
                warnOnce(provider.namespace(),
                        "item id lookup error: " + t.getClass().getSimpleName());
            }
        }
        return null;
    }

    public static boolean matches(final Block block, final String token) {
        final String normalized = CustomBlockTokens.normalize(token);
        if (block == null || normalized == null) {
            return false;
        }
        final CustomBlockProvider provider =
                PROVIDERS.get(CustomBlockTokens.namespace(normalized));
        if (provider == null) {
            return false;
        }
        try {
            return provider.matches(block, CustomBlockTokens.id(normalized));
        } catch (final Throwable t) {
            warnOnce(normalized, "block match error: " + t.getClass().getSimpleName());
            return false;
        }
    }

    private static void warnOnce(final String token, final String reason) {
        if (plugin == null || !WARNED.add(token + "|" + reason)) {
            return;
        }
        plugin.getLogger().warning("Custom provider entry " + token + ": " + reason
                + ". The vanilla fallback or material price will be used.");
    }
}
