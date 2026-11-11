package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.config.PluginSettings;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;












public final class ResetMetrics {
    private static final Map<String, ResetMetrics> BY_MINE = new HashMap<>();

    private final long[] sliceNanos;
    private final int[] sliceBlocks;
    private int cursor;
    private int filled;
    private long resets;
    private long totalBlocks;
    private long lastResetAtMillis;
    private long lastDurationNanos;
    private long lastBlocks;
    private long lastSlices;
    private long currentStartNanos;
    private long currentBlocks;
    private long currentSlices;

    public ResetMetrics(final int sampleCapacity) {
        final int capacity = Math.max(16, Math.min(4096, sampleCapacity));
        this.sliceNanos = new long[capacity];
        this.sliceBlocks = new int[capacity];
    }

    


    public static ResetMetrics forMine(final String mineName) {
        return BY_MINE.computeIfAbsent(mineName == null ? "" : mineName,
                ignored -> new ResetMetrics(PluginSettings.get().timings.sampleSize));
    }

    


    public static ResetMetrics find(final String mineName) {
        return BY_MINE.get(mineName == null ? "" : mineName);
    }

    public static void remove(final String mineName) {
        BY_MINE.remove(mineName == null ? "" : mineName);
    }

    public static void clearAll() {
        BY_MINE.clear();
    }

    


    public static int trackedMines() {
        return BY_MINE.size();
    }

    



    public static Map<String, Snapshot> snapshotAll() {
        final Map<String, Snapshot> all = new TreeMap<>();
        BY_MINE.forEach((name, metrics) -> all.put(name, metrics.snapshot()));
        return Collections.unmodifiableMap(all);
    }

    


    public static double averageMillis(final long[] nanos) {
        if (nanos == null || nanos.length == 0) {
            return 0.0D;
        }
        long total = 0L;
        for (final long value : nanos) {
            total += Math.max(0L, value);
        }
        return total / 1_000_000.0D / nanos.length;
    }

    



    public static double percentileMillis(final long[] nanos, final double percentile) {
        if (nanos == null || nanos.length == 0) {
            return 0.0D;
        }
        final double clamped = Math.max(0.0D, Math.min(1.0D, percentile));
        final long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        final int rank = (int) Math.ceil(clamped * sorted.length) - 1;
        final int index = Math.max(0, Math.min(sorted.length - 1, rank));
        return sorted[index] / 1_000_000.0D;
    }

    


    public static double blocksPerSecond(final long[] nanos, final int[] blocks) {
        if (nanos == null || blocks == null || nanos.length == 0 || blocks.length == 0) {
            return 0.0D;
        }
        long totalNanos = 0L;
        long totalBlocks = 0L;
        final int length = Math.min(nanos.length, blocks.length);
        for (int i = 0; i < length; ++i) {
            totalNanos += Math.max(0L, nanos[i]);
            totalBlocks += Math.max(0, blocks[i]);
        }
        if (totalNanos <= 0L) {
            return 0.0D;
        }
        return totalBlocks / (totalNanos / 1_000_000_000.0D);
    }

    


    public void begin() {
        this.currentStartNanos = System.nanoTime();
        this.currentBlocks = 0L;
        this.currentSlices = 0L;
    }

    


    public void recordSlice(final long nanos, final int blocks) {
        if (!PluginSettings.get().timings.enabled) {
            return;
        }
        this.sliceNanos[this.cursor] = Math.max(0L, nanos);
        this.sliceBlocks[this.cursor] = Math.max(0, blocks);
        this.cursor = (this.cursor + 1) % this.sliceNanos.length;
        if (this.filled < this.sliceNanos.length) {
            ++this.filled;
        }
        this.currentBlocks += Math.max(0, blocks);
        ++this.currentSlices;
    }

    


    public void finish() {
        this.lastResetAtMillis = System.currentTimeMillis();
        this.lastDurationNanos = this.currentStartNanos == 0L
                ? 0L : System.nanoTime() - this.currentStartNanos;
        this.lastBlocks = this.currentBlocks;
        this.lastSlices = this.currentSlices;
        this.totalBlocks += this.currentBlocks;
        ++this.resets;
        this.currentStartNanos = 0L;
    }

    


    public void abandon() {
        this.currentStartNanos = 0L;
        this.currentBlocks = 0L;
        this.currentSlices = 0L;
    }

    public long resets() {
        return this.resets;
    }

    public long totalBlocks() {
        return this.totalBlocks;
    }

    public Snapshot snapshot() {
        final long[] nanos = new long[this.filled];
        final int[] blocks = new int[this.filled];
        final int start = this.filled == this.sliceNanos.length ? this.cursor : 0;
        for (int i = 0; i < this.filled; ++i) {
            final int index = (start + i) % this.sliceNanos.length;
            nanos[i] = this.sliceNanos[index];
            blocks[i] = this.sliceBlocks[index];
        }
        return new Snapshot(this.resets, this.totalBlocks, this.lastResetAtMillis,
                this.lastDurationNanos, this.lastBlocks, this.lastSlices,
                averageMillis(nanos), percentileMillis(nanos, 0.95D), percentileMillis(nanos, 1.0D),
                blocksPerSecond(nanos, blocks), this.filled);
    }

    


    public record Snapshot(long resets, long totalBlocks, long lastResetAtMillis,
                           long lastDurationNanos, long lastBlocks, long lastSlices,
                           double sliceAverageMillis, double sliceP95Millis, double sliceMaxMillis,
                           double blocksPerSecond, int samples) {
        public double lastDurationMillis() {
            return this.lastDurationNanos / 1_000_000.0D;
        }

        public double lastDurationSeconds() {
            return this.lastDurationNanos / 1_000_000_000.0D;
        }

        public long lastResetAgeMillis() {
            return this.lastResetAtMillis == 0L
                    ? -1L : System.currentTimeMillis() - this.lastResetAtMillis;
        }
    }
}
