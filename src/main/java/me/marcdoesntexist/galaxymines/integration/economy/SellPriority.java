package me.marcdoesntexist.galaxymines.integration.economy;

import java.util.Locale;









public enum SellPriority {
    PROVIDER_FIRST,
    MATERIAL_FIRST,
    HIGHEST,


    GLOBAL;




    public static SellPriority parseForMine(final String raw) {
        final SellPriority parsed = parseOrNull(raw);
        return parsed == null ? GLOBAL : parsed;
    }




    public static SellPriority parseGlobal(final String raw) {
        final SellPriority parsed = parseOrNull(raw);
        return parsed == null || parsed == GLOBAL ? PROVIDER_FIRST : parsed;
    }



    public static SellPriority parseOrNull(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final String value = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        return switch (value) {
            case "GLOBAL", "DEFAULT", "AUTO", "INHERIT" -> GLOBAL;
            case "PROVIDER", "PROVIDER_FIRST", "CUSTOM", "CUSTOM_FIRST" -> PROVIDER_FIRST;
            case "MATERIAL", "MATERIAL_FIRST", "BASE", "BASE_FIRST" -> MATERIAL_FIRST;
            case "HIGHEST", "MAX", "BEST" -> HIGHEST;
            default -> null;
        };
    }







    public double select(final double providerPrice, final boolean providerResolved,
                         final double materialPrice) {
        final double provider = providerResolved ? Math.max(0.0D, providerPrice) : 0.0D;
        final double material = Math.max(0.0D, materialPrice);
        return switch (this) {
            case MATERIAL_FIRST -> material > 0.0D ? material
                    : (providerResolved ? provider : 0.0D);
            case HIGHEST -> Math.max(provider, material);
            default -> providerResolved ? provider : material;
        };
    }
}
