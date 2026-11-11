package me.marcdoesntexist.galaxymines.stats;

import java.util.UUID;

public record LeaderboardEntry(UUID uuid, String name, long blocks, double earned) {
}
