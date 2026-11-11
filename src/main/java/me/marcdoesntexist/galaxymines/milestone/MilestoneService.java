package me.marcdoesntexist.galaxymines.milestone;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public final class MilestoneService {

    private static final MilestoneService INSTANCE = new MilestoneService();

    private final Map<UUID, Long> totals = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Long>> awarded = new ConcurrentHashMap<>();
    private MilestoneTracker tracker = new MilestoneTracker(List.of());
    private File dataFile;
    private Logger logger;
    private boolean enabled;

    private MilestoneService() {
    }

    public static MilestoneService get() {
        return INSTANCE;
    }

    public static List<Milestone> parse(final ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        final ConfigurationSection rewards = section.getConfigurationSection("rewards");
        if (rewards == null) {
            return List.of();
        }
        final List<Milestone> parsed = new ArrayList<>();
        for (final String key : rewards.getKeys(false)) {
            final long blocks;
            try {
                blocks = Long.parseLong(key.trim());
            } catch (NumberFormatException exception) {
                continue;
            }
            if (blocks <= 0L) {
                continue;
            }
            final ConfigurationSection node = rewards.getConfigurationSection(key);
            final Milestone milestone = node == null
                    ? new Milestone(blocks, "", List.of())
                    : new Milestone(blocks, node.getString("message", ""),
                    node.getStringList("commands"));
            if (milestone.paysSomething()) {
                parsed.add(milestone);
            }
        }
        return parsed;
    }

    private static void saveQuietly(final YamlConfiguration yaml, final File file) {
        try {
            yaml.save(file);
        } catch (Exception ignored) {

        }
    }

    public void load(final ConfigurationSection section, final File dataFile, final Logger logger) {
        this.dataFile = dataFile;
        this.logger = logger;
        this.tracker = new MilestoneTracker(parse(section));
        this.enabled = section != null && section.getBoolean("enabled", false)
                && !this.tracker.milestones().isEmpty();
        this.totals.clear();
        this.awarded.clear();
        if (!this.enabled) {
            return;
        }
        loadData();
        if (logger != null) {
            logger.info("Block milestones: " + tracker.milestones().size() + " thresholds");
        }
    }

    public void record(final Player player) {
        if (!enabled || player == null) {
            return;
        }
        final UUID id = player.getUniqueId();
        final long before = totals.getOrDefault(id, 0L);
        final long after = before + 1L;
        totals.put(id, after);
        final List<Milestone> crossed = tracker.crossed(before, after);
        if (crossed.isEmpty()) {
            return;
        }
        final Set<Long> paid = awarded.computeIfAbsent(id,
                ignored -> ConcurrentHashMap.newKeySet());
        boolean changed = false;
        for (final Milestone milestone : crossed) {
            if (paid.add(milestone.blocks())) {
                deliver(player, milestone);
                changed = true;
            }
        }
        if (changed) {
            saveAsync();
        }
    }

    private void deliver(final Player player, final Milestone milestone) {
        if (!milestone.message().isBlank()) {
            try {
                player.sendMessage(Text.parse(milestone.message()
                        .replace("%player%", player.getName())));
            } catch (Throwable ignored) {

            }
        }
        for (final String raw : milestone.commands()) {
            final String command = raw.replace("%player%", player.getName()).trim();
            if (command.isEmpty()) {
                continue;
            }
            try {
                Bukkit.getScheduler().runTask(GalaxyMines.getInstance(),
                        () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
            } catch (Throwable exception) {
                if (logger != null) {
                    logger.warning("Milestone command failed: " + command + " ("
                            + exception.getMessage() + ")");
                }
            }
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public int thresholds() {
        return tracker.milestones().size();
    }

    public long total(final UUID player) {
        return totals.getOrDefault(player, 0L);
    }

    public long nextThreshold(final UUID player) {
        return tracker.nextThreshold(total(player));
    }

    public MilestoneTracker tracker() {
        return tracker;
    }

    public Status status(final UUID player) {
        final long total = total(player);
        return new Status(this.enabled, this.tracker.milestones(), total,
                this.tracker.nextThreshold(total),
                this.awarded.getOrDefault(player, Set.of()));
    }

    private void loadData() {
        if (dataFile == null || !dataFile.isFile()) {
            return;
        }
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        final ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (final String key : players.getKeys(false)) {
            final UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            totals.put(id, Math.max(0L, players.getLong(key + ".total", 0L)));
            final Set<Long> paid = ConcurrentHashMap.newKeySet();
            for (final long value : players.getLongList(key + ".awarded")) {
                if (value > 0L) {
                    paid.add(value);
                }
            }
            awarded.put(id, paid);
        }
    }


    public synchronized void saveAsync() {
        if (!enabled || dataFile == null) {
            return;
        }
        final YamlConfiguration yaml = new YamlConfiguration();
        for (final Map.Entry<UUID, Long> entry : totals.entrySet()) {
            yaml.set("players." + entry.getKey() + ".total", entry.getValue());
            yaml.set("players." + entry.getKey() + ".awarded",
                    new ArrayList<>(awarded.getOrDefault(entry.getKey(), Set.of())));
        }
        final File target = dataFile;
        try {
            Bukkit.getScheduler().runTaskAsynchronously(GalaxyMines.getInstance(),
                    () -> saveQuietly(yaml, target));
        } catch (Throwable ignored) {

        }
    }

    public void shutdown() {
        if (enabled) {
            saveAsync();
        }
        totals.clear();
        awarded.clear();
        enabled = false;
    }

    public record Status(boolean enabled, List<Milestone> milestones, long total,
                         long nextThreshold, Set<Long> awarded) {

        public Status {
            milestones = List.copyOf(milestones == null ? List.of() : milestones);
            awarded = Set.copyOf(awarded == null ? Set.of() : awarded);
        }

        public boolean allReached() {
            return this.nextThreshold <= 0L;
        }

        public boolean isPaid(final long blocks) {
            return this.awarded.contains(blocks);
        }


        public long remaining() {
            return this.nextThreshold <= 0L ? 0L : Math.max(0L, this.nextThreshold - this.total);
        }
    }
}
