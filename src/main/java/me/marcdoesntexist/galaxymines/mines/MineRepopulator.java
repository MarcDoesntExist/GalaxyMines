package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.mines.types.BasicMine;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;













public final class MineRepopulator {
    private static final List<Session> SESSIONS = new ArrayList<>();
    private static BukkitTask ticker;
    private static int lastTickBlocks;
    private static long lastTickNanos;
    private static long totalBlocks;

    private MineRepopulator() {
    }



    public static Session start(final BasicMine mine, final Runnable onComplete) {
        final MineResetPalette palette = mine.getResetPalette();
        final double blockPercentage = palette.blockPercentage();
        final PluginSettings.Reset settings = PluginSettings.get().reset;
        final boolean wave = mine.getResetStyle() == MineResetStyle.WAVE;
        final int waveLayers = mine.getWaveLayersPerTick() > 0
                ? mine.getWaveLayersPerTick() : settings.waveLayersPerTick;
        final Session session = new Session(mine,
                MineBlockCursor.of(mine, wave, mine.getWaveDirection()), palette,
                blockPercentage < 100.0D, blockPercentage, settings.blocksPerTick,
                waveLayers, wave, onComplete,
                ResetMetrics.forMine(mine.getName()));
        SESSIONS.add(session);
        ensureTicker();
        return session;
    }



    public static boolean isResetting(final BasicMine mine) {
        for (final Session session : SESSIONS) {
            if (session.mine == mine) {
                return true;
            }
        }
        return false;
    }



    public static void cancel(final BasicMine mine) {
        if (mine == null) {
            return;
        }
        for (int i = SESSIONS.size() - 1; i >= 0; --i) {
            final Session session = SESSIONS.get(i);
            if (session.mine == mine) {
                session.done = true;
                session.metrics.abandon();
                SESSIONS.remove(i);
            }
        }
        stopTickerIfIdle();
    }

    public static void cancel(final Session session) {
        if (session == null) {
            return;
        }
        session.done = true;
        session.metrics.abandon();
        SESSIONS.remove(session);
        stopTickerIfIdle();
    }

    public static void cancelAll() {
        for (final Session session : SESSIONS) {
            session.done = true;
            session.metrics.abandon();
        }
        SESSIONS.clear();
        stopTickerIfIdle();
    }

    public static int activeSessions() {
        return SESSIONS.size();
    }

    public static int lastTickBlocks() {
        return lastTickBlocks;
    }

    public static double lastTickMillis() {
        return lastTickNanos / 1_000_000.0D;
    }

    public static long totalBlocks() {
        return totalBlocks;
    }

    private static void ensureTicker() {
        if (ticker != null) {
            return;
        }
        ticker = Bukkit.getScheduler().runTaskTimer(GalaxyMines.getInstance(),
                MineRepopulator::tick, 1L, 1L);
    }

    private static void stopTickerIfIdle() {
        if (!SESSIONS.isEmpty() || ticker == null) {
            return;
        }
        ticker.cancel();
        ticker = null;
    }

