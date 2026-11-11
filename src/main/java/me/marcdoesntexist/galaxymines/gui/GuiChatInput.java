package me.marcdoesntexist.galaxymines.gui;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.messages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;









public final class GuiChatInput implements Listener {
    private static final long TIMEOUT_MILLIS = 60_000L;
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
    private static boolean registered;

    private GuiChatInput() {
    }



    public static void register(final GalaxyMines plugin) {
        if (GuiChatInput.registered || plugin == null) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(new GuiChatInput(), plugin);
        GuiChatInput.registered = true;
    }

    public static void request(final Player player, final String promptKey, final String fallback,
                               final Consumer<String> consumer) {
        if (player == null || consumer == null) {
            return;
        }
        PENDING.put(player.getUniqueId(),
                new Pending(System.currentTimeMillis() + TIMEOUT_MILLIS, consumer));
        player.sendMessage(Messages.get(promptKey, fallback,
                "%seconds%", Long.toString(TIMEOUT_MILLIS / 1000L)));
    }

    public static boolean hasPending(final Player player) {
        return player != null && PENDING.containsKey(player.getUniqueId());
    }

    public static void cancel(final Player player) {
        if (player != null) {
            PENDING.remove(player.getUniqueId());
        }
    }




    public static boolean submit(final Player player, final String raw) {
        if (player == null) {
            return false;
        }
        final Pending pending = PENDING.remove(player.getUniqueId());
        if (pending == null) {
            return false;
        }
        if (pending.expiresAt() < System.currentTimeMillis()) {
            player.sendMessage(Messages.get("messages.gui.input.timeout",
                    "<red>That input expired, try again."));
            return true;
        }
        final String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.equalsIgnoreCase("cancel")
                || value.equalsIgnoreCase("annulla")) {
            player.sendMessage(Messages.get("messages.gui.input.cancelled",
                    "<gray>Input cancelled."));
            return true;
        }
        pending.consumer().accept(value);
        return true;
    }



    public static void clearAll() {
        PENDING.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(final AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if (!hasPending(player)) {
            return;
        }
        event.setCancelled(true);
        final String message = event.getMessage();
        final GalaxyMines plugin = GalaxyMines.getInstance();
        if (plugin == null) {
            submit(player, message);
            return;
        }
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> submit(player, message));
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        PENDING.remove(event.getPlayer().getUniqueId());
    }

    private record Pending(long expiresAt, Consumer<String> consumer) {
    }
}
