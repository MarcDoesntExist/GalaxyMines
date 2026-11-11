package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockItems;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;


public final class MineBulkOperations {
    private static final Map<Mine, BukkitTask> TASKS = new IdentityHashMap<>();

    private MineBulkOperations() {
    }


    public static boolean startClear(final Mine mine, final Runnable onComplete) {
        return startClear(mine, GalaxyMines.getInstance(), onComplete);
    }

    static boolean startClear(final Mine mine, final Plugin plugin, final Runnable onComplete) {
        final BlockData air = Material.AIR.createBlockData();
        return start(mine, plugin, block -> block.setBlockData(air, false), onComplete);
    }


    public static boolean startFill(final Mine mine, final ItemStack item,
                                    final Runnable onComplete) {
        return startFill(mine, GalaxyMines.getInstance(), item, onComplete);
    }

    static boolean startFill(final Mine mine, final Plugin plugin, final ItemStack item,
                             final Runnable onComplete) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        final ItemStack stack = item.clone();
        final String token = CustomBlockItems.tokenOf(stack);
        final BlockData fallback = stack.getType().isBlock()
                ? stack.getType().createBlockData()
                : Material.NOTE_BLOCK.createBlockData();
        return start(mine, plugin, block -> {
            if (token != null && CustomBlockRegistry.place(token, block.getLocation())) {
                return;
            }
            block.setBlockData(fallback, false);
        }, onComplete);
    }

    private static boolean start(final Mine mine, final Plugin plugin,
                                 final Consumer<Block> action, final Runnable onComplete) {
        if (mine == null || plugin == null || action == null) {
            return false;
        }
        if (TASKS.containsKey(mine)) {
            return false;
        }
        final PluginSettings.Bulk settings = PluginSettings.get().bulk;
        final Iterator<Block> iterator = mine.iterator();
        final BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int processed = 0;
            try {
                while (iterator.hasNext() && processed < settings.blocksPerTick) {
                    action.accept(iterator.next());
                    ++processed;
                }
                if (!iterator.hasNext()) {
                    TASKS.remove(mine);
                    taskRef[0].cancel();
                    runCallback(plugin, mine, onComplete);
                }
            } catch (RuntimeException ex) {
                TASKS.remove(mine);
                taskRef[0].cancel();
                plugin.getLogger().log(Level.SEVERE,
                        "Bulk operation failed for mine " + mine.getName(), ex);
            }
        }, settings.intervalTicks, settings.intervalTicks);
        TASKS.put(mine, taskRef[0]);
        return true;
    }

    private static void runCallback(final Plugin plugin, final Mine mine,
                                    final Runnable onComplete) {
        if (onComplete == null) {
            return;
        }
        try {
            onComplete.run();
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "Bulk operation callback failed for mine " + mine.getName(), ex);
        }
    }

    public static boolean isRunning(final Mine mine) {
        return mine != null && TASKS.containsKey(mine);
    }


    public static void cancel(final Mine mine) {
        if (mine == null) {
            return;
        }
        final BukkitTask task = TASKS.remove(mine);
        if (task != null) {
            task.cancel();
        }
    }

    public static void cancelAll() {
        for (final BukkitTask task : TASKS.values()) {
            task.cancel();
        }
        TASKS.clear();
    }
}
