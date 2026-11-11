package me.marcdoesntexist.galaxymines.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;







public final class PluginSettings {
    private static PluginSettings instance = defaults();

    public final int defaultResetDelay;
    public final boolean defaultBroadcastOnReset;
    public final boolean defaultBroadcastNearby;
    public final double defaultBroadcastRadius;
    public final int maxMineNameLength;
    public final Holograms holograms;
    public final Reset reset;
    public final Bulk bulk;
    public final Protection protection;
    public final BossBars bossBars;
    public final Drops drops;
    public final Teleport teleport;
    public final Feedback feedback;
    public final Statistics statistics;
    public final Economy economy;
    public final Events events;
    public final LuckyBlocks luckyBlocks;
    public final Timings timings;
    public final Infinite infinite;

    private PluginSettings(final int defaultResetDelay, final boolean defaultBroadcastOnReset,
                           final boolean defaultBroadcastNearby, final double defaultBroadcastRadius,
                           final int maxMineNameLength, final Holograms holograms, final Reset reset,
                           final Bulk bulk, final Protection protection, final BossBars bossBars,
                           final Drops drops,
                           final Teleport teleport, final Feedback feedback,
                           final Statistics statistics, final Economy economy,
                           final Events events, final LuckyBlocks luckyBlocks,
                           final Timings timings, final Infinite infinite) {
        this.defaultResetDelay = defaultResetDelay;
        this.defaultBroadcastOnReset = defaultBroadcastOnReset;
        this.defaultBroadcastNearby = defaultBroadcastNearby;
        this.defaultBroadcastRadius = defaultBroadcastRadius;
        this.maxMineNameLength = maxMineNameLength;
        this.holograms = holograms;
        this.reset = reset;
        this.bulk = bulk;
        this.protection = protection;
        this.bossBars = bossBars;
        this.drops = drops;
        this.teleport = teleport;
        this.feedback = feedback;
        this.statistics = statistics;
        this.economy = economy;
        this.events = events;
        this.luckyBlocks = luckyBlocks;
        this.timings = timings;
        this.infinite = infinite;
    }

    public static PluginSettings get() {
        return PluginSettings.instance;
    }

