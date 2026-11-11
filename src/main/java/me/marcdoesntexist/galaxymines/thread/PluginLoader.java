package me.marcdoesntexist.galaxymines.thread;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.ColorDesign;
import me.marcdoesntexist.galaxymines.api.chat.PluginChatManager;
import me.marcdoesntexist.galaxymines.config.GalaxyMinesConfiguration;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.holograms.impl.DisplayHologramAdapter;
import me.marcdoesntexist.galaxymines.messages.GalaxyMineMessageManager;
import me.marcdoesntexist.galaxymines.messages.LegacyMessagesMigration;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.utils.AsyncFileWriter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class PluginLoader {
    private static final GalaxyMines plugin;

    static {
        plugin = GalaxyMines.getInstance();
    }

    public static boolean load() {
        final long startms = System.currentTimeMillis();
        PluginLoader.plugin.debug("Loading config...");
        setupConfig();
        PluginLoader.plugin.debug("Checking dependencies...");
        if (!setupDependencies()) {
            return false;
        }
        PluginLoader.plugin.debug("Loading plugin...");
        setupPlugin();
        final long endms = System.currentTimeMillis();
        final long startupTime = endms - startms;
        PluginLoader.plugin.debug(
                "The plugin loaded in " + startupTime / 1000L + " seconds (" + startupTime
                        + "ms)!", true);
        return true;
    }

    private static void setupConfig() {
        GalaxyMines.pluginFolder = PluginLoader.plugin.getDataFolder();
        ensureFolder(GalaxyMines.pluginFolder, "plugin folder");
        final File pluginconfig = new File(PluginLoader.plugin.getDataFolder(), "config.yml");
        if (!pluginconfig.exists()) {
            PluginLoader.plugin.debug("Creating config.yml...", 3);
            PluginLoader.plugin.loadResource(pluginconfig, "config.yml");
        }
        GalaxyMines.pluginConfig = YamlConfiguration.loadConfiguration(pluginconfig);

        GalaxyMines.pluginConfig = me.marcdoesntexist.galaxymines.config.ConfigMigration.apply(
                pluginconfig, GalaxyMines.pluginConfig, PluginLoader.plugin.getLogger());
        PluginSettings.load(GalaxyMines.pluginConfig);
        final File guiconfig = new File(PluginLoader.plugin.getDataFolder(), "Menu.yml");
        if (!guiconfig.exists()) {
            PluginLoader.plugin.debug("Creating Menu.yml...", 3);
            PluginLoader.plugin.loadResource(guiconfig, "Menu.yml");
        }
        GalaxyMinesConfiguration.guiConfig = YamlConfiguration.loadConfiguration(guiconfig);
        MenuConfig.load(GalaxyMinesConfiguration.guiConfig);
        PluginLoader.plugin.minimumDebugPriotity =
                GalaxyMines.pluginConfig.getInt("settings.minimumDebugLevel");


        GalaxyMines.storageFolder = new File(GalaxyMines.pluginFolder, "storage");
        ensureFolder(GalaxyMines.storageFolder, "mine storage");
        GalaxyMines.minesFolder = new File(GalaxyMines.pluginFolder, "mines");
        ensureFolder(GalaxyMines.minesFolder, "mines folder");
        ensureResource(new File(GalaxyMines.pluginFolder, "prices.yml"), "prices.yml");
        ensureResource(new File(GalaxyMines.pluginFolder, "events.yml"), "events.yml");
        ensureResource(new File(GalaxyMines.pluginFolder, "luckyblocks.yml"), "luckyblocks.yml");

        final File messages = new File(PluginLoader.plugin.getDataFolder(), "Messages.yml");
        if (!messages.exists()) {
            PluginLoader.plugin.debug("Creating Messages.yml...", 3);
            PluginLoader.plugin.loadResource(messages, "Messages.yml");
        }
        LegacyMessagesMigration.migrate(messages);
        GalaxyMines.defaultHologramText =
                GalaxyMines.pluginConfig.getStringList("settings.holograms.defaultHologramText");
        GalaxyMinesConfiguration.messagesConfig = YamlConfiguration.loadConfiguration(messages);
        Messages.load(GalaxyMinesConfiguration.messagesConfig);

        GalaxyMinesConfiguration.forceDefaultBroadcastMessage =
                GalaxyMines.pluginConfig.getBoolean("global.forceDefaultBroadcastMessage", true);
        GalaxyMines.convertMineResetLite =
                GalaxyMines.pluginConfig.getBoolean("global.convertFromMineResetLite");
    }

    private static void ensureFolder(final File folder, final String description) {
        if (folder != null && !folder.exists() && !folder.mkdirs()) {
            PluginLoader.plugin
                    .debug("Could not create the " + description + " (" + folder + ")", true);
        }
    }


    private static void ensureResource(final File file, final String resource) {
        if (!file.exists()) {
            PluginLoader.plugin.debug("Creating " + resource + "...", 3);
            PluginLoader.plugin.loadResource(file, resource);
        }
    }

    public static boolean setupDependencies() {
        GalaxyMines.hologramAdapter = null;

        final Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEdit == null || !worldEdit.isEnabled() || !(worldEdit instanceof WorldEditPlugin)) {
            PluginLoader.plugin.debug("WorldEdit was not found or is not enabled.", true);
            PluginLoader.plugin.debug("The plugin will be disabled", true);
            Bukkit.getPluginManager().disablePlugin(PluginLoader.plugin);
            return false;
        }

        GalaxyMinesConfiguration.useHolograms =
                GalaxyMines.pluginConfig.getBoolean("settings.holograms.enableHolograms");
        GalaxyMinesConfiguration.forceDefaultHolograms =
                GalaxyMines.pluginConfig.getBoolean("settings.holograms.forceDefaultHologramOnAllMines",
                        GalaxyMines.pluginConfig.getBoolean("global.forceDefaultHologramOnAllMines"));
        if (GalaxyMinesConfiguration.useHolograms) {

            GalaxyMines.hologramAdapter = new DisplayHologramAdapter();
            PluginLoader.plugin.debug("Using the plugin's built-in holograms.", 3);
            if (GalaxyMinesConfiguration.forceDefaultHolograms) {
                PluginLoader.plugin.debug("Forcing all holograms to use default preset.", 3);
            }
        }
        return true;
    }

    private static void savePluginConfig() {
        AsyncFileWriter.write(new File(PluginLoader.plugin.getDataFolder(), "config.yml"),
                GalaxyMines.pluginConfig.saveToString());
    }

    private static void setupPlugin() {
        if (GalaxyMinesConfiguration.useHolograms) {

            me.marcdoesntexist.galaxymines.holograms.OrphanCleanupTask.get().runOnce();
            final int minutes =
                    me.marcdoesntexist.galaxymines.config.PluginSettings.get()
                            .holograms.orphanCleanupMinutes;
            if (me.marcdoesntexist.galaxymines.holograms.OrphanCleanupTask.shouldRun(minutes)) {
                final long ticks = minutes * 60L * 20L;
                org.bukkit.Bukkit.getScheduler().runTaskTimer(PluginLoader.plugin,
                        () -> me.marcdoesntexist.galaxymines.holograms.OrphanCleanupTask.get()
                                .runOnce(), ticks, ticks);
            }
        }
        GalaxyMines.minesFolder = new File(PluginLoader.plugin.getDataFolder(), "mines");
        ensureFolder(GalaxyMines.minesFolder, "mines folder");
        if (GalaxyMinesConfiguration.useHolograms && GalaxyMines.hologramAdapter == null) {
            PluginLoader.plugin
                    .debug("The plugin could not find hologram support...", true);
            GalaxyMines.pluginConfig.set("settings.holograms.enableHolograms", false);
            GalaxyMinesConfiguration.useHolograms = false;
            savePluginConfig();
        }

        GalaxyMines.defaultColorDesign = new ColorDesign('a', 'e', '7', 'c');
        GalaxyMines.chatManager =
                (PluginChatManager) PluginChatManager.getInstance(PluginLoader.plugin).setup();
        GalaxyMines.messageManager =
                (GalaxyMineMessageManager) GalaxyMineMessageManager.getInstance().setup();
        PluginLoader.plugin.debug();
        PluginLoader.plugin.debug("Starting timer...", 2);
        PluginLoader.plugin.startTimers();
        PluginLoader.plugin.debug("Timer started!", 2);
    }
}
