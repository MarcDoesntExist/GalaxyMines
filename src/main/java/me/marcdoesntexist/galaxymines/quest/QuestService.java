package me.marcdoesntexist.galaxymines.quest;

import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.integration.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.logging.Logger;


public final class QuestService {
    private static final QuestService INSTANCE = new QuestService();

    private final Map<String, QuestDefinition> quests = new LinkedHashMap<>();
    private final Map<UUID, PlayerState> players = new ConcurrentHashMap<>();
    private File file;
    private Logger logger = Logger.getLogger("GalaxyMines");
    private LongSupplier clock = System::currentTimeMillis;
    private boolean enabled;
    private QuestPeriod period = QuestPeriod.DAILY;

    private QuestService() {
    }

    public static QuestService get() {
        return INSTANCE;
    }


    public synchronized void load(final ConfigurationSection config, final File dataFile,
                                  final Logger logger) {
        if (logger != null) {
            this.logger = logger;
        }
        this.file = dataFile;
        this.quests.clear();
        this.enabled = config != null && config.getBoolean("enabled", true);
        this.period = config == null
                ? QuestPeriod.DAILY : QuestPeriod.parse(config.getString("reset", "daily"));
        if (config != null) {
            final ConfigurationSection entries = config.getConfigurationSection("entries");
            if (entries != null) {
                for (final String id : entries.getKeys(false)) {
                    final ConfigurationSection section = entries.getConfigurationSection(id);
                    if (section == null) {
                        continue;
                    }
                    final QuestType type = QuestType.parse(section.getString("type"));
                    final long target = Math.max(1L, section.getLong("target", 1L));
                    if (type == null) {
                        this.logger.warning("Quest '" + id + "' has an unknown type; skipped.");
                        continue;
                    }
                    this.quests.put(id.toLowerCase(java.util.Locale.ROOT),
                            new QuestDefinition(id.toLowerCase(java.util.Locale.ROOT),
                                    section.getString("display-name", id), type, target,
                                    Math.max(0.0D, section.getDouble("reward-money", 0.0D)),
                                    List.copyOf(section.getStringList("reward-commands"))));
                }
            }
        }
        this.loadProgress();
    }

    public synchronized boolean isEnabled() {
        return this.enabled && !this.quests.isEmpty();
    }


    public synchronized List<QuestDefinition> track(final UUID uuid, final QuestType type,
                                                    final long amount) {
        if (!this.isEnabled() || uuid == null || type == null || amount <= 0L) {
            return List.of();
        }
        final PlayerState state = this.state(uuid);
        final List<QuestDefinition> completed = new ArrayList<>();
        for (final QuestDefinition quest : this.quests.values()) {
            if (quest.type() != type || state.claimed.contains(quest.id())) {
                continue;
            }
            final long current = state.progress.getOrDefault(quest.id(), 0L);
            if (current >= quest.target()) {
                continue;
            }
            final long updated = Math.min(quest.target(), current + amount);
            state.progress.put(quest.id(), updated);
            if (updated >= quest.target()) {
                state.claimed.add(quest.id());
                completed.add(quest);
            }
        }
        if (!completed.isEmpty()) {
            this.save();
        }
        return completed;
    }


