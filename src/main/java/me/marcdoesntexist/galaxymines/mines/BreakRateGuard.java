package me.marcdoesntexist.galaxymines.mines;

import java.util.*;


public final class BreakRateGuard {

    private static final long WARNING_INTERVAL_MILLIS = 3_000L;

    private final Map<UUID, Deque<Long>> history = new HashMap<>();
    private final Map<UUID, Long> lastWarning = new HashMap<>();


    public boolean allow(final UUID player, final long now, final int maxPerSecond) {
        if (player == null || maxPerSecond <= 0) {
            return true;
        }
        final Deque<Long> timestamps = history.computeIfAbsent(player, ignored -> new ArrayDeque<>());
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= 1_000L) {
            timestamps.pollFirst();
        }
        if (timestamps.size() >= maxPerSecond) {
            return false;
        }
        timestamps.addLast(now);
        return true;
    }


    public boolean shouldWarn(final UUID player, final long now) {
        final Long previous = lastWarning.get(player);
        if (previous != null && now - previous < WARNING_INTERVAL_MILLIS) {
            return false;
        }
        lastWarning.put(player, now);
        return true;
    }

    public void forget(final UUID player) {
        history.remove(player);
        lastWarning.remove(player);
    }
}
