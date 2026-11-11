package me.marcdoesntexist.galaxymines.mines;

import java.util.Locale;
import java.util.Random;













public final class ResetThreshold {


    public static final long NANOS_PER_TICK = 50_000_000L;



    public static final ResetThreshold DISABLED = new ResetThreshold(0.0D, 0.0D, 0.0D, 0);

    private final double defaultPercent;
    private final double minPercent;
    private final double maxPercent;
    private final long intervalNanos;
    private double target;
    private long lastEvaluationNanos;
    private boolean evaluated;

    public ResetThreshold(final double defaultPercent, final double minPercent,
                          final double maxPercent, final int checkIntervalTicks) {
        this.defaultPercent = clamp(defaultPercent);
        double min = clamp(minPercent);
        double max = clamp(maxPercent);
        if (min <= 0.0D && max <= 0.0D) {
            min = this.defaultPercent;
            max = this.defaultPercent;
        }
        if (min > max) {
            final double swap = min;
            min = max;
            max = swap;
        }
        this.minPercent = min;
        this.maxPercent = max;
        this.intervalNanos = Math.max(0, checkIntervalTicks) * NANOS_PER_TICK;
        this.target = this.defaultPercent > 0.0D ? this.defaultPercent : Math.max(min, max);
    }

    private static double clamp(final double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    public boolean enabled() {
        return this.target > 0.0D;
    }

    public double target() {
        return this.target;
    }

    public double defaultPercent() {
        return this.defaultPercent;
    }

    public double minPercent() {
        return this.minPercent;
    }

    public double maxPercent() {
        return this.maxPercent;
    }

    public int checkIntervalTicks() {
        return (int) (this.intervalNanos / NANOS_PER_TICK);
    }




    public double rollTarget(final Random random) {
        if (this.maxPercent <= this.minPercent) {
            this.target = this.defaultPercent > 0.0D ? this.defaultPercent : this.maxPercent;
        } else {
            final double rolled = this.minPercent
                    + (random == null ? 0.0D : random.nextDouble()) * (this.maxPercent - this.minPercent);
            this.target = Math.round(rolled * 100.0D) / 100.0D;
        }
        return this.target;
    }




    public boolean shouldEvaluate(final long nowNanos) {
        return this.intervalNanos == 0L || !this.evaluated
                || nowNanos - this.lastEvaluationNanos >= this.intervalNanos;
    }




    public boolean reached(final double remainingPercent, final long nowNanos) {
        if (!enabled() || !shouldEvaluate(nowNanos)) {
            return false;
        }
        this.lastEvaluationNanos = nowNanos;
        this.evaluated = true;
        return remainingPercent <= this.target;
    }



    public String describe() {
        if (!enabled()) {
            return "disabled";
        }
        if (this.maxPercent > this.minPercent) {
            return String.format(Locale.ROOT, "%.2f%% (range %.2f-%.2f)",
                    this.target, this.minPercent, this.maxPercent);
        }
        return String.format(Locale.ROOT, "%.2f%%", this.target);
    }
}
