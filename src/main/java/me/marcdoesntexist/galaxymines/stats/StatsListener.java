package me.marcdoesntexist.galaxymines.stats;

import me.marcdoesntexist.galaxymines.quest.QuestService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class StatsListener implements Listener {
    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        PlayerStatsManager.loadAsync(event.getPlayer());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        PlayerStatsManager.flush(event.getPlayer(), true);
        QuestService.get().save();
    }
}
