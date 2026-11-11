package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.api.utils.StringUtils;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.events.player.PlayerEnterMineEvent;
import me.marcdoesntexist.galaxymines.events.player.PlayerExitMineEvent;
import me.marcdoesntexist.galaxymines.messages.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;





public final class MineBossBarManager implements Listener {
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private BukkitTask updateTask;

    private static BossBar.Color color(final String configured) {
        try {
            return BossBar.Color.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BossBar.Color.BLUE;
        }
    }

    private static BossBar.Overlay overlay(final String configured) {
        try {
            return BossBar.Overlay.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    public void start() {
        this.stop();
        this.updateTask = Bukkit.getScheduler().runTaskTimer(GalaxyMines.getInstance(),
                this::updateBars, 20L, 20L);
    }

    public void stop() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
            this.updateTask = null;
        }
        for (final var entry : this.bars.entrySet()) {
            final Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.hideBossBar(entry.getValue());
            }
        }
        this.bars.clear();
    }

    @EventHandler
    public void onEnter(final PlayerEnterMineEvent event) {
        if (!PluginSettings.get().bossBars.enabled || !event.getMine().isBossBarEnabled()) {
            return;
        }
        this.show(event.getPlayer(), event.getMine());
    }

    @EventHandler
    public void onExit(final PlayerExitMineEvent event) {
        this.hide(event.getPlayer());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        this.hide(event.getPlayer());
    }

    private void show(final Player player, final Mine mine) {
        this.hide(player);
        final BossBar bar = BossBar.bossBar(this.title(mine), this.progress(mine),
                color(PluginSettings.get().bossBars.color),
                overlay(PluginSettings.get().bossBars.style));
        this.bars.put(player.getUniqueId(), bar);
        player.showBossBar(bar);
    }

    private void hide(final Player player) {
        final BossBar bar = this.bars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private void updateBars() {
        if (!PluginSettings.get().bossBars.enabled) {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                this.hide(player);
            }
            return;
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final Mine mine = MineTracker.current(player);
            if (mine == null || !mine.isBossBarEnabled()) {
                this.hide(player);
                continue;
            }
            final BossBar bar = this.bars.get(player.getUniqueId());
            if (bar == null) {
                this.show(player, mine);
                continue;
            }
            bar.name(this.title(mine));
            bar.progress(this.progress(mine));
        }
    }

    private Component title(final Mine mine) {
        final String rendered = Messages.get("messages.bossbar",
                "%mine% - %percent%%% - reset in %resettime%",
                "%mine%", mine.getName(),
                "%percent%", String.valueOf(mine.getPercentageRemaining()),
                "%resettime%", StringUtils.secondsToString(mine.getTimeToNextReset()));
        return Text.parse(rendered);
    }

    private float progress(final Mine mine) {
        if (mine.getVolume() <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F,
                mine.getRemainingBlocks() / (float) mine.getVolume()));
    }
}
