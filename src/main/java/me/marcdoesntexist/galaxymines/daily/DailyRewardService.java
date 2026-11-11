package me.marcdoesntexist.galaxymines.daily;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.logging.Logger;








public final class DailyRewardService {
    private static final DailyRewardService INSTANCE = new DailyRewardService();

    private final Map<UUID, Claim> claims = new ConcurrentHashMap<>();
    private final Map<Integer, Reward> rewards = new ConcurrentHashMap<>();
    private ClaimState state = new ClaimState(false, 24, 48, 7);
    private Reward fallback = new Reward(0.0D, List.of());
    private File file;
    private Logger logger = Logger.getLogger("GalaxyMines");
    private LongSupplier clock = System::currentTimeMillis;

    private DailyRewardService() {
    }

    public static DailyRewardService get() {
        return INSTANCE;
    }

    private static ClaimState readState(final ConfigurationSection config) {
        if (config == null) {
            return new ClaimState(false, 24, 48, 7);
        }
        return new ClaimState(config.getBoolean("enabled", true),
                clamp(config.getInt("cooldown-hours", 24), 1, 720),
                clamp(config.getInt("streak-window-hours", 48), 1, 1440),
                clamp(config.getInt("max-streak", 7), 1, 3650));
    }

    private static Reward readReward(final ConfigurationSection parent, final String path) {
        final ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            return new Reward(0.0D, List.of());
        }
        final List<String> commands = new ArrayList<>();
        for (final String command : section.getStringList("commands")) {
            if (command != null && !command.isBlank()) {
                commands.add(command);
            }
        }
        return new Reward(Math.max(0.0D, section.getDouble("money", 0.0D)),
                List.copyOf(commands));
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.min(max, Math.max(min, value));
    }



    public synchronized void load(final ConfigurationSection config, final File dataFile,
                                  final Logger logger) {
        if (logger != null) {
            this.logger = logger;
        }
        this.file = dataFile;
        this.rewards.clear();
        this.fallback = new Reward(0.0D, List.of());
        this.state = readState(config);
        if (config != null) {
            final ConfigurationSection ladder = config.getConfigurationSection("rewards");
            if (ladder != null) {
                for (final String key : ladder.getKeys(false)) {
                    try {
                        this.rewards.put(Integer.parseInt(key), readReward(ladder, key));
                    } catch (NumberFormatException ignored) {
                        this.logger.warning("Skipping daily reward with a non-numeric day: "
                                + key);
                    }
                }
            }
            if (config.isConfigurationSection("default")) {
                this.fallback = readReward(config, "default");
            }
        }
        this.loadClaims();
    }

    public synchronized boolean isEnabled() {
        return this.state.enabled();
    }

    public synchronized Result claim(final UUID uuid, final String name) {
        if (uuid == null) {
            return Result.disabled();
        }
        if (!this.state.enabled()) {
            return Result.disabled();
        }
        final long now = this.clock.getAsLong();
        final Claim current = this.claims.get(uuid);
        if (current != null && current.lastClaim() > 0L) {
            final long elapsed = now - current.lastClaim();
            if (elapsed >= 0L && elapsed < this.state.cooldownHours() * 3_600_000L) {
                return new Result(false, false,
                        this.state.cooldownHours() * 3_600_000L - elapsed,
                        current.streak(), this.fallback);
            }
        }
        int streak = 1;
        if (current != null && current.streak() > 0) {
            final long elapsed = now - current.lastClaim();
            if (elapsed >= 0L && elapsed <= this.state.streakWindowHours() * 3_600_000L) {
                streak = current.streak() + 1;
            }
        }
        streak = Math.min(this.state.maxStreak(), streak);
        final Reward reward = this.rewards.getOrDefault(streak, this.fallback);
        this.claims.put(uuid, new Claim(now, streak));
        this.save();
        return new Result(true, true, 0L, streak, reward);
    }



    public synchronized void reset(final UUID uuid) {
        if (uuid != null && this.claims.remove(uuid) != null) {
            this.save();
        }
    }

    public synchronized int streak(final UUID uuid) {
        final Claim claim = this.claims.get(uuid);
        return claim == null ? 0 : claim.streak();
    }

    public synchronized Reward rewardFor(final int streak) {
        return this.rewards.getOrDefault(streak, this.fallback);
    }

    public synchronized void shutdown() {
        this.save();
        this.claims.clear();
    }

    public synchronized void save() {
        if (this.file == null) {
            return;
        }
        final YamlConfiguration config = new YamlConfiguration();
        for (final Map.Entry<UUID, Claim> entry : this.claims.entrySet()) {
            config.set("players." + entry.getKey() + ".last-claim", entry.getValue().lastClaim());
            config.set("players." + entry.getKey() + ".streak", entry.getValue().streak());
        }
        try {
            final File parent = this.file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                this.logger.warning("Could not create the daily rewards folder " + parent);
                return;
            }
            config.save(this.file);
        } catch (IOException exception) {
            this.logger.warning("Could not save dailyrewards-data.yml: "
                    + exception.getMessage());
        }
    }




    synchronized void clock(final LongSupplier clock) {
        this.clock = clock == null ? System::currentTimeMillis : clock;
    }

    private void loadClaims() {
        this.claims.clear();
        if (this.file == null || !this.file.isFile()) {
            return;
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);
        final ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (final String raw : players.getKeys(false)) {
            try {
                this.claims.put(UUID.fromString(raw), new Claim(
                        players.getLong(raw + ".last-claim", 0L),
                        players.getInt(raw + ".streak", 0)));
            } catch (IllegalArgumentException ignored) {
                this.logger.warning("Skipping daily reward entry with an invalid UUID: " + raw);
            }
        }
    }

    private record ClaimState(boolean enabled, int cooldownHours, int streakWindowHours,
                              int maxStreak) {
    }

    private record Claim(long lastClaim, int streak) {
    }

    public record Reward(double money, List<String> commands) {
    }

    public record Result(boolean claimed, boolean enabled, long remainingMillis, int streak,
                         Reward reward) {
        static Result disabled() {
            return new Result(false, false, 0L, 0, new Reward(0.0D, List.of()));
        }

        public int remainingSeconds() {
            final long seconds = this.remainingMillis / 1000L;
            return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
        }

        public String remainingText() {
            final int seconds = Math.max(0, this.remainingSeconds());
            if (seconds <= 0) {
                return "0s";
            }
            final StringBuilder builder = new StringBuilder();
            final int hours = seconds / 3600;
            final int minutes = (seconds % 3600) / 60;
            if (hours > 0) {
                builder.append(hours).append('h');
            }
            if (minutes > 0) {
                builder.append(builder.isEmpty() ? "" : " ").append(minutes).append('m');
            }
            if (builder.isEmpty()) {
                builder.append(seconds).append('s');
            }
            return builder.toString().toLowerCase(Locale.ROOT);
        }
    }
}
