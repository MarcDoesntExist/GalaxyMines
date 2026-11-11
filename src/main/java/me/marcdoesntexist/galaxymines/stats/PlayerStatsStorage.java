package me.marcdoesntexist.galaxymines.stats;

import java.util.List;
import java.util.UUID;


public interface PlayerStatsStorage {
    void init() throws Exception;

    PlayerStats load(UUID uuid, String name) throws Exception;

    void save(PlayerStats stats) throws Exception;

    List<LeaderboardEntry> leaderboard(StatsPeriod period, String key, int limit)
            throws Exception;

    void close();
}
