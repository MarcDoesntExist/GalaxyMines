package me.marcdoesntexist.galaxymines.holograms;

import me.marcdoesntexist.galaxymines.holograms.impl.DisplayHologram;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;












public final class OrphanCleanupTask {
    private static final OrphanCleanupTask INSTANCE =
            new OrphanCleanupTask(DisplayHologram::cleanupOrphans);

    private final IntSupplier cleanup;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile long lastRunAt;
    private volatile int lastRemoved = -1;
    private volatile boolean lastSkipped;

    OrphanCleanupTask(final IntSupplier cleanup) {
        this.cleanup = cleanup;
    }

    public static OrphanCleanupTask get() {
        return INSTANCE;
    }



    public static boolean shouldRun(final int minutes) {
        return minutes > 0;
    }





    public boolean runOnce() {
        if (!this.running.compareAndSet(false, true)) {
            this.lastSkipped = true;
            return false;
        }
        try {
            this.lastRemoved = Math.max(0, this.cleanup.getAsInt());
            this.lastRunAt = System.currentTimeMillis();
            return true;
        } catch (RuntimeException exception) {
            return false;
        } finally {
            this.running.set(false);
        }
    }

    public long lastRunAt() {
        return this.lastRunAt;
    }



    public int lastRemoved() {
        return this.lastRemoved;
    }



    public boolean lastSkipped() {
        return this.lastSkipped;
    }



    public String lastRunRelative() {
        if (this.lastRunAt <= 0L) {
            return "never";
        }
        final long minutes = Math.max(0L, (System.currentTimeMillis() - this.lastRunAt) / 60_000L);
        return minutes + "m";
    }
}
