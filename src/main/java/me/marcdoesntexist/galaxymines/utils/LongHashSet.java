package me.marcdoesntexist.galaxymines.utils;

import java.util.Arrays;
import java.util.function.LongConsumer;


public final class LongHashSet {
    private static final int MIN_CAPACITY = 8;

    private long[] keys;
    private boolean[] used;
    private int mask;
    private int threshold;
    private int size;

    public LongHashSet() {
        this(16);
    }

    public LongHashSet(final int expectedSize) {
        final int capacity = capacityFor(expectedSize);
        this.keys = new long[capacity];
        this.used = new boolean[capacity];
        this.mask = capacity - 1;
        this.threshold = capacity * 2 / 3;
    }

    private static int capacityFor(final int expectedSize) {
        int capacity = MIN_CAPACITY;
        final int target = Math.max(MIN_CAPACITY, expectedSize);
        while (capacity < target) {
            capacity <<= 1;
        }
        return capacity;
    }

    private static int spread(final long value) {
        long hash = value;
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= hash >>> 33;
        return (int) hash;
    }

    public boolean add(final long value) {
        if (this.size >= this.threshold) {
            this.resize();
        }
        int index = spread(value) & this.mask;
        while (this.used[index]) {
            if (this.keys[index] == value) {
                return false;
            }
            index = (index + 1) & this.mask;
        }
        this.used[index] = true;
        this.keys[index] = value;
        ++this.size;
        return true;
    }

    public boolean contains(final long value) {
        int index = spread(value) & this.mask;
        while (this.used[index]) {
            if (this.keys[index] == value) {
                return true;
            }
            index = (index + 1) & this.mask;
        }
        return false;
    }

    public void clear() {
        Arrays.fill(this.used, false);
        this.size = 0;
    }

    public int size() {
        return this.size;
    }

    public void forEachLong(final LongConsumer consumer) {
        for (int i = 0; i < this.keys.length; ++i) {
            if (this.used[i]) {
                consumer.accept(this.keys[i]);
            }
        }
    }

    public long[] toLongArray() {
        final long[] values = new long[this.size];
        int index = 0;
        for (int i = 0; i < this.keys.length; ++i) {
            if (this.used[i]) {
                values[index++] = this.keys[i];
            }
        }
        return values;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    private void resize() {
        final long[] oldKeys = this.keys;
        final boolean[] oldUsed = this.used;
        final int capacity = oldKeys.length << 1;
        this.keys = new long[capacity];
        this.used = new boolean[capacity];
        this.mask = capacity - 1;
        this.threshold = capacity * 2 / 3;
        for (int i = 0; i < oldKeys.length; ++i) {
            if (!oldUsed[i]) {
                continue;
            }
            int index = spread(oldKeys[i]) & this.mask;
            while (this.used[index]) {
                index = (index + 1) & this.mask;
            }
            this.used[index] = true;
            this.keys[index] = oldKeys[i];
        }
    }
}
