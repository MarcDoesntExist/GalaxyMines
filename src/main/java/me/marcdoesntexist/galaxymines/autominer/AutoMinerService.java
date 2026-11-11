package me.marcdoesntexist.galaxymines.autominer;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.mines.MineDropProcessor;
import me.marcdoesntexist.galaxymines.mines.MineManager;
import me.marcdoesntexist.galaxymines.stats.PlayerStatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public final class AutoMinerService {
    private static final AutoMinerService INSTANCE = new AutoMinerService();

    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private AutoMinerSettings settings = AutoMinerSettings.defaults();
    private File file;
    private Logger logger = Logger.getLogger("GalaxyMines");
    private BukkitTask task;

    private AutoMinerService() {
    }

    public static AutoMinerService get() {
        return INSTANCE;
    }

    public synchronized void load(final ConfigurationSection config, final File dataFile,
                                  final Logger logger) {
        if (logger != null) {
            this.logger = logger;
        }
        this.file = dataFile;
        this.settings = AutoMinerSettings.of(config);
        this.loadPlayers();
    }

    public synchronized AutoMinerSettings settings() {
        return this.settings;
    }

    public boolean isActive(final UUID uuid) {
        return uuid != null && this.active.contains(uuid);
    }


    public synchronized boolean toggle(final Player player) {
        final UUID uuid = player.getUniqueId();
        final boolean enabled;
        if (this.active.remove(uuid)) {
            enabled = false;
        } else {
            this.active.add(uuid);
            enabled = true;
        }
        this.save();
        return enabled;
    }

    public synchronized void stop(final UUID uuid) {
        if (uuid != null && this.active.remove(uuid)) {
            this.save();
        }
    }


    public synchronized boolean setActive(final Player player, final boolean enabled) {
        if (player == null) {
            return false;
        }
        final boolean changed = enabled
                ? this.active.add(player.getUniqueId())
                : this.active.remove(player.getUniqueId());
        if (changed) {
            this.save();
        }
        return this.active.contains(player.getUniqueId());
    }

    public synchronized void start() {
        this.stopTask();
        if (!this.settings.enabled()) {
            return;
        }
        final GalaxyMines plugin = GalaxyMines.getInstance();
        if (plugin == null) {
            return;
        }
        final long ticks = this.settings.intervalSeconds() * 20L;
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, ticks, ticks);
    }

    public synchronized void shutdown() {
        this.stopTask();
        this.save();
        this.active.clear();
    }

    private void stopTask() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private void tick() {
        if (!this.settings.enabled() || this.active.isEmpty()) {
            return;
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (!this.active.contains(player.getUniqueId())) {
                continue;
            }
            if (!player.hasPermission("galaxymines.autominer")) {
                continue;
            }
            final Mine mine = MineManager.getMineAt(player.getLocation());
            if (mine == null || mine.isResetInProgress()) {
                continue;
            }
            this.mineAround(player, mine);
        }
    }

    private void mineAround(final Player player, final Mine mine) {
        final int radius = this.settings.radius();
        final Location center = player.getLocation();
        final List<Block> candidates = new ArrayList<>();
        for (int x = -radius; x <= radius; ++x) {
            for (int y = -radius; y <= radius; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    final Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType().isAir()) {
                        continue;
                    }
                    if (MineManager.getMineAt(block) != mine) {
                        continue;
                    }
                    candidates.add(block);
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        Collections.shuffle(candidates);
        int mined = 0;
        for (final Block block : candidates) {
            if (mined >= this.settings.maxBlocksPerCycle()) {
                break;
            }
            if (!this.breakBlock(player, mine, block, center)) {
                continue;
            }
            ++mined;
        }
    }

    private boolean breakBlock(final Player player, final Mine mine, final Block block,
                               final Location center) {
        if (center == null) {
            return false;
        }
        if (block.getType().isAir()) {
            return false;
        }
        final List<ItemStack> drops = MineDropProcessor.capture(block, player);
        if (!MineDropProcessor.canAccept(player, mine, drops)) {
            return false;
        }
        block.setBlockData(Material.AIR.createBlockData(), false);
        mine.setBlockAsBroken(block);
        PlayerStatsManager.addMined(player, mine);
        MineDropProcessor.deliver(player, mine, block.getLocation().add(0.5D, 0.5D, 0.5D), drops);
        return true;
    }

    private void loadPlayers() {
        this.active.clear();
        if (this.file == null || !this.file.isFile()) {
            return;
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);
        for (final String raw : config.getStringList("players")) {
            try {
                this.active.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void save() {
        if (this.file == null) {
            return;
        }
        final YamlConfiguration config = new YamlConfiguration();
        config.set("players", this.active.stream().map(UUID::toString).toList());
        try {
            final File parent = this.file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                this.logger.warning("Could not create the auto-miner folder " + parent);
                return;
            }
            config.save(this.file);
        } catch (IOException exception) {
            this.logger.warning("Could not save autominer-data.yml: "
                    + exception.getMessage());
        }
    }
}
