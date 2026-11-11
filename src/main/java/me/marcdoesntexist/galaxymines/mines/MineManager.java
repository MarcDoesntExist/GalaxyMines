package me.marcdoesntexist.galaxymines.mines;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.config.GalaxyMinesConfiguration;
import me.marcdoesntexist.galaxymines.config.MineConfig;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.events.mine.MineLoadEvent;
import me.marcdoesntexist.galaxymines.integration.WorldGuardIntegration;
import me.marcdoesntexist.galaxymines.messages.GalaxyMineMessageManager;
import me.marcdoesntexist.galaxymines.mines.types.CuboidMine;
import me.marcdoesntexist.galaxymines.utils.AsyncFileWriter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MineManager {



    public static final long MAX_MINE_VOLUME = 5_000_000L;
    private static final ArrayList<Mine> MINES;
    private static final GalaxyMines GALAXY_MINES;
    private static final Map<java.util.UUID, List<String>> SUSPENDED_WORLDS = new HashMap<>();

    static {
        MINES = new ArrayList<>();
        GALAXY_MINES = GalaxyMines.getInstance();
    }

    public static ArrayList<Mine> getMines() {
        return new ArrayList<>(MineManager.MINES);
    }

    public static boolean isNameAvailable(final String name) {
        for (final Mine mine : MineManager.MINES) {
            if (mine.getName().equalsIgnoreCase(name)) {
                return false;
            }
        }
        return true;
    }





    public static boolean isValidName(final String name) {
        if (name == null || name.isBlank()
                || name.length() > Math.max(1, PluginSettings.get().maxMineNameLength)) {
            return false;
        }
        for (int i = 0; i < name.length(); ++i) {
            final char character = name.charAt(i);
            if (!((character >= 'a' && character <= 'z')
                    || (character >= 'A' && character <= 'Z')
                    || (character >= '0' && character <= '9')
                    || character == '_' || character == '-')) {
                return false;
            }
        }
        return true;
    }

    public static Mine loadMine(final MineConfig c) {
        if (!isNameAvailable(c.getName())) {
            throw new IllegalStateException("A mine named " + c.getName() + " is already loaded");
        }
        final Mine m = MineConfig.loadMine(c);
        if (m == null) {
            throw new IllegalArgumentException(
                    "Unsupported mine type in configuration for " + c.getName());
        }
        if (GalaxyMinesConfiguration.useHolograms && m instanceof HologramCompatible) {
            GalaxyMines.instance.debug("Creating hologram for mine " + m.getName(), 3);
            try {
                ((HologramCompatible) m).setupHolograms();
            } catch (Throwable t) {
                GalaxyMines.instance.printException(
                        "Could not create holograms for mine " + m.getName(), t);
            }
        }
        registerMine(m);
        if (!m.restoreState()) {
            m.reset();
        }
        new MineLoadEvent(m).call();
        return m;
    }

    public static void registerMine(final Mine m) {
        MineManager.MINES.add(m);
        MineAccess.register(m);
        MineIndex.add(m);
        WorldGuardIntegration.onMineRegistered(m);
        MineManager.GALAXY_MINES.debug("Mine " + m.getName() + " registered");
    }

    public static void unregisterMines() {
        for (final Mine mine : MineManager.MINES) {
            MineAccess.unregister(mine.getName());
        }
        MineIndex.clear();
        MineTracker.clearAll();
        MineManager.MINES.clear();
        GalaxyMines.getInstance().debug("All mines have been disabled");
    }

    public static void unregisterMine(final Mine mine) {
        MineTracker.clear(mine);
        MineAccess.unregister(mine.getName());
        MineIndex.remove(mine);
        MineManager.MINES.remove(mine);
        GalaxyMines.getInstance().debug("Mine " + mine.getName() + " has been disabled");
    }

    public static Mine getMine(final String name) {
        for (final Mine mine : MineManager.MINES) {
            if (mine.getName().equalsIgnoreCase(name)) {
                return mine;
            }
        }
        return null;
    }

    public static boolean isPlayerInAMine(final Player p) {
        return getMineWith(p) != null;
    }

    public static Mine getMineWith(final Player p) {
        return MineIndex.getMineAt(p.getLocation());
    }

    public static Mine getMineAt(final Block block) {
        return MineIndex.getMineAt(block.getLocation());
    }

    public static Mine getMineAt(final org.bukkit.Location location) {
        return MineIndex.getMineAt(location);
    }

    public static void sendInfo(final Player p, final Mine m) {
        GalaxyMines.chatManager
                .sendMessage(p, GalaxyMineMessageManager.getInstance().translateAll(m.getInfo(), m));
    }

    private static void notifyBlockChange(final Block b) {
        MineIndex.forEachMineAt(b.getLocation(), mine -> mine.setBlockAsBroken(b));
    }

    public static boolean isPartOfMine(final Block b) {
        return MineIndex.getMineAt(b.getLocation()) != null;
    }

    public static void saveAll() {
        for (final Mine mine : MineManager.MINES) {
            mine.save();
        }
    }

    public static int suspendedWorldCount() {
        return SUSPENDED_WORLDS.size();
    }

    public static int suspendedMineCount() {
        int count = 0;
        for (final List<String> mines : SUSPENDED_WORLDS.values()) {
            count += mines.size();
        }
        return count;
    }




    public static boolean renameMine(final Mine mine, final String newName) {
        if (mine == null || !isValidName(newName) || !isNameAvailable(newName)) {
            return false;
        }
        final String oldName = mine.getName();
        if (oldName.equals(newName)) {
            return true;
        }
        final boolean holograms = mine instanceof HologramCompatible;
        if (holograms) {
            deleteHolograms((HologramCompatible) mine);
        }
        MineIndex.remove(mine);
        MineAccess.unregister(oldName);
        mine.renameTo(newName);
        WorldGuardIntegration.onMineRenamed(mine, oldName);
        MineAccess.register(mine);
        MineIndex.add(mine);
        if (holograms && GalaxyMinesConfiguration.useHolograms) {
            try {
                ((HologramCompatible) mine).setupHolograms();
            } catch (RuntimeException ex) {
                GalaxyMines.getInstance().printException(
                        "Could not create holograms for renamed mine " + newName, ex);
            }
        }
        GalaxyMines.getInstance().debug(
                "Mine " + oldName + " renamed to " + newName, true);
        return true;
    }




    public static ExpansionResult checkExpansion(final Mine mine, final int blocks) {
        if (mine == null || mine.isDeleted() || !mine.supportsExpansion()) {
            return ExpansionResult.NOT_SUPPORTED;
        }
        if (mine.getWorld() == null) {
            return ExpansionResult.NO_WORLD;
        }
        final MineBounds expanded = MineExpansion.expand(mine.getBounds(), blocks);
        if (MineExpansion.volume(expanded) > MAX_MINE_VOLUME) {
            return ExpansionResult.TOO_LARGE;
        }
        for (final Mine other : MineManager.MINES) {
            if (other == mine || other.isDeleted() || other.getWorld() == null
                    || !other.getWorld().equals(mine.getWorld())) {
                continue;
            }
            if (MineExpansion.overlaps(expanded, other.getBounds())) {
                return ExpansionResult.OVERLAP;
            }
        }
        return ExpansionResult.OK;
    }




    public static ExpansionResult expandMine(final Mine mine, final int blocks) {
        final ExpansionResult check = checkExpansion(mine, blocks);
        if (check != ExpansionResult.OK) {
            return check;
        }
        final boolean holograms = mine instanceof HologramCompatible;
        if (holograms) {
            deleteHolograms((HologramCompatible) mine);
        }
        if (!mine.expandHorizontally(blocks)) {
            return ExpansionResult.NOT_SUPPORTED;
        }
        MineIndex.add(mine);
        WorldGuardIntegration.onMineDeleted(mine);
        WorldGuardIntegration.onMineRegistered(mine);
        if (holograms && GalaxyMinesConfiguration.useHolograms) {
            try {
                ((HologramCompatible) mine).setupHolograms();
            } catch (RuntimeException ex) {
                GalaxyMines.getInstance().printException(
                        "Could not recreate holograms for expanded mine " + mine.getName(), ex);
            }
        }
        mine.reset();
        GalaxyMines.getInstance().debug("Mine " + mine.getName() + " expanded by " + blocks
                + " blocks per side, new volume " + MineExpansion.volume(mine.getBounds()), true);
        return ExpansionResult.OK;
    }



    public static void cloneMine(final Mine source, final String newName,
                                 final Consumer<Mine> onSuccess,
                                 final Consumer<String> onFailure) {
        if (source == null || !isValidName(newName) || !isNameAvailable(newName)) {
            onFailure.accept("exists");
            return;
        }
        final FileConfiguration copy = source.exportConfiguration();
        copy.set("name", newName);
        copy.set("alias", newName);
        copy.set("state", null);
        final String text = copy.saveToString();
        final File target = new File(GalaxyMines.minesFolder, newName + ".yml");
        AsyncFileWriter.write(target, text, () -> org.bukkit.Bukkit.getScheduler()
                .runTask(GalaxyMines.getInstance(), () -> {
                    try {
                        final MineConfig config = new MineConfig(
                                YamlConfiguration.loadConfiguration(target));
                        final Mine clone = loadMine(config);
                        clone.reset();
                        onSuccess.accept(clone);
                    } catch (Throwable t) {
                        GalaxyMines.getInstance().printException(
                                "Could not load the cloned mine " + newName, t);
                        onFailure.accept("load");
                    }
                }));
    }



    public static Mine createCuboid(final String name, final Location first,
                                    final Location second) {
        if (!isValidName(name) || !isNameAvailable(name)
                || first == null || second == null || first.getWorld() == null
                || !first.getWorld().equals(second.getWorld())) {
            return null;
        }
        final CuboidMine mine = new CuboidMine(name, first, second);
        registerMine(mine);
        mine.save();
        mine.reset();
        return mine;
    }




    public static void suspendWorld(final org.bukkit.World world) {
        if (world == null) {
            return;
        }
        MineTracker.clearWorld(world);
        final List<String> names = new ArrayList<>();
        for (final Mine mine : MineIndex.getMinesInWorld(world)) {
            names.add(mine.getName());
            mine.save();
            if (mine instanceof HologramCompatible) {
                deleteHolograms((HologramCompatible) mine);
            }
            WorldGuardIntegration.onMineDeleted(mine);
            MineAccess.unregister(mine.getName());
            MineIndex.remove(mine);
            MineManager.MINES.remove(mine);
        }
        SUSPENDED_WORLDS.put(world.getUID(), names);
        if (!names.isEmpty()) {
            GalaxyMines.getInstance().debug("Suspended " + names.size()
                    + " mines while world " + world.getName() + " unloads", true);
        }
    }



    public static void resumeWorld(final org.bukkit.World world) {
        if (world == null) {
            return;
        }
        final List<String> names = SUSPENDED_WORLDS.remove(world.getUID());
        if (names == null) {
            return;
        }
        for (final String name : names) {
            final File file = new File(GalaxyMines.minesFolder, name + ".yml");
            if (!file.isFile()) {
                continue;
            }
            try {
                final MineConfig config =
                        new MineConfig(YamlConfiguration.loadConfiguration(file));
                loadMine(config);
            } catch (Throwable t) {
                GalaxyMines.getInstance().printException(
                        "Could not reload mine " + name + " after world load", t);
            }
        }
    }

    private static void deleteHolograms(final HologramCompatible mine) {
        for (final me.marcdoesntexist.galaxymines.holograms.CompatibleHologram hologram
                : mine.getHolograms()) {
            try {
                hologram.delete();
            } catch (Throwable ignored) {
            }
        }
        mine.getHolograms().clear();
    }




    public enum ExpansionResult {
        OK, NOT_SUPPORTED, NO_WORLD, OVERLAP, TOO_LARGE
    }

    public static class WorldEditManager {
        @Subscribe
        public void wrapForLogging(final EditSessionEvent event) {
            final Actor actor = event.getActor();
            final World world = event.getWorld();
            if (actor != null && actor.isPlayer() && event.getExtent() != null) {
                event.setExtent(new WorldEditLogger(world, actor, event.getExtent()));
            }
        }

        public static class WorldEditLogger extends AbstractDelegateExtent {
            private final Actor actor;
            private final World world;

            public WorldEditLogger(final World world, final Actor actor, final Extent extent) {
                super(extent);
                this.actor = actor;
                this.world = world;
            }

            public Actor getActor() {
                return this.actor;
            }

            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(
                    final BlockVector3 position, final T block) throws WorldEditException {
                final boolean changed = super.setBlock(position, block);
                if (changed) {
                    this.notifyChange(position);
                }
                return changed;
            }

            private void notifyChange(final BlockVector3 position) {
                final org.bukkit.World bukkitWorld = BukkitAdapter.adapt(this.world);
                if (bukkitWorld == null) {
                    return;
                }
                MineManager.notifyBlockChange(bukkitWorld
                        .getBlockAt(position.x(), position.y(), position.z()));
            }
        }
    }
}
