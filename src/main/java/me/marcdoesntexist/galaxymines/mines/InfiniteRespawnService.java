package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.mines.types.BasicMine;
import me.marcdoesntexist.galaxymines.utils.BlockPositions;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;












public final class InfiniteRespawnService {
    private static final Map<BasicMine, LongRespawnQueue> QUEUES = new IdentityHashMap<>();
    private static BukkitTask ticker;
    private static int lastTickRespawns;
    private static long totalRespawns;

    private InfiniteRespawnService() {
    }



    public static void schedule(final BasicMine mine, final int x, final int y, final int z) {
        if (mine == null || mine.isDeleted() || mine.isResetInProgress()) {
            return;
        }
        final LongRespawnQueue queue = QUEUES.computeIfAbsent(mine,
                ignored -> new LongRespawnQueue(256));
        final long due = System.currentTimeMillis()
                + Math.max(1L, (long) (mine.getInfiniteRespawnSeconds() * 1000.0D));
        if (!queue.offer(due, BlockPositions.pack(x, y, z), mine.getInfiniteMaxPending())) {
            GalaxyMines.getInstance().debug("Infinite respawn queue full for mine "
                    + mine.getName() + "; dropping the oldest pending block.", true);
            if (!queue.isEmpty()) {
                queue.poll();
            }
            queue.offer(due, BlockPositions.pack(x, y, z), mine.getInfiniteMaxPending());
        }
        ensureTicker();
    }

    public static int pending(final BasicMine mine) {
        final LongRespawnQueue queue = QUEUES.get(mine);
        return queue == null ? 0 : queue.size();
    }

    public static int pendingTotal() {
        int total = 0;
        for (final LongRespawnQueue queue : QUEUES.values()) {
            total += queue.size();
        }
        return total;
    }

    public static int lastTickRespawns() {
        return lastTickRespawns;
    }

    public static long totalRespawns() {
        return totalRespawns;
    }



    public static void clear(final BasicMine mine) {
        if (mine == null) {
            return;
        }
        QUEUES.remove(mine);
        stopTickerIfIdle();
    }

    public static void clearAll() {
        QUEUES.clear();
        stopTickerIfIdle();
    }

    private static void ensureTicker() {
        if (ticker != null) {
            return;
        }
        ticker = Bukkit.getScheduler().runTaskTimer(GalaxyMines.getInstance(),
                InfiniteRespawnService::tick, 1L, 1L);
    }

    private static void stopTickerIfIdle() {
        if (!QUEUES.isEmpty() || ticker == null) {
            return;
        }
        ticker.cancel();
        ticker = null;
    }

    private static void tick() {
        final long now = System.currentTimeMillis();
        final int budget = PluginSettings.get().infinite.maxRespawnsPerTick;
        int respawned = 0;
        final Iterator<Map.Entry<BasicMine, LongRespawnQueue>> iterator = QUEUES.entrySet()
                .iterator();
        while (iterator.hasNext() && respawned < budget) {
            final Map.Entry<BasicMine, LongRespawnQueue> entry = iterator.next();
            final BasicMine mine = entry.getKey();
            final LongRespawnQueue queue = entry.getValue();
            if (mine.isDeleted() || !mine.isEnabled()) {
                iterator.remove();
                continue;
            }
            while (respawned < budget && !queue.isEmpty() && queue.peekDue() <= now) {
                final long packed = queue.peekPosition();
                queue.poll();
                apply(mine, packed);
                ++respawned;
                ++totalRespawns;
            }
            if (queue.isEmpty()) {
                iterator.remove();
            }
        }
        lastTickRespawns = respawned;
        stopTickerIfIdle();
    }

    private static void apply(final BasicMine mine, final long packed) {
        final Block block = mine.getWorld().getBlockAt(BlockPositions.unpackX(packed),
                BlockPositions.unpackY(packed), BlockPositions.unpackZ(packed));
        if (!mine.contains(block)) {
            return;
        }
        block.setBlockData(mine.getResetPalette().nextBlock().data(), false);
    }
}
