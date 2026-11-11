package me.marcdoesntexist.galaxymines.bomb;

import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.mines.MineManager;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;





public final class BombListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final ItemStack item = event.getItem();
        if (!BombItem.isBomb(item)) {
            return;
        }
        event.setCancelled(true);
        final Player player = event.getPlayer();
        if (!BombService.get().isEnabled()) {
            player.sendMessage(Text.parse("<red>Bombs are disabled on this server."));
            return;
        }
        if (!player.hasPermission("galaxymines.bomb")) {
            player.sendMessage(Text.parse(
                    "<red>You do not have permission to use bombs."));
            return;
        }
        final Block clicked = event.getClickedBlock();
        if (clicked == null || MineManager.getMineAt(clicked) == null) {
            player.sendMessage(Text.parse("<red>Bombs can only be used inside a mine."));
            return;
        }
        final long remaining = BombService.get().cooldownRemaining(player);
        if (remaining > 0L) {
            player.sendMessage(Text.parse("<red>Wait <white>" + remaining
                    + "<red>s before using another bomb."));
            return;
        }
        final int radius = BombItem.radius(item);
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
        final int broken = BombService.get().explode(player,
                clicked.getLocation().add(0.5D, 0.5D, 0.5D), radius);
        player.sendMessage(Text.parse("<green>Bomb detonated: <white>" + broken
                + " <green>blocks."));
    }
}
