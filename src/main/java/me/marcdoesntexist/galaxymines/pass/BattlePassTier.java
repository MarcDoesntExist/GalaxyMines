package me.marcdoesntexist.galaxymines.pass;

import java.util.List;


public record BattlePassTier(int level, long xp, double money, List<String> commands) {

    public BattlePassTier {
        commands = commands == null ? List.of() : List.copyOf(commands);
    }
}
