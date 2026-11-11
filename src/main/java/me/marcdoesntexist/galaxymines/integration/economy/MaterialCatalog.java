package me.marcdoesntexist.galaxymines.integration.economy;

import org.bukkit.Material;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;













public final class MaterialCatalog {
    public static final String RESOURCE = "/materials.txt";

    private static volatile Set<String> names = load();

    private MaterialCatalog() {
    }

    



    public static boolean isKnown(final String raw) {
        final String key = normalize(raw);
        if (key.isEmpty()) {
            return false;
        }
        return names.contains(key) || Material.matchMaterial(key) != null;
    }

    public static int size() {
        return names.size();
    }

    
    public static Set<String> all() {
        return names;
    }

    static String normalize(final String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private static Set<String> load() {
        final InputStream stream = MaterialCatalog.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            return Set.of();
        }
        final Set<String> parsed = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) {
                    continue;
                }
                parsed.add(value.toUpperCase(Locale.ROOT));
            }
        } catch (IOException exception) {
            return Set.of();
        }
        return Collections.unmodifiableSet(parsed);
    }
}
