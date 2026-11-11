package me.marcdoesntexist.galaxymines.api.utils;

import me.marcdoesntexist.galaxymines.compat.Potions;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PotionUtils {


    public static PotionEffect parsePotionEffect(final String input) throws PotionParseException {
        final PotionSpec spec = parseSpec(input);
        final PotionEffectType type = Potions.get(spec.effectName());
        if (type == null) {
            throw new PotionParseException(input,
                    new IllegalArgumentException("Unknown potion effect: "
                            + spec.effectName()));
        }
        try {
            return new PotionEffect(type, spec.durationTicks(), spec.amplifier());
        } catch (final RuntimeException ex) {
            throw new PotionParseException(input, ex);
        }
    }


    static PotionSpec parseSpec(final String input) throws PotionParseException {
        try {
            final String[] config = input.split(":");
            final int duration = config.length > 1 ? Integer.parseInt(config[1].trim()) * 20 :
                    20 * 60;
            final int amplifier = config.length > 2 ? Integer.parseInt(config[2].trim()) : 0;
            return new PotionSpec(config[0], Math.max(1, duration), Math.max(0, amplifier));
        } catch (final Exception ex) {
            throw new PotionParseException(input, ex);
        }
    }

    public static PotionEffect random() {
        final List<PotionEffectType> types = new ArrayList<>();
        for (final PotionEffectType type : PotionEffectType.values()) {
            if (type != null) {
                types.add(type);
            }
        }
        if (types.isEmpty()) {
            return new PotionEffect(PotionEffectType.SPEED, 20 * 30, 0);
        }
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return new PotionEffect(types.get(random.nextInt(types.size())),
                20 * (random.nextInt(5) + 1), random.nextInt(5));
    }

    record PotionSpec(String effectName, int durationTicks, int amplifier) {
    }

    public static class PotionParseException extends Exception {
        public PotionParseException(final String input, final Exception e) {
            super("Unable to parse potion effect '" + input + "': " + e.getMessage(), e);
        }
    }
}