    private static void tick() {
        final long tickStart = System.nanoTime();
        final PluginSettings.Reset settings = PluginSettings.get().reset;
        final long deadline = tickStart + settings.globalNanosPerTick;
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        int tickBlocks = 0;
        List<Session> completed = null;
        final int count = Math.min(SESSIONS.size(), Integer.MAX_VALUE);
        for (int index = 0; index < count; ++index) {
            if (index >= SESSIONS.size()) {
                break;
            }
            final Session session = SESSIONS.get(index);
            if (session.done) {
                continue;
            }
            session.layersThisTick = 0;
            final long sessionStart = System.nanoTime();
            final long sessionDeadline = Math.min(deadline,
                    sessionStart + settings.maxNanosPerTick);
            int sessionBlocks = 0;
            boolean exhausted = false;
            try {
                while (sessionBlocks < session.blocksPerTick) {
                    if ((sessionBlocks & 31) == 0) {
                        final long now = System.nanoTime();
                        if (now >= deadline || now >= sessionDeadline) {
                            break;
                        }
                    }
                    if (!session.cursor.advance()) {
                        exhausted = true;
                        break;
                    }
                    if (session.wave) {
                        final int currentY = session.cursor.y();
                        if (currentY != session.lastY) {
                            if (session.lastY != Integer.MIN_VALUE
                                    && session.layersThisTick >= session.waveLayersPerTick) {
                                session.cursor.pushBack();
                                break;
                            }
                            session.lastY = currentY;
                            ++session.layersThisTick;
                        }
                    }
                    final BlockData data = session.canBeAir
                            && random.nextDouble(100.0D) >= session.blockPercentage
                            ? session.palette.air()
                            : session.palette.nextBlock().data();
                    apply(session.mine.getWorld(), session.cursor.x(), session.cursor.y(),
                            session.cursor.z(), data);
                    ++sessionBlocks;
                    ++tickBlocks;
                    ++session.processedTotal;
                }
                if (sessionBlocks > 0) {
                    session.metrics.recordSlice(System.nanoTime() - sessionStart, sessionBlocks);
                }
                if (exhausted) {
                    session.done = true;
                    if (completed == null) {
                        completed = new ArrayList<>(1);
                    }
                    completed.add(session);
                }
            } catch (RuntimeException ex) {
                session.done = true;
                session.metrics.abandon();
                GalaxyMines.getInstance().printException(
                        "There was an error repopulating mine " + session.mine.getName(), ex);
                if (completed == null) {
                    completed = new ArrayList<>(1);
                }
                completed.add(session);
                session.failed = true;
            }
        }
        if (completed != null) {
            for (final Session session : completed) {
                SESSIONS.remove(session);
                if (session.failed) {
                    session.mine.abortReset();
                } else {
                    session.metrics.finish();
                    runCallback(session);
                }
            }
        }
        lastTickBlocks = tickBlocks;
        lastTickNanos = System.nanoTime() - tickStart;
        totalBlocks += tickBlocks;
        stopTickerIfIdle();
    }

    private static void apply(final World world, final int x, final int y, final int z,
                              final BlockData data) {
        world.getBlockAt(x, y, z).setBlockData(data, false);
    }

    private static void runCallback(final Session session) {
        if (session.onComplete == null) {
            return;
        }
        try {
            session.onComplete.run();
        } catch (RuntimeException ex) {
            GalaxyMines.getInstance().printException(
                    "The reset callback failed for mine " + session.mine.getName(), ex);
        }
    }



    public static final class Session {
        private final BasicMine mine;
        private final MineBlockCursor cursor;
        private final MineResetPalette palette;
        private final boolean canBeAir;
        private final double blockPercentage;
        private final int blocksPerTick;
        private final int waveLayersPerTick;
        private final boolean wave;
        private final Runnable onComplete;
        private final ResetMetrics metrics;
        private int processedTotal;
        private int lastY = Integer.MIN_VALUE;
        private int layersThisTick;
        private boolean done;
        private boolean failed;

        private Session(final BasicMine mine, final MineBlockCursor cursor,
                        final MineResetPalette palette, final boolean canBeAir,
                        final double blockPercentage, final int blocksPerTick,
                        final int waveLayersPerTick, final boolean wave,
                        final Runnable onComplete, final ResetMetrics metrics) {
            this.mine = mine;
            this.cursor = cursor;
            this.palette = palette;
            this.canBeAir = canBeAir;
            this.blockPercentage = blockPercentage;
            this.blocksPerTick = Math.max(1, blocksPerTick);
            this.waveLayersPerTick = Math.max(1, waveLayersPerTick);
            this.wave = wave;
            this.onComplete = onComplete;
            this.metrics = metrics;
        }

        public int processedBlocks() {
            return this.processedTotal;
        }

        public boolean isDone() {
            return this.done;
        }
    }
}
