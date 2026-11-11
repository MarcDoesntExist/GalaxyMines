package me.marcdoesntexist.galaxymines.pass;

import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.integration.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public final class BattlePassService {
    private static final BattlePassService INSTANCE = new BattlePassService();

    private final Map<Integer, BattlePassTier> tiers = new TreeMap<>();
    private final Map<UUID, PlayerState> players = new ConcurrentHashMap<>();
    private File file;
    private Logger logger = Logger.getLogger("GalaxyMines");
    private boolean enabled;
    private String season = "1";
    private long xpPerBlock = 1L;
    private long xpPerItemSold = 1L;
    private long lastSaveAt;

    private BattlePassService() {
    }

    public static BattlePassService get() {
        return INSTANCE;
    }


    public synchronized void load(final ConfigurationSection config, final File dataFile,
                                  final Logger logger) {
        if (logger != null) {
            this.logger = logger;
        }
        this.file = dataFile;
        this.tiers.clear();
        this.enabled = config != null && config.getBoolean("enabled", true);
        this.season = config == null ? "1" : config.getString("season", "1");
        this.xpPerBlock = config == null
                ? 1L : Math.max(0L, config.getLong("xp-per-block", 1L));
        this.xpPerItemSold = config == null
                ? 1L : Math.max(0L, config.getLong("xp-per-item-sold", 1L));
        if (config != null) {
            final ConfigurationSection entries = config.getConfigurationSection("tiers");
            if (entries != null) {
                for (final String key : entries.getKeys(false)) {
                    final int level;
                    try {
                        level = Integer.parseInt(key.trim());
                    } catch (NumberFormatException exception) {
                        this.logger.warning("Battle pass tier '" + key
                                + "' is not a number; skipped.");
                        continue;
                    }
                    final ConfigurationSection section = entries.getConfigurationSection(key);
                    if (section == null) {
                        continue;
                    }
                    this.tiers.put(level, new BattlePassTier(level,
                            Math.max(1L, section.getLong("xp", 1L)),
                            Math.max(0.0D, section.getDouble("money", 0.0D)),
                            section.getStringList("commands")));
                }
            }
        }
        this.loadProgress();
    }

    public synchronized boolean isEnabled() {
        return this.enabled && !this.tiers.isEmpty();
    }

    public synchronized String season() {
        return this.season;
    }

    public synchronized long xpPerBlock() {
        return this.xpPerBlock;
    }

    public synchronized long xpPerItemSold() {
        return this.xpPerItemSold;
    }

    public synchronized List<BattlePassTier> tiers() {
        return List.copyOf(this.tiers.values());
    }


    public synchronized long addXp(final UUID uuid, final long amount) {
        if (!this.isEnabled() || uuid == null || amount <= 0L) {
            return this.xp(uuid);
        }
        final PlayerState state = this.state(uuid);
        state.xp = Math.min(Long.MAX_VALUE - amount, state.xp + amount);
        final long now = System.currentTimeMillis();
        if (now - this.lastSaveAt >= 15_000L) {
            this.lastSaveAt = now;
            this.save();
        }
        return state.xp;
    }

    public synchronized long xp(final UUID uuid) {
        final PlayerState state = uuid == null ? null : this.players.get(uuid);
        return state == null ? 0L : state.xp;
    }


    public synchronized int level(final UUID uuid) {
        final long xp = this.xp(uuid);
        int level = 0;
        for (final BattlePassTier tier : this.tiers.values()) {
            if (xp < tier.xp()) {
                break;
            }
            level = tier.level();
        }
        return level;
    }


    public synchronized List<BattlePassTier> claim(final UUID uuid) {
        final List<BattlePassTier> claimed = new ArrayList<>();
        if (!this.isEnabled() || uuid == null) {
            return claimed;
        }
        final PlayerState state = this.state(uuid);
        for (final BattlePassTier tier : this.tiers.values()) {
            if (state.xp < tier.xp() || !state.claimed.add(tier.level())) {
                continue;
            }
            claimed.add(tier);
        }
        if (!claimed.isEmpty()) {
            this.save();
        }
        return claimed;
    }


    public void claim(final Player player) {
        if (player == null) {
            return;
        }
        for (final BattlePassTier tier : this.claim(player.getUniqueId())) {
            if (tier.money() > 0.0D) {
                EconomyService.deposit(player, tier.money());
            }
            for (final String raw : tier.commands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        raw.replace("%player%", player.getName())
                                .replace("%uuid%", player.getUniqueId().toString()));
            }
            player.sendMessage(Text.parse("<green>Battle pass tier <yellow>" + tier.level()
                    + " <green>claimed: <white>" + EconomyService.format(tier.money())
                    + " <green>and its rewards."));
        }
    }


    public synchronized List<TierStatus> status(final UUID uuid) {
        final PlayerState state = uuid == null ? null : this.players.get(uuid);
        final long xp = state == null ? 0L : state.xp;
        final List<TierStatus> statuses = new ArrayList<>();
        for (final BattlePassTier tier : this.tiers.values()) {
            statuses.add(new TierStatus(tier, xp >= tier.xp(),
                    state != null && state.claimed.contains(tier.level())));
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
        config.set("season", this.season);
        for (final Map.Entry<UUID, PlayerState> entry : this.players.entrySet()) {
            final String path = "players." + entry.getKey();
            config.set(path + ".xp", entry.getValue().xp);
            config.set(path + ".claimed", new ArrayList<>(entry.getValue().claimed));
        }
        try {
            final File parent = this.file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                this.logger.warning("Could not create the battle pass folder " + parent);
                return;
            }
            config.save(this.file);
        } catch (IOException exception) {
            this.logger.warning("Could not save battlepass-data.yml: "
                    + exception.getMessage());
        }
    }

    public synchronized void shutdown() {
        this.save();
        this.players.clear();
    }

    private PlayerState state(final UUID uuid) {
        return this.players.computeIfAbsent(uuid, ignored -> new PlayerState());
    }

    private void loadProgress() {
        this.players.clear();
        if (this.file == null || !this.file.isFile()) {
            return;
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);
        if (!this.season.equals(config.getString("season", this.season))) {
            return;
        }
        final ConfigurationSection root = config.getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (final String raw : root.getKeys(false)) {
            final UUID uuid;
            try {
                uuid = UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            final PlayerState state = new PlayerState();
            state.xp = Math.max(0L, root.getLong(raw + ".xp", 0L));
            state.claimed.addAll(root.getIntegerList(raw + ".claimed"));
            this.players.put(uuid, state);
        }
    }

    public record TierStatus(BattlePassTier tier, boolean unlocked, boolean claimed) {
    }

    private static final class PlayerState {
        private final Set<Integer> claimed = new LinkedHashSet<>();
        private long xp;
    }
}
