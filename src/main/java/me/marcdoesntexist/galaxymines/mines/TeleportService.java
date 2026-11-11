package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.messages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;





public final class TeleportService {
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new ConcurrentHashMap<>();

    private TeleportService() {
    }

    public static void request(final Player player, final Mine mine) {
        final PluginSettings.Teleport settings = PluginSettings.get().teleport;
        final long now = System.currentTimeMillis();
        final Long until = COOLDOWN_UNTIL.get(player.getUniqueId());
        if (settings.cooldownSeconds > 0 && until != null && until > now) {
            player.sendMessage(Text.parse(Messages.get("messages.mine.teleportCooldown",
                    "Wait %seconds% seconds before teleporting again.",
                    "%seconds%", Long.toString(Math.max(1L, (until - now + 999L) / 1000L)))));
            return;
        }
        cancel(player, false);
        COOLDOWN_UNTIL.put(player.getUniqueId(), now + settings.cooldownSeconds * 1000L);
        if (settings.warmupSeconds <= 0) {
            teleport(player, mine);
            return;
        }
        player.sendMessage(Text.parse(Messages.get("messages.mine.teleportWarmup",
                "Teleporting to %mine% in %seconds% seconds. Do not move.",
                "%mine%", mine.getName(), "%seconds%",
                Integer.toString(settings.warmupSeconds))));
        final BukkitTask task = Bukkit.getScheduler().runTaskLater(GalaxyMines.getInstance(),
                () -> {
                    final Pending pending = PENDING.remove(player.getUniqueId());
                    if (pending != null && player.isOnline()) {
                        teleport(player, mine);
                    }
                }, settings.warmupSeconds * 20L);
        PENDING.put(player.getUniqueId(), new Pending(mine, player.getLocation().clone(), task));
    }

    private static void teleport(final Player player, final Mine mine) {
        final Location destination = mine.getSafeTeleportLocation();
        player.teleportAsync(destination).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                player.sendMessage(Text.parse(Messages.get("messages.mine.teleported",
                        "Teleported to the mine %mine%.", "%mine%", mine.getName())));
                return;
            }
            player.sendMessage(Text.parse(Messages.get("messages.mine.teleportFailed",
                    "The teleport to %mine% was cancelled.", "%mine%", mine.getName())));
        });
    }

    public static void handleMove(final Player player, final Location from, final Location to) {
        final Pending pending = PENDING.get(player.getUniqueId());
        if (pending == null || to == null) {
            return;
        }
        if (!PluginSettings.get().teleport.cancelOnMove) {
            return;
        }
        if (from.getWorld() != to.getWorld()
                || from.distanceSquared(to) > 0.04D) {
            cancel(player, true);
        }
    }

    public static void cancel(final Player player, final boolean notify) {
        final Pending pending = PENDING.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }
        pending.task().cancel();
        if (notify) {
            player.sendMessage(Text.parse(Messages.get("messages.mine.teleportCancelled",
                    "Mine teleport cancelled because you moved.")));
        }
    }

    public static void clear(final Player player) {
        cancel(player, false);
        COOLDOWN_UNTIL.remove(player.getUniqueId());
    }

    public static int pendingCount() {
        return PENDING.size();
    }

    public static void cancelAll() {
        for (final Pending pending : PENDING.values()) {
            pending.task().cancel();
        }
        PENDING.clear();
        COOLDOWN_UNTIL.clear();
    }

    private record Pending(Mine mine, Location start, BukkitTask task) {
    }
}
