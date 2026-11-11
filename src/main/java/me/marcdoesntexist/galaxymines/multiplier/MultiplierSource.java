package me.marcdoesntexist.galaxymines.multiplier;

import org.bukkit.entity.Player;


public interface MultiplierSource {


    String id();


    default double dropMultiplier(final Player player) {
        return 1.0D;
    }


    default double sellMultiplier(final Player player) {
        return 1.0D;
    }
}