    public static void load(final FileConfiguration config) {
        if (config == null) {
            PluginSettings.instance = defaults();
            return;
        }
        final int resetDelay =
                Math.max(1, config.getInt("settings.defaults.resetDelayMinutes", 5));
        final boolean broadcastOnReset =
                config.getBoolean("settings.defaults.broadcastOnReset", true);
        final boolean broadcastNearby =
                config.getBoolean("settings.defaults.broadcastNearby", false);
        final double broadcastRadius = Math.max(1.0D,
                config.getDouble("settings.defaults.broadcastRadius", 50.0D));
        final int maxNameLength = clamp(config.getInt("settings.defaults.maxMineNameLength", 32),
                1, 64);
        final Holograms holograms = new Holograms(
                clamp(config.getDouble("settings.holograms.display.lineSpacing", 0.27D), 0.05D, 2.0D),
                config.getString("settings.holograms.display.billboard", "CENTER"),
                config.getBoolean("settings.holograms.display.seeThrough", false),
                config.getBoolean("settings.holograms.display.shadowed", true),
                clamp(config.getInt("settings.holograms.display.backgroundAlpha", 0), 0, 255),
                clamp(config.getInt("settings.holograms.display.lineWidth", 200), 1, 1000),
                clamp(config.getInt("settings.holograms.display.teleportDuration", 1), 0, 100),
                config.getString("settings.holograms.display.itemBillboard", "FIXED"),
                clamp(config.getInt("settings.holograms.orphan-cleanup-minutes", 0), 0, 1440));
        final Reset reset = new Reset(
                clamp(config.getInt("settings.reset.blocksPerTick", 8192), 1, 1_000_000),
                clamp(config.getDouble("settings.reset.maxMillisecondsPerTick", 4.0D), 0.5D, 50.0D),
                clamp(config.getDouble("settings.reset.globalMillisecondsPerTick", 6.0D),
                        0.5D, 50.0D),
                config.getBoolean("settings.reset.countdown.enabled", false),
                countdownSeconds(config.getIntegerList("settings.reset.countdown.seconds")),
                config.getBoolean("settings.reset.countdown.title", true),
                config.getBoolean("settings.reset.countdown.actionBar", true),
                config.getString("settings.reset.countdown.sound", "block.note_block.pling"),
                clamp(config.getInt("settings.reset.minCooldownSeconds", 0), 0, 3600),
                config.getBoolean("settings.reset.safeTeleport", true),
                clamp(config.getInt("settings.reset.stateSaveSeconds", 30), 0, 600),
                config.getBoolean("settings.reset.preloadChunks", true),
                clamp(config.getInt("settings.reset.maxPreloadChunks", 4096), 1, 1_000_000),
                clamp(config.getInt("settings.reset.waveLayersPerTick", 1), 1, 64),
                config.getBoolean("settings.reset.commands.enabled", true));
        final Bulk bulk = new Bulk(
                clamp(config.getInt("settings.bulk.blocksPerTick", 4096), 1, 1_000_000),
                clamp(config.getInt("settings.bulk.intervalTicks", 1), 1, 200));
        final Protection protection = new Protection(
                config.getBoolean("settings.protection.cancelBlockBreakOutsideMines", false),
                config.getBoolean("settings.protection.compositionOnly", false),
                clamp(config.getInt("settings.protection.denyCooldownSeconds", 3), 0, 60),
                config.getBoolean("settings.protection.requireAccessPermission", false),
                config.getBoolean("settings.protection.requireBreakPermission", false));
        final BossBars bossBars = new BossBars(
                config.getBoolean("settings.bossbar.enabled", false),
                config.getString("settings.bossbar.color", "BLUE"),
                config.getString("settings.bossbar.style", "SOLID"));
        final Drops drops = new Drops(
                config.getBoolean("settings.drops.autoPickup", false),
                config.getBoolean("settings.drops.autoSmelt", false),
                config.getBoolean("settings.drops.autoBlock", false),
                config.getBoolean("settings.drops.autoSell", false),
                clamp(config.getDouble("settings.drops.dropMultiplier", 1.0D), 0.0D, 100.0D),
                clamp(config.getDouble("settings.drops.sellMultiplier", 1.0D), 0.0D, 100.0D),
                config.getString("settings.drops.fullInventoryPolicy", "DROP"));
        final Teleport teleport = new Teleport(
                clamp(config.getInt("settings.teleport.warmupSeconds", 0), 0, 60),
                clamp(config.getInt("settings.teleport.cooldownSeconds", 0), 0, 3600),
                config.getBoolean("settings.teleport.cancelOnMove", true));
        final Feedback feedback = new Feedback(
                config.getString("settings.feedback.enter.sound", ""),
                config.getString("settings.feedback.enter.particle", ""),
                config.getString("settings.feedback.exit.sound", ""),
                config.getString("settings.feedback.exit.particle", ""),
                config.getString("settings.feedback.reset.sound", "entity.player.levelup"),
                config.getString("settings.feedback.reset.particle", ""),
                config.getString("settings.feedback.low.sound", "block.note_block.bass"),
                config.getString("settings.feedback.low.particle", ""));
        final Statistics statistics = new Statistics(
                config.getBoolean("settings.statistics.enabled", true),
                clamp(config.getInt("settings.statistics.saveSeconds", 60), 5, 3600),
                config.getString("settings.statistics.storage", "YAML"),
                config.getString("settings.statistics.jdbc.url", ""),
                config.getString("settings.statistics.jdbc.username", ""),
                config.getString("settings.statistics.jdbc.password", ""),
                clamp(config.getInt("settings.statistics.leaderboardCacheSeconds", 30), 1, 600));
        final Economy economy = new Economy(
                config.getBoolean("economy.enabled", false),
                config.getBoolean("economy.shop-enabled", false),
                config.getBoolean("economy.buy-enabled", false),
                config.getDouble("economy.buy-markup", 1.5D),
                config.getString("economy.pricesFile", "prices.yml"),
                readMultipliers(config),
                me.marcdoesntexist.galaxymines.integration.economy.SellPriority.parseGlobal(
                        config.getString("economy.sellPriority", "PROVIDER_FIRST")));
        final Events events = new Events(
                config.getBoolean("events.enabled", true),
                config.getString("events.file", "events.yml"));
        final LuckyBlocks luckyBlocks = new LuckyBlocks(
                config.getBoolean("luckyblocks.enabled", false),
                config.getString("luckyblocks.file", "luckyblocks.yml"));
        final Timings timings = new Timings(
                config.getBoolean("settings.timings.enabled", true),
                clamp(config.getInt("settings.timings.sampleSize", 512), 16, 4096));
        final Infinite infinite = new Infinite(
                clamp(config.getInt("settings.infinite.maxRespawnsPerTick", 128), 1, 4096));
        PluginSettings.instance = new PluginSettings(resetDelay, broadcastOnReset, broadcastNearby,
                broadcastRadius, maxNameLength, holograms, reset, bulk, protection, bossBars,
                drops, teleport, feedback, statistics, economy, events, luckyBlocks, timings,
                infinite);
    }

