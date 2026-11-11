package me.marcdoesntexist.galaxymines.mines;











public final class LongRespawnQueue {
    private long[] dueMillis;
    private long[] positions;
    private int head;
    private int size;

    public LongRespawnQueue() {
        this(64);
    }

    public LongRespawnQueue(final int initialCapacity) {
        final int capacity = capacityFor(Math.max(2, initialCapacity));
        this.dueMillis = new long[capacity];
        this.positions = new long[capacity];
    }

    private static int capacityFor(final int expected) {
        int capacity = 2;
        while (capacity < expected) {
            capacity <<= 1;
        }
        return capacity;
    }




    public boolean offer(final long due, final long position, final int maxSize) {
        if (maxSize > 0 && this.size >= maxSize) {
            return false;
        }
        if (this.size == this.dueMillis.length) {
            this.grow();
        }
        final int index = (this.head + this.size) & (this.dueMillis.length - 1);
        this.dueMillis[index] = due;
        this.positions[index] = position;
        ++this.size;
        return true;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public int size() {
        return this.size;
    }

    public long peekDue() {
        requireNotEmpty();
        return this.dueMillis[this.head];
    }

    public long peekPosition() {
        requireNotEmpty();
        return this.positions[this.head];
    }

    public void poll() {
        requireNotEmpty();
        this.head = (this.head + 1) & (this.dueMillis.length - 1);
        --this.size;
    }

    public void clear() {
        this.head = 0;
        this.size = 0;
    }

    private void requireNotEmpty() {
        if (this.size == 0) {
            throw new IllegalStateException("The respawn queue is empty");
        }
    }

    private void grow() {
        final int capacity = this.dueMillis.length << 1;
        final long[] newDue = new long[capacity];
        final long[] newPositions = new long[capacity];
        for (int i = 0; i < this.size; ++i) {
            final int index = (this.head + i) & (this.dueMillis.length - 1);
            newDue[i] = this.dueMillis[index];
            newPositions[i] = this.positions[index];
        }
        this.dueMillis = newDue;
        this.positions = newPositions;
        this.head = 0;
    }
}
