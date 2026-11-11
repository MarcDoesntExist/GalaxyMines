package me.marcdoesntexist.galaxymines.utils;










public final class BlockPositions {
    private static final int COORD_BITS = 26;
    private static final int COORD_MASK = 0x3FFFFFF;
    private static final int COORD_SIGN = 0x2000000;
    private static final int COORD_RANGE = 0x4000000;
    private static final int Y_BITS_MASK = 0xFFF;
    private static final int Y_SIGN = 0x800;
    private static final int Y_RANGE = 0x1000;

    private BlockPositions() {
    }

    public static long pack(final int x, final int y, final int z) {
        return ((long) (x & COORD_MASK) << 38)
                | ((long) (z & COORD_MASK) << 12)
                | (y & Y_BITS_MASK);
    }

    public static int unpackX(final long packed) {
        final int value = (int) ((packed >> 38) & COORD_MASK);
        return value >= COORD_SIGN ? value - COORD_RANGE : value;
    }

    public static int unpackZ(final long packed) {
        final int value = (int) ((packed >> 12) & COORD_MASK);
        return value >= COORD_SIGN ? value - COORD_RANGE : value;
    }

    public static int unpackY(final long packed) {
        final int value = (int) (packed & Y_BITS_MASK);
        return value >= Y_SIGN ? value - Y_RANGE : value;
    }
}
