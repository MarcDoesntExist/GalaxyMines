package me.marcdoesntexist.galaxymines.utils;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;


public class RandomCollection<E> {
    private final RandomGenerator random;
    private E[] values;
    private double[] cumulative;
    private int size;
    private double total;

    public RandomCollection() {
        this(ThreadLocalRandom.current());
    }

    @SuppressWarnings("unchecked")
    public RandomCollection(final RandomGenerator random) {
        this.values = (E[]) new Object[4];
        this.cumulative = new double[4];
        this.total = 0.0;
        this.random = random;
    }

    public void add(final double weight, final E result) {
        if (weight <= 0.0) {
            return;
        }
        if (this.size == this.values.length) {
            final int capacity = this.values.length << 1;
            this.values = Arrays.copyOf(this.values, capacity);
            this.cumulative = Arrays.copyOf(this.cumulative, capacity);
        }
        this.total += weight;
        this.values[this.size] = result;
        this.cumulative[this.size] = this.total;
        ++this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public E next() {
        if (this.size == 0) {
            throw new IllegalStateException("RandomCollection is empty");
        }
        final double value = this.random.nextDouble() * this.total;
        int low = 0;
        int high = this.size - 1;
        while (low < high) {
            final int middle = (low + high) >>> 1;
            if (this.cumulative[middle] < value) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return this.values[low];
    }
}
