package me.marcdoesntexist.galaxymines.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Potions {

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        ALIASES.put("CONFUSION", "NAUSEA");
        ALIASES.put("DAMAGE_RESISTANCE", "RESISTANCE");
        ALIASES.put("FAST_DIGGING", "HASTE");
        ALIASES.put("HARM", "INSTANT_DAMAGE");
        ALIASES.put("HEAL", "INSTANT_HEALTH");
        ALIASES.put("INCREASE_DAMAGE", "STRENGTH");
        ALIASES.put("JUMP", "JUMP_BOOST");
        ALIASES.put("SLOW", "SLOWNESS");
        ALIASES.put("SLOW_DIGGING", "MINING_FATIGUE");
    }

    private Potions() {
    }

    public static PotionEffectType get(final String rawName) {
        final String name = canonicalName(rawName);
        return name == null ? null : byName(name);
    }




    static String canonicalName(final String rawName) {
        if (rawName == null) {
            return null;
        }
        String name = rawName.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (name.isEmpty()) {
            return null;
        }
        if (name.startsWith("MINECRAFT:")) {
            name = name.substring("MINECRAFT:".length());
        }
        return ALIASES.getOrDefault(name, name);
    }

    public static String name(final PotionEffectType type) {
        if (type == null) {
            return "SPEED";
        }
        return type.getKey().getKey().toUpperCase(Locale.ROOT);
    }

    private static PotionEffectType byName(final String name) {
        try {
            final PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(
                    NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
            if (type != null) {
                return type;
            }
        } catch (final Throwable ignored) {
        }
        try {
            return PotionEffectType.getByName(name);
        } catch (final Throwable ignored) {
            return null;
        }
    }
}