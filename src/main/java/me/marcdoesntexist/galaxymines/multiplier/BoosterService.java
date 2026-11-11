package me.marcdoesntexist.galaxymines.multiplier;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.logging.Logger;









public final class BoosterService {
    private static final BoosterService INSTANCE = new BoosterService();

    private final Map<UUID, Map<BoosterType, PlayerBooster>> boosters = new ConcurrentHashMap<>();
    private File file;
    private Logger logger = Logger.getLogger("GalaxyMines");
    private LongSupplier clock = System::currentTimeMillis;
    private double maxMultiplier = 100.0D;
    private int maxDurationSeconds = 604800;

    private BoosterService() {
    }

    public static BoosterService get() {
        return INSTANCE;
    }



    public synchronized void load(final File file, final Logger logger,
                                  final double maxMultiplier, final int maxDurationSeconds) {
        this.file = file;
        if (logger != null) {
            this.logger = logger;
        }
        this.maxMultiplier = Math.max(1.0D, maxMultiplier);
        this.maxDurationSeconds = Math.max(1, maxDurationSeconds);
        this.boosters.clear();
        if (file == null || !file.isFile()) {
            return;
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        final ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        final long now = this.clock.getAsLong();
        for (final String raw : players.getKeys(false)) {
            final UUID uuid;
            try {
                uuid = UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                this.logger.warning("Skipping booster entry with an invalid UUID: " + raw);
                continue;
            }
            final ConfigurationSection entry = players.getConfigurationSection(raw);
            if (entry == null) {
                continue;
            }
            for (final BoosterType type : BoosterType.values()) {
                final ConfigurationSection section = entry.getConfigurationSection(type.name());
                if (section == null) {
                    continue;
                }
                final double multiplier = section.getDouble("multiplier", 1.0D);
                final long expiresAt = section.getLong("expires-at", 0L);
                if (multiplier > 1.0D && expiresAt > now) {
                    this.boosters.computeIfAbsent(uuid, ignored -> new EnumMap<>(BoosterType.class))
                            .put(type, new PlayerBooster(type, multiplier, expiresAt));
                }
            }
        }
    }





    public synchronized PlayerBooster give(final UUID uuid, final BoosterType type,
                                           final double multiplier, final int durationSeconds) {
        if (uuid == null || type == null) {
            throw new IllegalArgumentException("uuid and type are required");
        }
        final long now = this.clock.getAsLong();
        final double clamped = Math.min(this.maxMultiplier, Math.max(1.0D, multiplier));
        final int seconds = Math.min(this.maxDurationSeconds, Math.max(1, durationSeconds));
        final Map<BoosterType, PlayerBooster> current = this.boosters
                .computeIfAbsent(uuid, ignored -> new EnumMap<>(BoosterType.class));
        final PlayerBooster existing = current.get(type);
        final double stacked = existing != null && existing.active(now)
                ? Math.min(this.maxMultiplier, existing.multiplier() * clamped)
                : clamped;
        final long from = existing == null ? now : Math.max(now, existing.expiresAt());
        final PlayerBooster booster = new PlayerBooster(type, stacked,
                from + seconds * 1000L);
        current.put(type, booster);
        this.save();
        return booster;
    }



    public synchronized double multiplier(final UUID uuid, final BoosterType type) {
        if (uuid == null || type == null) {
            return 1.0D;
        }
        final Map<BoosterType, PlayerBooster> current = this.boosters.get(uuid);
        if (current == null) {
            return 1.0D;
        }
        final PlayerBooster booster = current.get(type);
        if (booster == null) {
            return 1.0D;
        }
        if (!booster.active(this.clock.getAsLong())) {
            current.remove(type);
            if (current.isEmpty()) {
                this.boosters.remove(uuid);
            }
            this.save();
            return 1.0D;
        }
        return booster.multiplier();
    }

    public double multiplier(final Player player, final BoosterType type) {
        return player == null ? 1.0D : this.multiplier(player.getUniqueId(), type);
    }



    public synchronized List<PlayerBooster> active(final UUID uuid) {
        final Map<BoosterType, PlayerBooster> current = this.boosters.get(uuid);
        if (current == null) {
            return List.of();
        }
        final long now = this.clock.getAsLong();
        final List<PlayerBooster> active = new ArrayList<>(current.size());
        for (final PlayerBooster booster : current.values()) {
            if (booster.active(now)) {
                active.add(booster);
            }
        }
        active.sort(Comparator.comparing(booster -> booster.type().name()));
        return List.copyOf(active);
    }

    public synchronized int remainingSeconds(final UUID uuid, final BoosterType type) {
        if (uuid == null || type == null) {
            return 0;
        }
        final Map<BoosterType, PlayerBooster> current = this.boosters.get(uuid);
        if (current == null) {
            return 0;
        }
        final PlayerBooster booster = current.get(type);
        return booster == null ? 0 : booster.remainingSeconds(this.clock.getAsLong());
    }

    public synchronized boolean clear(final UUID uuid, final BoosterType type) {
        final Map<BoosterType, PlayerBooster> current = this.boosters.get(uuid);
        if (current == null || type == null || current.remove(type) == null) {
            return false;
        }
        if (current.isEmpty()) {
            this.boosters.remove(uuid);
        }
        this.save();
        return true;
    }

    public synchronized int clearAll(final UUID uuid) {
        final Map<BoosterType, PlayerBooster> removed = this.boosters.remove(uuid);
        if (removed == null || removed.isEmpty()) {
            return 0;
        }
        this.save();
        return removed.size();
    }



    public synchronized void save() {
        if (this.file == null) {
            return;
        }
        final YamlConfiguration config = new YamlConfiguration();
        final long now = this.clock.getAsLong();
        for (final Map.Entry<UUID, Map<BoosterType, PlayerBooster>> entry
                : this.boosters.entrySet()) {
            for (final PlayerBooster booster : entry.getValue().values()) {
                if (!booster.active(now)) {
                    continue;
                }
                final String path = "players." + entry.getKey() + "." + booster.type().name();
                config.set(path + ".multiplier", booster.multiplier());
                config.set(path + ".expires-at", booster.expiresAt());
            }
        }
        try {
            final File parent = this.file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                this.logger.warning("Could not create the booster folder " + parent);
                return;
            }
            config.save(this.file);
        } catch (IOException exception) {
            this.logger.warning("Could not save boosters.yml: " + exception.getMessage());
        }
    }

    public synchronized void shutdown() {
        this.save();
        this.boosters.clear();
    }



    synchronized void clock(final LongSupplier clock) {
        this.clock = clock == null ? System::currentTimeMillis : clock;
    }
}
