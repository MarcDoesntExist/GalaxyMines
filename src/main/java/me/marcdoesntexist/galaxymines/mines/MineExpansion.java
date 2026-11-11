package me.marcdoesntexist.galaxymines.mines;







public final class MineExpansion {


    public static final int MAX_BLOCKS_PER_SIDE = 64;

    private MineExpansion() {
    }




    public static MineBounds expand(final MineBounds bounds, final int blocks) {
        final int amount = Math.max(0, blocks);
        return new MineBounds(bounds.minX() - amount, bounds.minY(), bounds.minZ() - amount,
                bounds.maxX() + amount, bounds.maxY(), bounds.maxZ() + amount);
    }



    public static long volume(final MineBounds bounds) {
        if (bounds == null) {
            return 0L;
        }
        final long x = (long) bounds.maxX() - bounds.minX() + 1L;
        final long y = (long) bounds.maxY() - bounds.minY() + 1L;
        final long z = (long) bounds.maxZ() - bounds.minZ() + 1L;
        if (x <= 0L || y <= 0L || z <= 0L) {
            return 0L;
        }
        return x * y * z;
    }



    public static boolean overlaps(final MineBounds first, final MineBounds second) {
        return first.minX() <= second.maxX() && first.maxX() >= second.minX()
                && first.minY() <= second.maxY() && first.maxY() >= second.minY()
                && first.minZ() <= second.maxZ() && first.maxZ() >= second.minZ();
    }
}
