package me.marcdoesntexist.galaxymines.api;

import me.marcdoesntexist.galaxymines.api.chat.ChatManager;
import me.marcdoesntexist.galaxymines.api.chat.ColorDesign;
import me.marcdoesntexist.galaxymines.api.chat.MessageManager;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public abstract class CustomPlugin extends JavaPlugin implements Listener {
    public static CustomPlugin instance;
    public static ChatManager chatManager;
    public static MessageManager messageManager;
    public static ColorDesign defaultColorDesign;
    public static File pluginFolder;
    public static File pluginConfigFile;
    public static FileConfiguration pluginConfig;
    public int minimumDebugPriotity;
    private boolean allowBroadcastdebug;

    public CustomPlugin() {
        this.minimumDebugPriotity = 0;
    }

    public static boolean isPermissionRegistered(final String permission) {
        for (final Permission p : Bukkit.getPluginManager().getPermissions()) {
            if (p.getName().equalsIgnoreCase(permission)) {
                return true;
            }
        }
        return false;
    }

    public static void registerPermission(final String permission) {
        registerPermission(permission, PermissionDefault.OP);
    }

    public static void registerPermission(final String permission, final PermissionDefault defaultValue) {
        if (!isPermissionRegistered(permission)) {
            Bukkit.getPluginManager()
                    .addPermission(new Permission(permission, "GalaxyMines command permission", defaultValue));
        }
    }

    public static void registerListener(final Listener l) {
        Bukkit.getPluginManager().registerEvents(l, CustomPlugin.instance);
        CustomPlugin.instance
                .debug("Listener " + l.getClass().getSimpleName() + "@" + l.hashCode() + " registered.",
                        2);
    }

    public static void unregisterListener(final Listener l) {
        HandlerList.unregisterAll(l);
    }

    public abstract String getPluginName();

    public abstract FileConfiguration getMessagesConfig();

    public abstract void reload(final Player p0);

    public void onEnable() {
        CustomPlugin.instance = this;
        CustomPlugin.pluginFolder = this.getDataFolder();
        if (!CustomPlugin.pluginFolder.exists()) {
            this.debug("Creating the plugin folder...", 3);
            CustomPlugin.pluginFolder.mkdir();
        }
        CustomPlugin.pluginConfigFile = new File(CustomPlugin.pluginFolder, "config.yml");
        if (!CustomPlugin.pluginConfigFile.exists()) {
            this.debug("Creating the config file...", 3);
            this.loadResource(CustomPlugin.pluginConfigFile, "config.yml");
        }
        CustomPlugin.pluginConfig =
                YamlConfiguration.loadConfiguration(CustomPlugin.pluginConfigFile);
    }

    public FileConfiguration loadConfig() {
        final FileConfiguration fc = this.getConfig();
        this.saveConfig();
        return fc;
    }

    public void loadResource(final File config, final String resource) {
        if (!config.exists()) {
            this.saveResource(resource, true);
        }
        YamlConfiguration.loadConfiguration(config);
    }

    public void broadcastDebug(final String msg) {
        this.broadcastDebug(msg, this.minimumDebugPriotity);
    }

    public void broadcastDebug(final String msg, final int priority) {
        if (this.allowBroadcastdebug) {
            if (priority >= this.minimumDebugPriotity) {
                final StackTraceElement[] stackTraceElements =
                        Thread.currentThread().getStackTrace();
                final StackTraceElement e = stackTraceElements[3];
                Bukkit.broadcast(Text.parse("<red><bold>" + this.getPluginName()
                        + "<red><bold>Debug<gray>-<yellow>" + e.getClassName() + "@" + e.getMethodName()
                        + "<italic>(" + e.getLineNumber() + ") <gold><bold>> <gray>" + msg));
            }
        } else {
            this.debug(msg, priority);
        }
    }

    public void debug() {
        this.debug(" ");
    }

    public void debug(final String[] msg) {
        for (final String m : msg) {
            this.debug(m);
        }
    }

    public void debug(final String[] msg, final int priority) {
        for (final String m : msg) {
            this.debug(m, priority);
        }
    }

    public void debug(final String msg) {
        this.debug(msg, 0);
    }

    public void debug(final String msg, final int priority) {
        if (priority >= this.minimumDebugPriotity) {
            this.getLogger().info(msg);
        }
    }

    public void debug(final String msg, final boolean force) {
        if (force) {
            this.getLogger().info(msg);
        } else {
            this.debug(msg);
        }
    }

    public void printException(final String msg, final Throwable t) {
        this.getLogger().log(Level.SEVERE, msg, t);
    }
}
