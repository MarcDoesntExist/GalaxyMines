package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Locale;





public final class MineFeedback {
    private MineFeedback() {
    }

    public static void play(final Player player, final String sound, final String particle) {
        playSound(player, sound);
        spawnParticle(player, particle);
    }

    public static void playAll(final Collection<? extends Player> players, final String sound,
                               final String particle) {
        for (final Player player : players) {
            play(player, sound, particle);
        }
    }

    private static void playSound(final Player player, final String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            player.playSound(player.getLocation(), raw.trim(), 0.8F, 1.0F);
        } catch (IllegalArgumentException | NullPointerException ex) {
            GalaxyMines.getInstance().debug("Unknown feedback sound '" + raw + "': "
                    + ex.getMessage(), true);
        }
    }

    private static void spawnParticle(final Player player, final String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            final String name = raw.trim().toUpperCase(Locale.ROOT)
                    .replace("MINECRAFT:", "").replace('-', '_');
            final Particle particle = Particle.valueOf(name);
            final Location location = player.getLocation().add(0.0D, 1.0D, 0.0D);
            player.spawnParticle(particle, location, 12, 0.35D, 0.5D, 0.35D, 0.01D);
        } catch (IllegalArgumentException ex) {
            GalaxyMines.getInstance().debug("Unknown feedback particle '" + raw + "': "
                    + ex.getMessage(), true);
        }
    }
}
