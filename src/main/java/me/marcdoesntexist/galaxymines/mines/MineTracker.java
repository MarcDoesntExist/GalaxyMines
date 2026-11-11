package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.events.player.PlayerEnterMineEvent;
import me.marcdoesntexist.galaxymines.events.player.PlayerExitMineEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public final class MineTracker {
    private static final Map<UUID, Mine> CURRENT = new HashMap<>();

    private MineTracker() {
    }

    public static void update(final Player player, final Location to) {
        final Mine previous = CURRENT.get(player.getUniqueId());
        final Mine next = MineManager.getMineAt(to);
        if (previous == next) {
            return;
        }
        if (previous != null) {
            CURRENT.remove(player.getUniqueId());
            leave(player, previous);
        }
        if (next == null) {
            return;
        }
        if (!MineAccess.canAccess(player, next)) {
            MineAccess.denyAccess(player, next);
            return;
        }
        CURRENT.put(player.getUniqueId(), next);
        new PlayerEnterMineEvent(player, next).call();
        final PluginSettings.Feedback feedback = PluginSettings.get().feedback;
        MineFeedback.play(player, feedback.enterSound, feedback.enterParticle);
    }

    public static Mine current(final Player player) {
        return CURRENT.get(player.getUniqueId());
    }

    public static void refresh(final Player player) {
        update(player, player.getLocation());
    }

    public static void clear(final Player player) {
        final Mine previous = CURRENT.remove(player.getUniqueId());
        if (previous != null) {
            leave(player, previous);
        }
    }


    public static void clear(final Mine mine) {
        if (mine == null) {
            return;
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (CURRENT.get(player.getUniqueId()) == mine) {
                clear(player);
            }
        }
    }


    public static void clearWorld(final World world) {
        if (world == null) {
            return;
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final Mine mine = CURRENT.get(player.getUniqueId());
            if (mine != null && world.equals(mine.getWorld())) {
                clear(player);
            }
        }
    }

    public static void clearAll() {
        for (final Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            clear(player);
        }
        CURRENT.clear();
    }


    private static void leave(final Player player, final Mine mine) {
        if (mine.useEffects()) {
            for (final PotionEffect effect : mine.getEffects()) {
                player.removePotionEffect(effect.getType());
            }
        }
        new PlayerExitMineEvent(player, mine).call();
        final PluginSettings.Feedback feedback = PluginSettings.get().feedback;
        MineFeedback.play(player, feedback.exitSound, feedback.exitParticle);
    }
}
