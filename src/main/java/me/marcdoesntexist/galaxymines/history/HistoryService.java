package me.marcdoesntexist.galaxymines.history;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public final class HistoryService {
    private static final HistoryService INSTANCE = new HistoryService();

    private final Map<UUID, Deque<HistoryEntry>> entries = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private File file;
    private Logger logger = Logger.getLogger("GalaxyMines");
    private int maxEntries = 50;

    private HistoryService() {
    }

    public static HistoryService get() {
        return INSTANCE;
    }

    private static HistoryEntry read(final Map<?, ?> row) {
        if (row == null) {
            return null;
        }
        final Object time = row.get("time");
        final Object amount = row.get("amount");
        final Object items = row.get("items");
        if (!(time instanceof Number) || !(amount instanceof Number)
                || !(items instanceof Number)) {
            return null;
        }
        return new HistoryEntry(((Number) time).longValue(),
                String.valueOf(row.get("mine")),
                ((Number) amount).doubleValue(),
                ((Number) items).intValue(),
                String.valueOf(row.get("currency")));
    }


    public synchronized void load(final File file, final Logger logger, final int maxEntries) {
        this.file = file;
        if (logger != null) {
            this.logger = logger;
        }
        this.maxEntries = Math.max(1, maxEntries);
        this.entries.clear();
        this.names.clear();
        if (file == null || !file.isFile()) {
            return;
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        final ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (final String raw : players.getKeys(false)) {
            final UUID uuid;
            try {
                uuid = UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            final ConfigurationSection section = players.getConfigurationSection(raw);
            if (section == null) {
                continue;
            }
            final String name = section.getString("name");
            if (name != null && !name.isBlank()) {
                this.names.put(uuid, name);
            }
            final Deque<HistoryEntry> history = new ArrayDeque<>();
            for (final Map<?, ?> row : section.getMapList("entries")) {
                final HistoryEntry entry = read(row);
                if (entry != null) {
                    history.addLast(entry);
                }
            }
            if (!history.isEmpty()) {
                this.entries.put(uuid, history);
            }
        }
    }


    public synchronized void record(final UUID uuid, final String name, final String mine,
                                    final double amount, final int items, final String currency) {
        if (uuid == null || amount <= 0.0D || items <= 0) {
            return;
        }
        if (name != null && !name.isBlank()) {
            this.names.put(uuid, name);
        }
        final Deque<HistoryEntry> history = this.entries.computeIfAbsent(uuid,
                ignored -> new ArrayDeque<>());
        history.addFirst(new HistoryEntry(System.currentTimeMillis(),
                mine == null ? "" : mine, amount, items,
                currency == null ? "" : currency));
        while (history.size() > this.maxEntries) {
            history.removeLast();
        }
        this.save();
    }


    public synchronized List<HistoryEntry> entries(final UUID uuid) {
        final Deque<HistoryEntry> history = this.entries.get(uuid);
        return history == null ? List.of() : List.copyOf(history);
    }

    public synchronized int count(final UUID uuid) {
        final Deque<HistoryEntry> history = this.entries.get(uuid);
        return history == null ? 0 : history.size();
    }

    public synchronized void clear(final UUID uuid) {
        if (this.entries.remove(uuid) != null) {
            this.save();
        }
    }


    public synchronized void save() {
        if (this.file == null) {
            return;
        }
        final YamlConfiguration config = new YamlConfiguration();
        for (final Map.Entry<UUID, Deque<HistoryEntry>> entry : this.entries.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            final String path = "players." + entry.getKey();
            final String name = this.names.get(entry.getKey());
            if (name != null) {
                config.set(path + ".name", name);
            }
            final List<Map<String, Object>> rows = new ArrayList<>();
            for (final HistoryEntry history : entry.getValue()) {
                final Map<String, Object> row = new LinkedHashMap<>();
                row.put("time", history.time());
                row.put("mine", history.mine());
                row.put("amount", history.amount());
                row.put("items", history.items());
                row.put("currency", history.currency());
                rows.add(row);
            }
            config.set(path + ".entries", rows);
        }
        try {
            final File parent = this.file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                this.logger.warning("Could not create the history folder " + parent);
                return;
            }
            config.save(this.file);
        } catch (IOException exception) {
            this.logger.warning("Could not save history.yml: " + exception.getMessage());
        }
    }

    public synchronized void shutdown() {
        this.save();
        this.entries.clear();
        this.names.clear();
    }
}
