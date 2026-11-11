package me.marcdoesntexist.galaxymines.integration.customblocks;

import java.util.Locale;
import java.util.Set;


public final class CustomBlockTokens {
    public static final String NEXO = "nexo";
    public static final String ITEMSADDER = "itemsadder";
    public static final String ORAXEN = "oraxen";
    private static final Set<String> PROVIDERS = Set.of(NEXO, ITEMSADDER, ORAXEN);

    private CustomBlockTokens() {
    }


    public static String normalize(final String input) {
        if (input == null) {
            return null;
        }
        final String trimmed = input.trim();
        final int separator = trimmed.indexOf(':');
        if (separator <= 0 || separator == trimmed.length() - 1) {
            return null;
        }
        final String namespace = trimmed.substring(0, separator).trim().toLowerCase(Locale.ROOT);
        if (!PROVIDERS.contains(namespace)) {
            return null;
        }
        final String id = trimmed.substring(separator + 1).trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty() || id.indexOf('=') >= 0 || id.chars().anyMatch(Character::isWhitespace)) {
            return null;
        }
        return namespace + ":" + id;
    }


    public static String qualify(final String provider, final String rawId) {
        if (!isProvider(provider) || rawId == null) {
            return null;
        }
        final String providerKey = provider.trim().toLowerCase(Locale.ROOT);
        final String existing = normalize(rawId);
        if (existing != null) {
            return providerKey.equals(namespace(existing)) ? existing : null;
        }
        return normalize(providerKey + ":" + rawId.trim());
    }

    public static boolean isToken(final String input) {
        return normalize(input) != null;
    }

    public static boolean isProvider(final String namespace) {
        return namespace != null && PROVIDERS.contains(namespace.toLowerCase(Locale.ROOT));
    }

    public static String namespace(final String token) {
        final String normalized = normalize(token);
        if (normalized == null) {
            return null;
        }
        return normalized.substring(0, normalized.indexOf(':'));
    }

    public static String id(final String token) {
        final String normalized = normalize(token);
        if (normalized == null) {
            return null;
        }
        return normalized.substring(normalized.indexOf(':') + 1);
    }
}
