package me.marcdoesntexist.galaxymines.mines;

import java.util.Locale;


public final class MineProgression {


    public static final MineProgression DISABLED =
            new MineProgression(false, 1, 10, 0L, 0.0D, 0.0D, 1.0D);

    private static final long MAX_BLOCKS_PER_LEVEL = 1_000_000_000_000L;

    private final boolean enabled;
    private final int level;
    private final int maxLevel;
    private final long minedBlocks;
    private final long blocksPerLevel;
    private final double dropBonusPerLevel;
    private final double sellBonusPerLevel;
    private final double maxBonus;

    public MineProgression(final boolean enabled, final int level, final int maxLevel,
                           final long blocksPerLevel, final double dropBonusPerLevel,
                           final double sellBonusPerLevel, final double maxBonus) {
        this(enabled, level, maxLevel, 0L, blocksPerLevel, dropBonusPerLevel,
                sellBonusPerLevel, maxBonus);
    }

    public MineProgression(final boolean enabled, final int level, final int maxLevel,
                           final long minedBlocks, final long blocksPerLevel,
                           final double dropBonusPerLevel, final double sellBonusPerLevel,
                           final double maxBonus) {
        this.enabled = enabled;
        this.maxLevel = Math.max(1, Math.min(100, maxLevel));
        this.level = Math.max(1, Math.min(this.maxLevel, level));
        this.minedBlocks = Math.max(0L, minedBlocks);
        this.blocksPerLevel = Math.max(0L, Math.min(MAX_BLOCKS_PER_LEVEL, blocksPerLevel));
        this.dropBonusPerLevel = sanitizeBonus(dropBonusPerLevel);
        this.sellBonusPerLevel = sanitizeBonus(sellBonusPerLevel);
        this.maxBonus = sanitizeBonus(maxBonus);
    }

    private static double sanitizeBonus(final double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, value);
    }

    public boolean enabled() {
        return this.enabled;
    }

    public int level() {
        return this.level;
    }

    public int maxLevel() {
        return this.maxLevel;
    }

    public long minedBlocks() {
        return this.minedBlocks;
    }

    public long blocksPerLevel() {
        return this.blocksPerLevel;
    }

    public double dropBonusPerLevel() {
        return this.dropBonusPerLevel;
    }

    public double sellBonusPerLevel() {
        return this.sellBonusPerLevel;
    }

    public double maxBonus() {
        return this.maxBonus;
    }


    public double dropBonus() {
        return bonus(this.dropBonusPerLevel);
    }


    public double sellBonus() {
        return bonus(this.sellBonusPerLevel);
    }


    public int levelFor(final long blocks) {
        if (!this.enabled || this.blocksPerLevel <= 0L) {
            return this.level;
        }
        final long computed = 1L + Math.max(0L, blocks) / this.blocksPerLevel;
        return (int) Math.max(1L, Math.min(this.maxLevel, computed));
    }


    public MineProgression withLevel(final int newLevel) {
        final int clamped = Math.max(1, Math.min(this.maxLevel, newLevel));
        final long blocks = this.blocksPerLevel <= 0L
                ? this.minedBlocks : (long) (clamped - 1) * this.blocksPerLevel;
        return new MineProgression(this.enabled, clamped, this.maxLevel, blocks,
                this.blocksPerLevel, this.dropBonusPerLevel, this.sellBonusPerLevel,
                this.maxBonus);
    }


    public MineProgression withMined(final long total) {
        final long blocks = Math.max(0L, total);
        return new MineProgression(this.enabled, levelFor(blocks), this.maxLevel, blocks,
                this.blocksPerLevel, this.dropBonusPerLevel, this.sellBonusPerLevel,
                this.maxBonus);
    }


    public long blocksToNextLevel() {
        if (this.blocksPerLevel <= 0L || this.level >= this.maxLevel) {
            return 0L;
        }
        return this.blocksPerLevel - (this.minedBlocks % this.blocksPerLevel);
    }


    public double progressPercent() {
        if (this.blocksPerLevel <= 0L || this.level >= this.maxLevel) {
            return 100.0D;
        }
        return 100.0D * (this.minedBlocks % this.blocksPerLevel) / this.blocksPerLevel;
    }


    public String describeBonus() {
        return String.format(Locale.ROOT, "%.2f%%", this.dropBonus() * 100.0D);
    }

    private double bonus(final double perLevel) {
        if (!this.enabled) {
            return 0.0D;
        }
        final double raw = Math.max(0, this.level - 1) * perLevel;
        return Math.min(this.maxBonus, raw);
    }
}