    private static java.util.Map<String, Double> readMultipliers(final FileConfiguration config) {
        final java.util.Map<String, Double> multipliers = new java.util.LinkedHashMap<>();
        final org.bukkit.configuration.ConfigurationSection section =
                config.getConfigurationSection("economy.sellMultipliers");
        if (section != null) {
            for (final String permission : section.getKeys(false)) {
                final double value = section.getDouble(permission, 1.0D);
                if (value > 0.0D) {
                    multipliers.put(permission, value);
                }
            }
        }
        return java.util.Collections.unmodifiableMap(multipliers);
    }

    private static int[] countdownSeconds(final List<Integer> configured) {
        final Set<Integer> values = new LinkedHashSet<>();
        for (final Integer value : configured) {
            if (value != null && value > 0) {
                values.add(Math.min(value, 3600));
            }
        }
        if (values.isEmpty()) {
            values.addAll(Arrays.asList(60, 30, 10, 5, 4, 3, 2, 1));
        }
        final List<Integer> sorted = new ArrayList<>(values);
        sorted.sort((first, second) -> Integer.compare(second, first));
        final int[] seconds = new int[sorted.size()];
        for (int i = 0; i < sorted.size(); ++i) {
            seconds[i] = sorted.get(i);
        }
        return seconds;
    }

