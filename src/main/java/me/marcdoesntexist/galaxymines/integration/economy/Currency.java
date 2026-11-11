package me.marcdoesntexist.galaxymines.integration.economy;

import org.bukkit.entity.Player;

import java.util.Locale;









public interface Currency {


    String id();



    default String displayName() {
        return id();
    }



    boolean available();




    boolean deposit(Player player, double amount);



    default String format(final double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }
}
