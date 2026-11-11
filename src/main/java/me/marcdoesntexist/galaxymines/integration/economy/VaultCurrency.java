package me.marcdoesntexist.galaxymines.integration.economy;

import org.bukkit.entity.Player;


public final class VaultCurrency implements Currency {
    public static final String ID = "money";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Money";
    }

    @Override
    public boolean available() {
        return EconomyService.isAvailable();
    }

    @Override
    public boolean deposit(final Player player, final double amount) {
        return EconomyService.deposit(player, amount);
    }

    @Override
    public String format(final double amount) {
        return EconomyService.format(amount);
    }
}
