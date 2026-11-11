package me.marcdoesntexist.galaxymines.mines;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.function.Consumer;


public final class MineIndex {
    private static final Map<UUID, Map<Long, List<Mine>>> BY_WORLD = new HashMap<>();

    private MineIndex() {
    }

    public static void add(final Mine mine) {
        if (mine == null || mine.isDeleted()) {
            return;
        }
        remove(mine);
        final MineBounds bounds = mine.getBounds();
        final Map<Long, List<Mine>> chunks = BY_WORLD.computeIfAbsent(
                mine.getWorld().getUID(), ignored -> new HashMap<>());
        for (int chunkX = bounds.minChunkX(); chunkX <= bounds.maxChunkX(); ++chunkX) {
            for (int chunkZ = bounds.minChunkZ(); chunkZ <= bounds.maxChunkZ(); ++chunkZ) {
                chunks.computeIfAbsent(key(chunkX, chunkZ), ignored -> new ArrayList<>()).add(mine);
            }
        }
    }

    public static void remove(final Mine mine) {
        if (mine == null) {
            return;
        }
        final Map<Long, List<Mine>> chunks = BY_WORLD.get(mine.getWorld().getUID());
        if (chunks == null) {
            return;
        }
        for (final List<Mine> mines : chunks.values()) {
            mines.remove(mine);
        }
        chunks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (chunks.isEmpty()) {
            BY_WORLD.remove(mine.getWorld().getUID());
        }
    }

    public static void clear() {
        BY_WORLD.clear();
    }

    public static boolean hasMinesInWorld(final World world) {
        return world != null && BY_WORLD.containsKey(world.getUID());
    }


    public static int worldCount() {
        return BY_WORLD.size();
    }


    public static int chunkCount() {
        int count = 0;
        for (final Map<Long, List<Mine>> chunks : BY_WORLD.values()) {
            count += chunks.size();
        }
        return count;
    }


    public static int entryCount() {
        int count = 0;
        for (final Map<Long, List<Mine>> chunks : BY_WORLD.values()) {
            for (final List<Mine> mines : chunks.values()) {
                count += mines.size();
            }
        }
        return count;
    }

    public static Mine getMineAt(final Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        final List<Mine> candidates = candidates(location);
        for (final Mine mine : candidates) {
            if (!mine.isDeleted() && mine.isEnabled() && mine.contains(location)) {
                return mine;
            }
        }
        return null;
    }

    public static void forEachMineAt(final Location location, final Consumer<Mine> consumer) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        for (final Mine mine : candidates(location)) {
            if (!mine.isDeleted() && mine.isEnabled() && mine.contains(location)) {
                consumer.accept(mine);
            }
        }
    }

    private static List<Mine> candidates(final Location location) {
        final Map<Long, List<Mine>> chunks = BY_WORLD.get(location.getWorld().getUID());
        if (chunks == null) {
            return List.of();
        }
        final List<Mine> mines = chunks.get(key(location.getBlockX() >> 4,
                location.getBlockZ() >> 4));
        return mines == null ? List.of() : mines;
    }

    static long key(final int chunkX, final int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    public static World worldOf(final UUID worldId) {
        return org.bukkit.Bukkit.getWorld(worldId);
    }

    public static List<Mine> getMinesInWorld(final World world) {
        if (world == null) {
            return List.of();
        }
        final Map<Long, List<Mine>> chunks = BY_WORLD.get(world.getUID());
        if (chunks == null) {
            return List.of();
        }
        final Set<Mine> mines = new LinkedHashSet<>();
        for (final List<Mine> chunkMines : chunks.values()) {
            mines.addAll(chunkMines);
        }
        return new ArrayList<>(mines);
    }
}
