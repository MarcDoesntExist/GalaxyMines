package me.marcdoesntexist.galaxymines.stats;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;





public final class PlayerStats {
    private final UUID uuid;
    private final Map<String, Long> mineBlocks = new HashMap<>();
    private final Map<String, PeriodValue> periods = new HashMap<>();
    private String name;
    private long totalBlocks;
    private long totalSoldItems;
    private double totalEarned;
    private boolean dirty;
    private long revision;
    private long lastPruneEpochDay = LocalDate.now().toEpochDay();

    public PlayerStats(final UUID uuid, final String name) {
        this.uuid = uuid;
        this.name = name;
    }

    static String periodKey(final StatsPeriod type, final LocalDate date) {
        return type.name() + ":" + type.key(date);
    }

    public UUID uuid() {
        return this.uuid;
    }

    public String name() {
        return this.name;
    }

    public void name(final String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public long totalBlocks() {
        return this.totalBlocks;
    }

    public long totalSoldItems() {
        return this.totalSoldItems;
    }

    public double totalEarned() {
        return this.totalEarned;
    }

    public boolean dirty() {
        return this.dirty;
    }

    public void clean() {
        this.dirty = false;
    }

    public long revision() {
        return this.revision;
    }

    public void markSaved(final long savedRevision) {
        if (this.revision == savedRevision) {
            this.dirty = false;
        }
    }




    public PlayerStats snapshot() {
        final PlayerStats copy = new PlayerStats(this.uuid, this.name);
        copy.totalBlocks = this.totalBlocks;
        copy.totalSoldItems = this.totalSoldItems;
        copy.totalEarned = this.totalEarned;
        copy.mineBlocks.putAll(this.mineBlocks);
        for (final var entry : this.periods.entrySet()) {
            final PeriodValue value = new PeriodValue();
            value.blocks = entry.getValue().blocks;
            value.earned = entry.getValue().earned;
            copy.periods.put(entry.getKey(), value);
        }
        return copy;
    }



    public void mergeFrom(final PlayerStats loaded) {
        if (loaded == null) {
            return;
        }
        this.totalBlocks += loaded.totalBlocks;
        this.totalSoldItems += loaded.totalSoldItems;
        this.totalEarned += loaded.totalEarned;
        for (final var entry : loaded.mineBlocks.entrySet()) {
            this.mineBlocks.merge(entry.getKey(), entry.getValue(), Long::sum);
        }
        for (final var entry : loaded.periods.entrySet()) {
            final PeriodValue value = period(entry.getKey());
            value.blocks += entry.getValue().blocks;
            value.earned += entry.getValue().earned;
        }
        this.dirty = true;
    }

    public Map<String, Long> mineBlocks() {
        return this.mineBlocks;
    }

    public Map<String, PeriodValue> periods() {
        return this.periods;
    }



    public void setLoadedTotals(final long blocks, final long sold, final double earned) {
        this.totalBlocks = Math.max(0L, blocks);
        this.totalSoldItems = Math.max(0L, sold);
        this.totalEarned = Math.max(0.0D, earned);
    }

    public void addMined(final String mine, final long blocks) {
        if (blocks <= 0) {
            return;
        }
        final String mineKey = mine == null ? "unknown" : mine;
        this.mineBlocks.merge(mineKey, blocks, Long::sum);
        this.totalBlocks += blocks;
        final LocalDate today = LocalDate.now();
        period(periodKey(StatsPeriod.DAILY, today)).blocks += blocks;
        period(periodKey(StatsPeriod.WEEKLY, today)).blocks += blocks;
        period("ALL:ALL").blocks += blocks;
        this.dirty = true;
        ++this.revision;
        pruneIfNewDay(today);
    }

    public void addSold(final long items, final double money) {
        if (items <= 0 && money <= 0.0D) {
            return;
        }
        this.totalSoldItems += Math.max(0L, items);
        this.totalEarned += Math.max(0.0D, money);
        final LocalDate today = LocalDate.now();
        period(periodKey(StatsPeriod.DAILY, today)).earned += Math.max(0.0D, money);
        period(periodKey(StatsPeriod.WEEKLY, today)).earned += Math.max(0.0D, money);
        period("ALL:ALL").earned += Math.max(0.0D, money);
        this.dirty = true;
        ++this.revision;
        pruneIfNewDay(today);
    }

    private void pruneIfNewDay(final LocalDate today) {
        if (today.toEpochDay() == this.lastPruneEpochDay) {
            return;
        }
        this.lastPruneEpochDay = today.toEpochDay();
        prunePeriods(today);
    }

    public long blocks(final StatsPeriod type, final LocalDate date) {
        return period(periodKey(type, date)).blocks;
    }

    public double earned(final StatsPeriod type, final LocalDate date) {
        return period(periodKey(type, date)).earned;
    }

    private void prunePeriods(final LocalDate today) {
        this.periods.keySet().removeIf(key -> {
            if (key.startsWith("DAILY:")) {
                return !key.equals("DAILY:" + StatsPeriod.DAILY.key(today))
                        && !key.equals("DAILY:" + StatsPeriod.DAILY.key(today.minusDays(1)))
                        && key.compareTo("DAILY:" + StatsPeriod.DAILY.key(today.minusDays(31))) < 0;
            }
            if (key.startsWith("WEEKLY:")) {
                return !key.equals("WEEKLY:" + StatsPeriod.WEEKLY.key(today))
                        && !key.equals("WEEKLY:" + StatsPeriod.WEEKLY.key(today.minusWeeks(1)))
                        && key.compareTo("WEEKLY:" + StatsPeriod.WEEKLY.key(today.minusWeeks(12))) < 0;
            }
            return false;
        });
    }

    private PeriodValue period(final String key) {
        return this.periods.computeIfAbsent(key, ignored -> new PeriodValue());
    }

    public static final class PeriodValue {
        public long blocks;
        public double earned;
    }
}