    public void addProgress(final Player player, final QuestType type, final long amount) {
        if (player == null) {
            return;
        }
        for (final QuestDefinition quest : this.track(player.getUniqueId(), type, amount)) {
            if (quest.rewardMoney() > 0.0D) {
                EconomyService.deposit(player, quest.rewardMoney());
            }
            for (final String raw : quest.rewardCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        raw.replace("%player%", player.getName())
                                .replace("%uuid%", player.getUniqueId().toString()));
            }
            player.sendMessage(Text.parse("<green>Quest complete: <yellow>"
                    + quest.displayName() + "<green>!"));
        }
    }


    public synchronized List<Status> status(final UUID uuid) {
        final List<Status> statuses = new ArrayList<>();
        if (uuid == null) {
            return statuses;
        }
        final PlayerState state = this.players.get(uuid);
        final boolean current = state != null && state.period.equals(this.periodKey());
        for (final QuestDefinition quest : this.quests.values()) {
            final long progress = current ? state.progress.getOrDefault(quest.id(), 0L) : 0L;
            statuses.add(new Status(quest, Math.min(progress, quest.target()),
                    current && state.claimed.contains(quest.id())));
        }
        return statuses;
    }


    public synchronized void reset(final UUID uuid) {
        if (uuid != null && this.players.remove(uuid) != null) {
            this.save();
        }
    }

    public synchronized void save() {
        if (this.file == null) {
            return;
        }
        final YamlConfiguration config = new YamlConfiguration();
        final String period = this.periodKey();
        for (final Map.Entry<UUID, PlayerState> entry : this.players.entrySet()) {
            if (!entry.getValue().period.equals(period)) {
                continue;
            }
            final String path = "players." + entry.getKey();
            config.set(path + ".period", entry.getValue().period);
            for (final Map.Entry<String, Long> progress : entry.getValue().progress.entrySet()) {
                config.set(path + ".progress." + progress.getKey(), progress.getValue());
            }
            config.set(path + ".claimed", new ArrayList<>(entry.getValue().claimed));
        }
        try {
            final File parent = this.file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                this.logger.warning("Could not create the quests folder " + parent);
                return;
            }
            config.save(this.file);
        } catch (IOException exception) {
            this.logger.warning("Could not save quests-data.yml: " + exception.getMessage());
        }
    }

    public synchronized void shutdown() {
        this.save();
        this.players.clear();
    }


    synchronized void clock(final LongSupplier clock) {
        this.clock = clock == null ? System::currentTimeMillis : clock;
    }

    private PlayerState state(final UUID uuid) {
        final String period = this.periodKey();
        final PlayerState existing = this.players.get(uuid);
        if (existing != null && existing.period.equals(period)) {
            return existing;
        }
        final PlayerState fresh = new PlayerState(period);
        this.players.put(uuid, fresh);
        return fresh;
    }

    private String periodKey() {
        final LocalDate date = Instant.ofEpochMilli(this.clock.getAsLong())
                .atZone(ZoneId.systemDefault()).toLocalDate();
        if (this.period == QuestPeriod.WEEKLY) {
            return date.get(WeekFields.ISO.weekBasedYear()) + "-W"
                    + String.format(java.util.Locale.ROOT, "%02d",
                    date.get(WeekFields.ISO.weekOfWeekBasedYear()));
        }
        return date.toString();
    }

    private void loadProgress() {
        this.players.clear();
        if (this.file == null || !this.file.isFile()) {
            return;
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);
        final ConfigurationSection root = config.getConfigurationSection("players");
        if (root == null) {
            return;
        }
        final String current = this.periodKey();
        for (final String raw : root.getKeys(false)) {
            final UUID uuid;
            try {
                uuid = UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            final String period = root.getString(raw + ".period", "");
            if (!current.equals(period)) {
                continue;
            }
            final PlayerState state = new PlayerState(period);
            final ConfigurationSection progress = root.getConfigurationSection(raw + ".progress");
            if (progress != null) {
                for (final String quest : progress.getKeys(false)) {
                    state.progress.put(quest, Math.max(0L, progress.getLong(quest, 0L)));
                }
            }
            state.claimed.addAll(root.getStringList(raw + ".claimed"));
            this.players.put(uuid, state);
        }
    }

    public enum QuestPeriod {
        DAILY,
        WEEKLY;

        public static QuestPeriod parse(final String raw) {
            return raw != null && "weekly".equalsIgnoreCase(raw.trim()) ? WEEKLY : DAILY;
        }
    }

    public record Status(QuestDefinition quest, long progress, boolean claimed) {
    }

    private static final class PlayerState {
        private final String period;
        private final Map<String, Long> progress = new HashMap<>();
        private final Set<String> claimed = new LinkedHashSet<>();

        private PlayerState(final String period) {
            this.period = period;
        }
    }
}