    private static PluginSettings defaults() {
        return new PluginSettings(5, true, false, 50.0D, 32,
                new Holograms(0.27D, "CENTER", false, true, 0, 200, 1, "FIXED", 0),
                new Reset(8192, 4.0D, 6.0D, false, new int[]{60, 30, 10, 5, 4, 3, 2, 1},
                        true, true, "block.note_block.pling", 0, true, 30, true, 4096, 1, true),
                new Bulk(4096, 1),
                new Protection(false, false, 3, false, false),
                new BossBars(false, "BLUE", "SOLID"),
                new Drops(false, false, false, false, 1.0D, 1.0D, "DROP"),
                new Teleport(0, 0, true),
                new Feedback("", "", "", "", "entity.player.levelup", "",
                        "block.note_block.bass", ""),
                new Statistics(true, 60, "YAML", "", "", "", 30),
                new Economy(false, false, false, 1.5D, "prices.yml",
                        java.util.Collections.emptyMap(),
                        me.marcdoesntexist.galaxymines.integration.economy.SellPriority
                                .PROVIDER_FIRST),
                new Events(true, "events.yml"), new LuckyBlocks(false, "luckyblocks.yml"),
                new Timings(true, 512), new Infinite(128));
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(final double value, final double min, final double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Reset {
        public final int blocksPerTick;
        


        public final long maxNanosPerTick;
        



        public final long globalNanosPerTick;
        public final boolean countdownEnabled;
        public final int[] countdownSeconds;
        public final boolean countdownTitle;
        public final boolean countdownActionBar;
        public final String countdownSound;
        public final int minCooldownSeconds;
        public final boolean safeTeleport;
        public final int stateSaveSeconds;
        public final boolean preloadChunks;
        public final int maxPreloadChunks;
        public final int waveLayersPerTick;
        



        public final boolean commandsEnabled;

        private Reset(final int blocksPerTick, final double maxMillisecondsPerTick,
                      final double globalMillisecondsPerTick,
                      final boolean countdownEnabled, final int[] countdownSeconds,
                      final boolean countdownTitle, final boolean countdownActionBar,
                      final String countdownSound, final int minCooldownSeconds,
                      final boolean safeTeleport, final int stateSaveSeconds,
                      final boolean preloadChunks, final int maxPreloadChunks,
                      final int waveLayersPerTick, final boolean commandsEnabled) {
            this.blocksPerTick = blocksPerTick;
            this.maxNanosPerTick = (long) (maxMillisecondsPerTick * 1_000_000.0D);
            this.globalNanosPerTick = (long) (globalMillisecondsPerTick * 1_000_000.0D);
            this.countdownEnabled = countdownEnabled;
            this.countdownSeconds = countdownSeconds;
            this.countdownTitle = countdownTitle;
            this.countdownActionBar = countdownActionBar;
            this.countdownSound = countdownSound;
            this.minCooldownSeconds = minCooldownSeconds;
            this.safeTeleport = safeTeleport;
            this.stateSaveSeconds = stateSaveSeconds;
            this.preloadChunks = preloadChunks;
            this.maxPreloadChunks = maxPreloadChunks;
            this.waveLayersPerTick = waveLayersPerTick;
            this.commandsEnabled = commandsEnabled;
        }
    }

    



    public static final class Bulk {
        public final int blocksPerTick;
        public final int intervalTicks;

        private Bulk(final int blocksPerTick, final int intervalTicks) {
            this.blocksPerTick = blocksPerTick;
            this.intervalTicks = intervalTicks;
        }
    }

    public static final class Drops {
        public final boolean autoPickup;
        public final boolean autoSmelt;
        public final boolean autoBlock;
        public final boolean autoSell;
        public final double dropMultiplier;
        public final double sellMultiplier;
        public final String fullInventoryPolicy;

        private Drops(final boolean autoPickup, final boolean autoSmelt, final boolean autoBlock,
                      final boolean autoSell, final double dropMultiplier,
                      final double sellMultiplier, final String fullInventoryPolicy) {
            this.autoPickup = autoPickup;
            this.autoSmelt = autoSmelt;
            this.autoBlock = autoBlock;
            this.autoSell = autoSell;
            this.dropMultiplier = dropMultiplier;
            this.sellMultiplier = sellMultiplier;
            this.fullInventoryPolicy = fullInventoryPolicy;
        }
    }

    public static final class Teleport {
        public final int warmupSeconds;
        public final int cooldownSeconds;
        public final boolean cancelOnMove;

        private Teleport(final int warmupSeconds, final int cooldownSeconds,
                         final boolean cancelOnMove) {
            this.warmupSeconds = warmupSeconds;
            this.cooldownSeconds = cooldownSeconds;
            this.cancelOnMove = cancelOnMove;
        }
    }

    public static final class Feedback {
        public final String enterSound;
        public final String enterParticle;
        public final String exitSound;
        public final String exitParticle;
        public final String resetSound;
        public final String resetParticle;
        public final String lowSound;
        public final String lowParticle;

        private Feedback(final String enterSound, final String enterParticle,
                         final String exitSound, final String exitParticle,
                         final String resetSound, final String resetParticle,
                         final String lowSound, final String lowParticle) {
            this.enterSound = enterSound;
            this.enterParticle = enterParticle;
            this.exitSound = exitSound;
            this.exitParticle = exitParticle;
            this.resetSound = resetSound;
            this.resetParticle = resetParticle;
            this.lowSound = lowSound;
            this.lowParticle = lowParticle;
        }
    }

    public static final class Statistics {
        public final boolean enabled;
        public final int saveSeconds;
        public final String storage;
        public final String jdbcUrl;
        public final String jdbcUsername;
        public final String jdbcPassword;
        public final int leaderboardCacheSeconds;

        private Statistics(final boolean enabled, final int saveSeconds, final String storage,
                           final String jdbcUrl, final String jdbcUsername,
                           final String jdbcPassword, final int leaderboardCacheSeconds) {
            this.enabled = enabled;
            this.saveSeconds = saveSeconds;
            this.storage = storage;
            this.jdbcUrl = jdbcUrl;
            this.jdbcUsername = jdbcUsername;
            this.jdbcPassword = jdbcPassword;
            this.leaderboardCacheSeconds = leaderboardCacheSeconds;
        }
    }

    public static final class Economy {
        public final boolean enabled;
        
        public final boolean shopEnabled;
        
        public final boolean buyEnabled;
        
        public final double buyMarkup;
        public final String pricesFile;
        public final java.util.Map<String, Double> sellMultipliers;
        


        public final me.marcdoesntexist.galaxymines.integration.economy.SellPriority sellPriority;

        private Economy(final boolean enabled, final boolean shopEnabled, final boolean buyEnabled,
                        final double buyMarkup, final String pricesFile,
                        final java.util.Map<String, Double> sellMultipliers,
                        final me.marcdoesntexist.galaxymines.integration.economy.SellPriority
                                sellPriority) {
            this.enabled = enabled;
            this.shopEnabled = shopEnabled;
            this.buyEnabled = buyEnabled;
            this.buyMarkup = buyMarkup;
            this.pricesFile = pricesFile;
            this.sellMultipliers = sellMultipliers;
            this.sellPriority = sellPriority;
        }
    }

    public static final class Events {
        public final boolean enabled;
        public final String file;

        private Events(final boolean enabled, final String file) {
            this.enabled = enabled;
            this.file = file == null || file.isBlank() ? "events.yml" : file;
        }
    }

    public static final class LuckyBlocks {
        public final boolean enabled;
        public final String file;

        private LuckyBlocks(final boolean enabled, final String file) {
            this.enabled = enabled;
            this.file = file == null || file.isBlank() ? "luckyblocks.yml" : file;
        }
    }

    



    public static final class Timings {
        public final boolean enabled;
        public final int sampleSize;

        private Timings(final boolean enabled, final int sampleSize) {
            this.enabled = enabled;
            this.sampleSize = sampleSize;
        }
    }

    



    public static final class Infinite {
        public final int maxRespawnsPerTick;

        private Infinite(final int maxRespawnsPerTick) {
            this.maxRespawnsPerTick = maxRespawnsPerTick;
        }
    }

    public static final class Protection {
        public final boolean cancelBlockBreakOutsideMines;
        public final boolean compositionOnly;
        public final int denyCooldownSeconds;
        public final boolean requireAccessPermission;
        public final boolean requireBreakPermission;

        private Protection(final boolean cancelBlockBreakOutsideMines,
                           final boolean compositionOnly, final int denyCooldownSeconds,
                           final boolean requireAccessPermission,
                           final boolean requireBreakPermission) {
            this.cancelBlockBreakOutsideMines = cancelBlockBreakOutsideMines;
            this.compositionOnly = compositionOnly;
            this.denyCooldownSeconds = denyCooldownSeconds;
            this.requireAccessPermission = requireAccessPermission;
            this.requireBreakPermission = requireBreakPermission;
        }
    }

    public static final class BossBars {
        public final boolean enabled;
        public final String color;
        public final String style;

        private BossBars(final boolean enabled, final String color, final String style) {
            this.enabled = enabled;
            this.color = color;
            this.style = style;
        }
    }

    public static final class Holograms {
        public final double lineSpacing;
        public final String billboard;
        public final boolean seeThrough;
        public final boolean shadowed;
        public final int backgroundAlpha;
        public final int lineWidth;
        public final int teleportDuration;
        public final String itemBillboard;
        


        public final int orphanCleanupMinutes;

        private Holograms(final double lineSpacing, final String billboard,
                          final boolean seeThrough, final boolean shadowed, final int backgroundAlpha,
                          final int lineWidth, final int teleportDuration, final String itemBillboard,
                          final int orphanCleanupMinutes) {
            this.lineSpacing = lineSpacing;
            this.billboard = billboard;
            this.seeThrough = seeThrough;
            this.shadowed = shadowed;
            this.backgroundAlpha = backgroundAlpha;
            this.lineWidth = lineWidth;
            this.teleportDuration = teleportDuration;
            this.itemBillboard = itemBillboard;
            this.orphanCleanupMinutes = orphanCleanupMinutes;
        }
    }
}
