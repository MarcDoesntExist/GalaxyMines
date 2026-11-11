package me.marcdoesntexist.galaxymines.integration.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.events.MineEventDefinition;
import me.marcdoesntexist.galaxymines.events.MineEventManager;
import me.marcdoesntexist.galaxymines.integration.economy.EconomyService;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.mines.MineManager;
import me.marcdoesntexist.galaxymines.multiplier.BoosterService;
import me.marcdoesntexist.galaxymines.multiplier.BoosterType;
import me.marcdoesntexist.galaxymines.stats.PlayerStats;
import me.marcdoesntexist.galaxymines.stats.PlayerStatsManager;
import me.marcdoesntexist.galaxymines.stats.StatsPeriod;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.Locale;


public final class GalaxyMinesPlaceholderExpansion extends PlaceholderExpansion {


    private static String eventPlaceholder(final String key) {
        if (!key.startsWith("event_")) {
            return null;
        }
        if (key.endsWith("_remaining")) {
            final String id = key.substring("event_".length(),
                    key.length() - "_remaining".length());
            return id.isBlank()
                    ? null : formatSeconds(MineEventManager.get().remainingSeconds(id));
        }
        if (key.endsWith("_name")) {
            final String id = key.substring("event_".length(), key.length() - "_name".length());
            if (id.isBlank()) {
                return null;
            }
            final MineEventDefinition definition = MineEventManager.get().definition(id);
            return definition == null ? "" : definition.displayName();
        }
        return null;
    }

    private static String mineName(final OfflinePlayer offlinePlayer) {
        final Mine mine = mine(offlinePlayer);
        return mine == null ? "" : mine.getName();
    }

    private static Mine mine(final OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return null;
        }
        final Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return null;
        }
        final Location location = player.getLocation();
        return MineManager.getMineAt(location);
    }

    private static String boosterMultiplier(final OfflinePlayer offlinePlayer,
                                            final BoosterType type) {
        if (offlinePlayer == null) {
            return "1.00";
        }
        return String.format(Locale.ROOT, "%.2f", BoosterService.get()
                .multiplier(offlinePlayer.getUniqueId(), type));
    }

    private static String boosterRemaining(final OfflinePlayer offlinePlayer,
                                           final BoosterType type) {
        if (offlinePlayer == null) {
            return "00:00";
        }
        return formatSeconds(BoosterService.get()
                .remainingSeconds(offlinePlayer.getUniqueId(), type));
    }

    private static String formatSeconds(final int seconds) {
        if (seconds <= 0) {
            return "00:00";
        }
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public String getIdentifier() {
        return "galaxymines";
    }

    @Override
    public String getAuthor() {
        return "MarcDoesntExists";
    }

    @Override
    public String getVersion() {
        return GalaxyMines.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(final OfflinePlayer offlinePlayer, final String params) {
        if (params == null) {
            return "";
        }
        final String key = params.toLowerCase(Locale.ROOT);
        final String eventValue = eventPlaceholder(key);
        if (eventValue != null) {
            return eventValue;
        }
        switch (key) {
            case "mine":
                return mineName(offlinePlayer);
            case "mine_alias": {
                final Mine mine = mine(offlinePlayer);
                return mine == null ? "" : mine.getAlias();
            }
            case "mine_remaining": {
                final Mine mine = mine(offlinePlayer);
                return mine == null ? "0" : Integer.toString(mine.getRemainingBlocks());
            }
            case "mine_percent": {
                final Mine mine = mine(offlinePlayer);
                return mine == null ? "0" : String.format(Locale.ROOT, "%.2f",
                        mine.getPercentageRemaining());
            }
            case "mine_reset": {
                final Mine mine = mine(offlinePlayer);
                return mine == null ? "" : formatSeconds(mine.getTimeToNextReset());
            }
            case "mines":
                return Integer.toString(MineManager.getMines().size());
            case "events":
                return String.join(",", MineEventManager.get().activeEvents());
            case "events_names":
                return String.join(", ", MineEventManager.get().activeDisplayNames());
            case "events_remaining":
                return formatSeconds(MineEventManager.get().secondsUntilNextExpiry());
            case "economy":
                return EconomyService.isAvailable() ? "yes" : "no";
            case "booster_sell":
                return boosterMultiplier(offlinePlayer, BoosterType.SELL);
            case "booster_drop":
                return boosterMultiplier(offlinePlayer, BoosterType.DROP);
            case "booster_sell_remaining":
                return boosterRemaining(offlinePlayer, BoosterType.SELL);
            case "booster_drop_remaining":
                return boosterRemaining(offlinePlayer, BoosterType.DROP);
            default:
                return statistics(offlinePlayer, key);
        }
    }

    private String statistics(final OfflinePlayer offlinePlayer, final String key) {
        if (offlinePlayer == null) {
            return "";
        }
        final PlayerStats stats = PlayerStatsManager.profile(offlinePlayer.getUniqueId());
        if (stats == null) {
            return key.startsWith("leaderboard") ? "" : "0";
        }
        final LocalDate today = LocalDate.now();
        return switch (key) {
            case "total_blocks" -> Long.toString(stats.totalBlocks());
            case "daily_blocks" -> Long.toString(stats.blocks(StatsPeriod.DAILY, today));
            case "weekly_blocks" -> Long.toString(stats.blocks(StatsPeriod.WEEKLY, today));
            case "total_sold" -> Long.toString(stats.totalSoldItems());
            case "total_earned" -> String.format(Locale.ROOT, "%.2f", stats.totalEarned());
            case "daily_earned" -> String.format(Locale.ROOT, "%.2f",
                    stats.earned(StatsPeriod.DAILY, today));
            case "weekly_earned" -> String.format(Locale.ROOT, "%.2f",
                    stats.earned(StatsPeriod.WEEKLY, today));
            default -> "";
        };
    }
}
