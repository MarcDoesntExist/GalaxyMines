package me.marcdoesntexist.galaxymines;

import com.sk89q.worldedit.WorldEdit;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.marcdoesntexist.galaxymines.api.CustomPlugin;
import me.marcdoesntexist.galaxymines.commands.GMineCommand;
import me.marcdoesntexist.galaxymines.commands.MineCommandTree;
import me.marcdoesntexist.galaxymines.commands.SellCommand;
import me.marcdoesntexist.galaxymines.commands.SellCommandTree;
import me.marcdoesntexist.galaxymines.config.GalaxyMinesConfiguration;
import me.marcdoesntexist.galaxymines.config.MineConfig;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.events.MineEventManager;
import me.marcdoesntexist.galaxymines.gui.GUIManager;
import me.marcdoesntexist.galaxymines.holograms.CompatibleHologram;
import me.marcdoesntexist.galaxymines.holograms.HologramAdapter;
import me.marcdoesntexist.galaxymines.holograms.HologramChunkListener;
import me.marcdoesntexist.galaxymines.integration.WorldGuardIntegration;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockRegistry;
import me.marcdoesntexist.galaxymines.integration.economy.EconomyService;
import me.marcdoesntexist.galaxymines.integration.economy.PriceRegistry;
import me.marcdoesntexist.galaxymines.luckyblocks.LuckyBlockManager;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.*;
import me.marcdoesntexist.galaxymines.stats.PlayerStatsManager;
import me.marcdoesntexist.galaxymines.stats.StatsListener;
import me.marcdoesntexist.galaxymines.thread.PluginLoader;
import me.marcdoesntexist.galaxymines.utils.AsyncFileWriter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GalaxyMines extends CustomPlugin {
    public static File minesFolder;
    public static File storageFolder;
    public static HologramAdapter hologramAdapter;
    public static List<String> defaultHologramText;
    public static boolean convertMineResetLite;
    private BukkitTask countdownTask;
    private BukkitTask hologramTask;
    private MineBossBarManager bossBarManager;
    private MineWandManager wandManager;

    public static GalaxyMines getInstance() {
        return (GalaxyMines) GalaxyMines.instance;
    }

    public static FileConfiguration getYAMLMineFile(final Mine m) {
        return YamlConfiguration.loadConfiguration(getMineFile(m));
    }

    public static File getMineFile(final Mine m) {
        return new File(GalaxyMines.minesFolder.getPath(), m.getName() + ".yml");
    }

    public static boolean isForceHologramsUse() {
        return GalaxyMinesConfiguration.forceDefaultHolograms;
    }

    public static void setForceHologramsUse(final boolean forceHologramsUse) {
        GalaxyMinesConfiguration.forceDefaultHolograms = forceHologramsUse;
    }

    public MineWandManager getWandManager() {
        return this.wandManager;
    }

    public void startTimers() {
        cancelTimers();
        GalaxyMinesConfiguration.hologramRefreshRate = Math.max(20L,
                GalaxyMines.pluginConfig.getLong("settings.holograms.customHologramRefreshRate"));
        GalaxyMinesConfiguration.useHologramMineSync =
                GalaxyMines.pluginConfig.getBoolean("settings.holograms.useHologramResetSync");
        if (GalaxyMinesConfiguration.useHolograms && GalaxyMines.hologramAdapter == null) {
            GalaxyMines.instance.debug(
                    "No hologram adapter available: disabling holograms.", true);
            GalaxyMinesConfiguration.useHolograms = false;
        }
        if (GalaxyMinesConfiguration.useHolograms) {
            if (GalaxyMinesConfiguration.useHologramMineSync) {
                GalaxyMines.instance
                        .debug("Enabling synchronized hologram updates on mine reset.", 3);
                this.countdownTask = Bukkit.getScheduler().runTaskTimer(GalaxyMines.instance,
                        () -> {
                            for (final Mine mine : MineManager.getMines()) {
                                if (mine instanceof HologramCompatible) {
                                    ((HologramCompatible) mine).updateHolograms();
                                }
                                mine.secondCountdown();
                            }
                        }, 20L, 20L);
            } else {
                this.countdownTask = Bukkit.getScheduler().runTaskTimer(GalaxyMines.instance,
                        () -> {
                            for (final Mine mine : MineManager.getMines()) {
                                mine.secondCountdown();
                            }
                        }, 20L, 20L);
                GalaxyMines.instance.debug(
                        "Configuring hologram update rate @ " + GalaxyMinesConfiguration.hologramRefreshRate
                                + " refresh rate.", 3);
                this.hologramTask = Bukkit.getScheduler().runTaskTimer(GalaxyMines.instance,
                        () -> {
                            for (final Mine mine : MineManager.getMines()) {
                                if (mine instanceof HologramCompatible) {
                                    ((HologramCompatible) mine).updateHolograms();
                                }
                            }
                        }, GalaxyMinesConfiguration.hologramRefreshRate,
                        GalaxyMinesConfiguration.hologramRefreshRate);
            }
        } else {
            this.countdownTask = Bukkit.getScheduler().runTaskTimer(GalaxyMines.instance,
                    () -> {
                        for (final Mine mine : MineManager.getMines()) {
                            mine.secondCountdown();
                        }
                    }, 20L, 20L);
        }
    }

    private void cancelTimers() {
        if (this.countdownTask != null) {
            this.countdownTask.cancel();
            this.countdownTask = null;
        }
        if (this.hologramTask != null) {
            this.hologramTask.cancel();
            this.hologramTask = null;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        AsyncFileWriter.init(this.getLogger());
        if (!PluginLoader.load()) {
            this.getLogger().severe("GalaxyMines cannot start without WorldEdit.");
            return;
        }
        WorldEdit.getInstance().getEventBus().register(new MineManager.WorldEditManager());
        WorldGuardIntegration.load(this);
        CustomBlockRegistry.load(this);
        Bukkit.getPluginManager().registerEvents(new MineListener(), this);
        Bukkit.getPluginManager().registerEvents(new MineWorldListener(), this);
        Bukkit.getPluginManager().registerEvents(new HologramChunkListener(), this);
        Bukkit.getPluginManager().registerEvents(new StatsListener(), this);
        Bukkit.getPluginManager().registerEvents(
                new me.marcdoesntexist.galaxymines.bomb.BombListener(), this);
        me.marcdoesntexist.galaxymines.utils.TickMonitor.register(this);
        this.bossBarManager = new MineBossBarManager();
        Bukkit.getPluginManager().registerEvents(this.bossBarManager, this);
        this.bossBarManager.start();
        this.setupIntegrations();
        CustomPlugin.registerPermission("galaxymines.admin");
        CustomPlugin.registerPermission("galaxymines.bypass");
        CustomPlugin.registerPermission("galaxymines.mine.*");
        CustomPlugin.registerPermission("galaxymines.mine.*.break");
        CustomPlugin.registerPermission("galaxymines.tp", org.bukkit.permissions.PermissionDefault.TRUE);
        CustomPlugin.registerPermission("galaxymines.sell", org.bukkit.permissions.PermissionDefault.TRUE);
        CustomPlugin.registerPermission("galaxymines.stats", org.bukkit.permissions.PermissionDefault.TRUE);
        CustomPlugin.registerPermission("galaxymines.wand");
        CustomPlugin.registerPermission("galaxymines.events");
        CustomPlugin.registerPermission("galaxymines.bomb",
                org.bukkit.permissions.PermissionDefault.TRUE);
        CustomPlugin.registerPermission("galaxymines.autominer");
        this.registerCommands();
        this.debug("Enabling the GUI...", 3);
        GUIManager.setup();
        this.debug("GUI enabled", 3);
        this.debug("Enabling mines...", 3);
        Bukkit.getScheduler().runTaskLater(this, GalaxyMines.this::loadMines, 3L);
    }

    



    private void setupIntegrations() {
        final PluginSettings settings = PluginSettings.get();
        EconomyService.setup();
        PriceRegistry.load(new File(this.getDataFolder(), settings.economy.pricesFile));
        me.marcdoesntexist.galaxymines.multiplier.BoosterService.get().load(
                new File(this.getDataFolder(), "boosters.yml"), this.getLogger(),
                this.getConfig().getDouble("boosters.max-multiplier", 100.0D),
                this.getConfig().getInt("boosters.max-duration-seconds", 604800));
        final me.marcdoesntexist.galaxymines.multiplier.MultiplierSources multipliers =
                me.marcdoesntexist.galaxymines.multiplier.MultiplierSources.get();
        multipliers.configure(
                this.getConfig().getBoolean("integrations.multipliers.enabled", true),
                this.getConfig().getDouble("integrations.multipliers.drop-cap", 3.0D),
                this.getConfig().getDouble("integrations.multipliers.sell-cap", 3.0D),
                this.getConfig().getStringList("integrations.multipliers.disabled"),
                this.getLogger());
        if (Bukkit.getPluginManager().getPlugin("GalaxyRanks") != null) {
            final me.marcdoesntexist.galaxymines.multiplier.GalaxyRanksMultiplierSource ranks =
                    me.marcdoesntexist.galaxymines.multiplier.GalaxyRanksMultiplierSource
                            .resolve(this.getLogger());
            if (ranks != null) {
                multipliers.register(ranks);
            } else {
                this.getLogger().warning("GalaxyRanks is installed but its multiplier API could "
                        + "not be resolved; rank multipliers stay at 1.0.");
            }
        }
        me.marcdoesntexist.galaxymines.history.HistoryService.get().load(
                new File(this.getDataFolder(), "history.yml"), this.getLogger(),
                this.getConfig().getInt("history.max-entries", 50));
        me.marcdoesntexist.galaxymines.daily.DailyRewardService.get().load(
                this.getConfig().getConfigurationSection("daily-rewards"),
                new File(this.getDataFolder(), "dailyrewards-data.yml"), this.getLogger());
        me.marcdoesntexist.galaxymines.quest.QuestService.get().load(
                this.getConfig().getConfigurationSection("quests"),
                new File(this.getDataFolder(), "quests-data.yml"), this.getLogger());
        me.marcdoesntexist.galaxymines.pass.BattlePassService.get().load(
                this.getConfig().getConfigurationSection("battle-pass"),
                new File(this.getDataFolder(), "battlepass-data.yml"), this.getLogger());
        me.marcdoesntexist.galaxymines.bomb.BombService.get().load(
                this.getConfig().getConfigurationSection("bombs"));
        me.marcdoesntexist.galaxymines.autominer.AutoMinerService.get().load(
                this.getConfig().getConfigurationSection("auto-miner"),
                new File(this.getDataFolder(), "autominer-data.yml"), this.getLogger());
        me.marcdoesntexist.galaxymines.autominer.AutoMinerService.get().start();
        MineDropProcessor.buildRecipeCache();
        MineEventManager.get().load(new File(this.getDataFolder(), settings.events.file),
                settings.events.enabled);
        LuckyBlockManager.get().load(new File(this.getDataFolder(), settings.luckyBlocks.file),
                settings.luckyBlocks.enabled);
        saveResource("milestones.yml", false);
        me.marcdoesntexist.galaxymines.milestone.MilestoneService.get().load(
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                        new File(this.getDataFolder(), "milestones.yml")),
                new File(this.getDataFolder(), "milestones-data.yml"), this.getLogger());
        PlayerStatsManager.init();
        me.marcdoesntexist.galaxymines.integration.placeholder.PlaceholderIntegration.setup(this);
        
        this.wandManager = new MineWandManager();
        Bukkit.getPluginManager().registerEvents(this.wandManager, this);
        this.wandManager.start();
    }

    private void stopIntegrations() {
        TeleportService.cancelAll();
        if (this.wandManager != null) {
            HandlerList.unregisterAll(this.wandManager);
            this.wandManager.stop();
            this.wandManager = null;
        }
        MineEventManager.get().stop();
        me.marcdoesntexist.galaxymines.multiplier.BoosterService.get().shutdown();
        me.marcdoesntexist.galaxymines.history.HistoryService.get().shutdown();
        me.marcdoesntexist.galaxymines.daily.DailyRewardService.get().shutdown();
        me.marcdoesntexist.galaxymines.quest.QuestService.get().shutdown();
        me.marcdoesntexist.galaxymines.pass.BattlePassService.get().shutdown();
        me.marcdoesntexist.galaxymines.bomb.BombService.get().clearCooldowns();
        me.marcdoesntexist.galaxymines.autominer.AutoMinerService.get().shutdown();
        LuckyBlockManager.get().unload();
        me.marcdoesntexist.galaxymines.integration.placeholder.PlaceholderIntegration.unload();
        me.marcdoesntexist.galaxymines.milestone.MilestoneService.get().shutdown();
        PlayerStatsManager.shutdown();
    }

    @Override
    public void onDisable() {
        if (this.bossBarManager != null) {
            this.bossBarManager.stop();
            this.bossBarManager = null;
        }
        this.stopIntegrations();
        
        MineManager.saveAll();
        AsyncFileWriter.flush();
        WorldGuardIntegration.unload();
        CustomBlockRegistry.unload();
        this.unloadEverything();
    }

    



    private void registerCommands() {
        final GMineCommand command = new GMineCommand();
        final SellCommand sellCommand = new SellCommand();
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(MineCommandTree.build(command),
                    List.of("gmine", "gmines", "galaxymine", "galaxymines", "mines"));
            for (final var node : SellCommandTree.build(sellCommand)) {
                event.registrar().register(node);
            }
        });
    }

    @Override
    public void reload(final Player p) {
        if (GalaxyMines.chatManager != null && p != null) {
            GalaxyMines.chatManager.sendMessage(p,
                    Messages.get("messages.command.reloading", "Reloading configuration..."));
        }
        this.debug("Cancelling mine resets...");
        cancelTimers();
        this.stopIntegrations();
        this.debug("Unloading the previous state...");
        this.unloadEverything();
        if (!PluginLoader.load()) {
            if (p != null) {
                GalaxyMines.chatManager.sendMessage(p,
                        Messages.get("messages.command.reloadFailed",
                                "Reload failed: WorldEdit is not available."));
            }
            return;
        }
        GUIManager.setup();
        WorldGuardIntegration.load(this);
        CustomBlockRegistry.load(this);
        this.setupIntegrations();
        this.loadMines();
        this.debug("Reload complete");
        if (GalaxyMines.chatManager != null && p != null) {
            GalaxyMines.chatManager.sendMessage(p,
                    Messages.get("messages.command.reloaded", "Configuration reloaded."));
        }
    }

    public void loadMines() {
        this.debug("Loading mines", true);
        this.debug();
        if (GalaxyMines.minesFolder == null || !GalaxyMines.minesFolder.isDirectory()) {
            this.debug("The mines folder does not exist: no mines loaded.", true);
            return;
        }
        final File[] mineFiles = GalaxyMines.minesFolder.listFiles();
        if (mineFiles == null) {
            this.debug("Could not read the mines folder.", true);
            return;
        }
        Arrays.sort(mineFiles, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        int i = 0;
        for (final File file : mineFiles) {
            if (!file.isFile()) {
                continue;
            }
            final String filename = file.getName();
            this.debug("-------------------------------", 3);
            this.debug("Trying to load " + filename + "...", 3);
            final int dot = filename.lastIndexOf('.');
            final String extension = dot < 0 ? "" : filename.substring(dot + 1);
            if (!"yml".equalsIgnoreCase(extension)) {
                this.debug(filename + " is not a supported file and will not be loaded.", true);
                continue;
            }
            try {
                final FileConfiguration mine = YamlConfiguration.loadConfiguration(file);
                final String name = mine.getString("name");
                if (!mine.getBoolean("enabled")) {
                    this.debug("Mine " + name + " is disabled.", 3);
                    continue;
                }
                this.debug("Loading...");
                final MineConfig m = new MineConfig(mine);
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                    try {
                        MineManager.loadMine(m);
                        this.debug("Loaded mine " + m.getName() + ".", true);
                        this.debug();
                    } catch (Throwable t) {
                        this.printException(
                                "Something went wrong while loading file " + file.getName()
                                        + ". Check your config.", t);
                    }
                }, 5L);
                ++i;
            } catch (Throwable t) {
                this.printException(
                        "Something went wrong while loading file " + file.getName()
                                + ". Check your config.", t);
            }
        }
        this.debug("Scheduled " + i + " mines", true);
    }

    private void unloadEverything() {
        MineBulkOperations.cancelAll();
        
        
        MineRepopulator.cancelAll();
        InfiniteRespawnService.clearAll();
        for (final Mine mine : MineManager.getMines()) {
            if (!(mine instanceof HologramCompatible compatible)) {
                continue;
            }
            for (final CompatibleHologram hologram : compatible.getHolograms()) {
                try {
                    hologram.delete();
                } catch (Throwable ignored) {
                    
                }
            }
            compatible.getHolograms().clear();
        }
        MineManager.unregisterMines();
        GalaxyMines.pluginConfig = null;
        GalaxyMines.pluginFolder = null;
        GalaxyMinesConfiguration.messagesConfig = null;
        GalaxyMinesConfiguration.guiConfig = null;
        GalaxyMinesConfiguration.useHolograms = false;
        GalaxyMines.hologramAdapter = null;
        cancelTimers();
    }

    @Override
    public String getPluginName() {
        return "GalaxyMines";
    }

    @Override
    public FileConfiguration getMessagesConfig() {
        return GalaxyMinesConfiguration.messagesConfig;
    }
}
