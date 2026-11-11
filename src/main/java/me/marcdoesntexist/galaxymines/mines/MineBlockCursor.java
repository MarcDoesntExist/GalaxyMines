package me.marcdoesntexist.galaxymines.mines;


public final class MineBlockCursor {


    public static final Shape EVERYTHING = (x, y, z) -> true;
    private final Shape shape;
    private final boolean wave;
    private final boolean descending;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private int x;
    private int y;
    private int z;
    private boolean started;
    private boolean pushedBack;
    private boolean finished;

    public MineBlockCursor(final int minX, final int minY, final int minZ,
                           final int maxX, final int maxY, final int maxZ,
                           final boolean wave, final Shape shape) {
        this(minX, minY, minZ, maxX, maxY, maxZ, wave, true, shape);
    }

    public MineBlockCursor(final int minX, final int minY, final int minZ,
                           final int maxX, final int maxY, final int maxZ,
                           final boolean wave, final boolean descending, final Shape shape) {
        this.descending = descending;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.shape = shape == null ? EVERYTHING : shape;
        this.wave = wave;
        this.x = this.minX;
        this.y = wave && descending ? this.maxY : this.minY;
        this.z = this.minZ;
        this.finished = this.minX > this.maxX || this.minY > this.maxY || this.minZ > this.maxZ;
    }


    public static MineBlockCursor of(final Mine mine, final boolean wave) {
        return of(mine, wave, MineWaveDirection.TOP_DOWN);
    }


    public static MineBlockCursor of(final Mine mine, final boolean wave,
                                     final MineWaveDirection direction) {
        final MineBounds bounds = mine.getBounds();
        final boolean descending = direction == null || direction.descending();
        return new MineBlockCursor(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ(), wave, descending,
                (x, y, z) -> mine.contains((double) x, (double) y, (double) z));
    }


    public boolean advance() {
        if (this.pushedBack) {
            this.pushedBack = false;
            return true;
        }
        if (this.finished) {
            return false;
        }
        if (this.started) {
            this.step();
        } else {
            this.started = true;
        }
        while (!this.finished) {
            if (this.shape.contains(this.x, this.y, this.z)) {
                return true;
            }
            this.step();
        }
        return false;
    }


    public void pushBack() {
        this.pushedBack = true;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int z() {
        return this.z;
    }


    public long capacity() {
        return (long) (this.maxX - this.minX + 1)
                * (this.maxY - this.minY + 1)
                * (this.maxZ - this.minZ + 1);
    }

    private void step() {
        if (this.wave) {
            if (++this.x > this.maxX) {
                this.x = this.minX;
                if (++this.z > this.maxZ) {
                    this.z = this.minZ;
                    if (this.descending) {
                        if (--this.y < this.minY) {
                            this.finished = true;
                        }
                    } else if (++this.y > this.maxY) {
                        this.finished = true;
                    }
                }
            }
        } else if (++this.x > this.maxX) {
            this.x = this.minX;
            if (++this.y > this.maxY) {
                this.y = this.minY;
                if (++this.z > this.maxZ) {
                    this.finished = true;
                }
            }
        }
    }


    @FunctionalInterface
    public interface Shape {
        boolean contains(int x, int y, int z);
    }
}
