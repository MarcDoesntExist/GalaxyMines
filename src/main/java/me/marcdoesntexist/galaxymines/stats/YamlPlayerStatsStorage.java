package me.marcdoesntexist.galaxymines.stats;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.utils.AsyncFileWriter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;





public final class YamlPlayerStatsStorage implements PlayerStatsStorage {
    private final File folder;

    public YamlPlayerStatsStorage(final File storageFolder) {
        this.folder = new File(storageFolder, "players");
    }

    @Override
    public void init() {
        if (!this.folder.exists() && !this.folder.mkdirs()) {
            GalaxyMines.getInstance().debug(
                    "Could not create the player statistics folder " + this.folder, true);
        }
    }

    @Override
    public PlayerStats load(final UUID uuid, final String name) {
        final File file = file(uuid);
        if (!file.isFile()) {
            return new PlayerStats(uuid, name);
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        final PlayerStats stats = new PlayerStats(uuid, config.getString("name", name));
        stats.setLoadedTotals(config.getLong("total.blocks"), config.getLong("total.sold"),
                config.getDouble("total.earned"));
        final ConfigurationSection mines = config.getConfigurationSection("mines");
        if (mines != null) {
            for (final String mine : mines.getKeys(false)) {
                stats.mineBlocks().put(mine, Math.max(0L, mines.getLong(mine)));
            }
        }
        final ConfigurationSection periods = config.getConfigurationSection("periods");
        if (periods != null) {
            for (final String key : periods.getKeys(false)) {
                final ConfigurationSection value = periods.getConfigurationSection(key);
                if (value == null) {
                    continue;
                }
                final PlayerStats.PeriodValue period =
                        stats.periods().computeIfAbsent(key,
                                ignored -> new PlayerStats.PeriodValue());
                period.blocks = Math.max(0L, value.getLong("blocks"));
                period.earned = Math.max(0.0D, value.getDouble("earned"));
            }
        }
        stats.clean();
        return stats;
    }

    @Override
    public void save(final PlayerStats stats) {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("name", stats.name());
        config.set("total.blocks", stats.totalBlocks());
        config.set("total.sold", stats.totalSoldItems());
        config.set("total.earned", stats.totalEarned());
        for (final var entry : stats.mineBlocks().entrySet()) {
            config.set("mines." + entry.getKey(), entry.getValue());
        }
        for (final var entry : stats.periods().entrySet()) {
            config.set("periods." + entry.getKey() + ".blocks", entry.getValue().blocks);
            config.set("periods." + entry.getKey() + ".earned", entry.getValue().earned);
        }
        AsyncFileWriter.write(file(stats.uuid()), config.saveToString());
    }

    @Override
    public List<LeaderboardEntry> leaderboard(final StatsPeriod period, final String key,
                                              final int limit) {
        final List<LeaderboardEntry> entries = new ArrayList<>();
        final File[] files = this.folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return entries;
        }
        final String path = "periods." + period.name() + ":" + key;
        for (final File file : files) {
            try {
                final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                final long blocks = config.getLong(path + ".blocks");
                if (blocks <= 0L) {
                    continue;
                }
                final String rawUuid = file.getName().substring(0, file.getName().length() - 4);
                entries.add(new LeaderboardEntry(UUID.fromString(rawUuid),
                        config.getString("name", rawUuid), blocks,
                        config.getDouble(path + ".earned")));
            } catch (RuntimeException ignored) {
            }
        }
        entries.sort(Comparator.comparingLong(LeaderboardEntry::blocks).reversed());
        return entries.size() > limit ? new ArrayList<>(entries.subList(0, limit)) : entries;
    }

    @Override
    public void close() {
    }

    private File file(final UUID uuid) {
        return new File(this.folder, uuid + ".yml");
    }
}
