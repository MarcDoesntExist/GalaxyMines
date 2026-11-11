package me.marcdoesntexist.galaxymines.stats;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;






public final class PlayerStatsManager {
    private static final Map<UUID, PlayerStats> PROFILES = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();
    private static final Map<String, CachedBoard> BOARDS = new ConcurrentHashMap<>();
    private static PlayerStatsStorage storage;
    private static ExecutorService executor;
    private static BukkitTask flushTask;
    private static boolean enabled;

    private PlayerStatsManager() {
    }

    public static void init() {
        shutdown();
        final PluginSettings.Statistics settings = PluginSettings.get().statistics;
        enabled = settings.enabled;
        if (!enabled) {
            return;
        }
        executor = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "GalaxyMines-Stats");
            thread.setDaemon(true);
            return thread;
        });
        storage = createStorage(settings);
        startFlushTask(settings.saveSeconds);
        
        
        for (final Player player : Bukkit.getOnlinePlayers()) {
            loadAsync(player);
        }
    }

    private static PlayerStatsStorage createStorage(final PluginSettings.Statistics settings) {
        final String type = settings.storage == null ? "YAML"
                : settings.storage.trim().toUpperCase(java.util.Locale.ROOT);
        if (!type.equals("YAML")) {
            try {
                final PlayerStatsStorage jdbc = new JdbcPlayerStatsStorage(type,
                        settings.jdbcUrl, settings.jdbcUsername, settings.jdbcPassword);
                jdbc.init();
                GalaxyMines.getInstance().debug("Player statistics storage: " + type, true);
                return jdbc;
            } catch (Throwable t) {
                GalaxyMines.getInstance().printException(
                        "Could not start " + type + " statistics storage: falling back to YAML.",
                        t);
            }
        }
        final YamlPlayerStatsStorage yaml = new YamlPlayerStatsStorage(GalaxyMines.storageFolder);
        try {
            yaml.init();
        } catch (Exception ex) {
            GalaxyMines.getInstance().printException(
                    "Could not initialise the YAML statistics folder", ex);
        }
        return yaml;
    }

    private static void startFlushTask(final int seconds) {
        if (flushTask != null) {
            flushTask.cancel();
        }
        flushTask = Bukkit.getScheduler().runTaskTimer(GalaxyMines.getInstance(),
                () -> flushDirty(false), 20L * seconds, 20L * seconds);
    }

    public static boolean isEnabled() {
        return enabled && storage != null;
    }

    public static int cachedProfileCount() {
        return PROFILES.size();
    }

    public static int loadingProfileCount() {
        return LOADING.size();
    }

    public static int cachedLeaderboardCount() {
        return BOARDS.size();
    }

    public static void loadAsync(final Player player) {
        if (!isEnabled()) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        if (PROFILES.containsKey(uuid) || !LOADING.add(uuid)) {
            return;
        }
        final String name = player.getName();
        submit(() -> {
            PlayerStats loaded;
            try {
                loaded = storage.load(uuid, name);
            } catch (Throwable t) {
                GalaxyMines.getInstance().printException(
                        "Could not load statistics for " + name, t);
                loaded = new PlayerStats(uuid, name);
            }
            final PlayerStats result = loaded;
            Bukkit.getScheduler().runTask(GalaxyMines.getInstance(), () -> {
                LOADING.remove(uuid);
                final PlayerStats existing = PROFILES.get(uuid);
                if (existing != null) {
                    existing.mergeFrom(result);
                } else {
                    PROFILES.put(uuid, result);
                }
            });
        });
    }

    public static PlayerStats profile(final Player player) {
        return PROFILES.computeIfAbsent(player.getUniqueId(),
                ignored -> new PlayerStats(player.getUniqueId(), player.getName()));
    }

    public static PlayerStats profile(final UUID uuid) {
        return PROFILES.get(uuid);
    }

    public static void addMined(final Player player, final Mine mine) {
        
        me.marcdoesntexist.galaxymines.milestone.MilestoneService.get().record(player);
        
        
        me.marcdoesntexist.galaxymines.quest.QuestService.get().addProgress(player,
                me.marcdoesntexist.galaxymines.quest.QuestType.BLOCKS_MINED, 1L);
        final me.marcdoesntexist.galaxymines.pass.BattlePassService battlePass =
                me.marcdoesntexist.galaxymines.pass.BattlePassService.get();
        if (battlePass.isEnabled()) {
            battlePass.addXp(player.getUniqueId(), battlePass.xpPerBlock());
        }
        if (!isEnabled()) {
            return;
        }
        profile(player).addMined(mine == null ? "unknown" : mine.getName(), 1L);
    }

    public static void addSold(final Player player, final Mine mine, final long items,
                               final double money) {
        me.marcdoesntexist.galaxymines.quest.QuestService.get().addProgress(player,
                me.marcdoesntexist.galaxymines.quest.QuestType.ITEMS_SOLD, items);
        me.marcdoesntexist.galaxymines.quest.QuestService.get().addProgress(player,
                me.marcdoesntexist.galaxymines.quest.QuestType.MONEY_EARNED,
                (long) Math.floor(money));
        final me.marcdoesntexist.galaxymines.pass.BattlePassService battlePass =
                me.marcdoesntexist.galaxymines.pass.BattlePassService.get();
        if (battlePass.isEnabled()) {
            battlePass.addXp(player.getUniqueId(), battlePass.xpPerItemSold() * items);
        }
        if (!isEnabled()) {
            return;
        }
        profile(player).addSold(items, money);
    }

    




    public static void flush(final Player player, final boolean forget) {
        if (!isEnabled()) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        final PlayerStats stats = PROFILES.get(uuid);
        if (stats == null) {
            return;
        }
        
        
        save(stats, forget ? () -> forgetProfile(uuid, stats) : null);
    }

    



    private static void forgetProfile(final UUID uuid, final PlayerStats stats) {
        if (!stats.dirty() && PROFILES.get(uuid) == stats && Bukkit.getPlayer(uuid) == null) {
            
            
            PROFILES.remove(uuid, stats);
        }
    }

    private static void save(final PlayerStats stats) {
        save(stats, null);
    }

    




    private static void save(final PlayerStats stats, final Runnable afterSave) {
        if (!isEnabled()) {
            return;
        }
        if (!stats.dirty()) {
            if (afterSave != null) {
                afterSave.run();
            }
            return;
        }
        final PlayerStats snapshot = stats.snapshot();
        final long revision = stats.revision();
        submit(() -> {
            try {
                storage.save(snapshot);
            } catch (Throwable t) {
                GalaxyMines.getInstance().printException(
                        "Could not save statistics for " + snapshot.name(), t);
            }
            Bukkit.getScheduler().runTask(GalaxyMines.getInstance(), () -> {
                stats.markSaved(revision);
                if (afterSave != null) {
                    afterSave.run();
                }
            });
        });
    }

    private static void flushDirty(final boolean logging) {
        if (!isEnabled()) {
            return;
        }
        int count = 0;
        for (final PlayerStats stats : PROFILES.values()) {
            if (stats.dirty()) {
                save(stats);
                ++count;
            }
        }
        if (logging && count > 0) {
            GalaxyMines.getInstance().debug("Queued " + count + " player statistic saves", 3);
        }
    }

    public static void leaderboard(final StatsPeriod period, final int limit,
                                   final Consumer<List<LeaderboardEntry>> callback) {
        if (!isEnabled()) {
            callback.accept(Collections.emptyList());
            return;
        }
        final String key = period.key(LocalDate.now());
        final String cacheKey = period.name() + ":" + key + ":" + limit;
        final int cacheSeconds = PluginSettings.get().statistics.leaderboardCacheSeconds;
        final CachedBoard cached = BOARDS.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.loadedAt()
                < cacheSeconds * 1000L) {
            callback.accept(cached.entries());
            return;
        }
        submit(() -> {
            List<LeaderboardEntry> entries;
            try {
                entries = storage.leaderboard(period, key, limit);
            } catch (Throwable t) {
                GalaxyMines.getInstance().printException("Could not read the leaderboard", t);
                entries = Collections.emptyList();
            }
            final List<LeaderboardEntry> result = entries;
            BOARDS.put(cacheKey, new CachedBoard(result, System.currentTimeMillis()));
            Bukkit.getScheduler().runTask(GalaxyMines.getInstance(),
                    () -> callback.accept(result));
        });
    }

    public static void invalidateLeaderboards() {
        BOARDS.clear();
    }

    public static Collection<PlayerStats> onlineProfiles() {
        final List<PlayerStats> result = new ArrayList<>();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final PlayerStats stats = PROFILES.get(player.getUniqueId());
            if (stats != null) {
                result.add(stats);
            }
        }
        return result;
    }

    private static void submit(final Runnable runnable) {
        final ExecutorService current = executor;
        if (current == null || current.isShutdown()) {
            runnable.run();
            return;
        }
        current.execute(runnable);
    }

    


    public static void shutdown() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
        final ExecutorService current = executor;
        if (current == null) {
            storage = null;
            enabled = false;
            return;
        }
        
        
        current.shutdown();
        try {
            if (!current.awaitTermination(10L, TimeUnit.SECONDS)) {
                current.shutdownNow();
            }
        } catch (InterruptedException ex) {
            current.shutdownNow();
            Thread.currentThread().interrupt();
        }
        for (final PlayerStats stats : PROFILES.values()) {
            if (stats.dirty()) {
                try {
                    storage.save(stats.snapshot());
                    stats.clean();
                } catch (Throwable t) {
                    GalaxyMines.getInstance().printException(
                            "Could not save statistics for " + stats.name(), t);
                }
            }
        }
        if (storage != null) {
            storage.close();
        }
        storage = null;
        executor = null;
        enabled = false;
        LOADING.clear();
        PROFILES.clear();
        BOARDS.clear();
    }

    private record CachedBoard(List<LeaderboardEntry> entries, long loadedAt) {
    }
}
