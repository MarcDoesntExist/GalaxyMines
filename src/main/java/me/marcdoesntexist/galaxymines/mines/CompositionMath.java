package me.marcdoesntexist.galaxymines.mines;

import java.util.Collection;




public final class CompositionMath {
    private CompositionMath() {
    }

    public static double total(final Collection<Double> percentages) {
        double total = 0.0D;
        if (percentages != null) {
            for (final Double percentage : percentages) {
                if (percentage != null) {
                    total += percentage;
                }
            }
        }
        return total;
    }

    public static boolean exceeds(final double total) {
        return total > 100.0D;
    }

    public static double free(final double total) {
        return exceeds(total) ? 0.0D : 100.0D - total;
    }

    public static double excess(final double total) {
        return exceeds(total) ? total - 100.0D : 0.0D;
    }
}
