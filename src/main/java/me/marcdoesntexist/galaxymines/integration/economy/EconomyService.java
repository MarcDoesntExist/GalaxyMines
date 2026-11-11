package me.marcdoesntexist.galaxymines.integration.economy;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;


public final class EconomyService {
    private static Object economy;
    private static Method isEnabled;
    private static Method getBalance;
    private static Method depositPlayer;
    private static Method withdrawPlayer;
    private static Method format;
    private static boolean available;

    private EconomyService() {
    }

    public static boolean setup() {
        available = false;
        economy = null;
        isEnabled = null;
        getBalance = null;
        depositPlayer = null;
        withdrawPlayer = null;
        format = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        try {
            final Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            final Object registration = Bukkit.getServicesManager()
                    .getRegistration((Class) economyClass);
            if (registration == null) {
                return false;
            }
            final Method getProvider = registration.getClass().getMethod("getProvider");
            economy = getProvider.invoke(registration);
            isEnabled = economyClass.getMethod("isEnabled");
            getBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
            depositPlayer = economyClass.getMethod("depositPlayer", OfflinePlayer.class,
                    double.class);
            withdrawPlayer = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class,
                    double.class);
            format = economyClass.getMethod("format", double.class);
            available = Boolean.TRUE.equals(isEnabled.invoke(economy));
        } catch (Throwable t) {
            GalaxyMines.getInstance().debug(
                    "Vault is installed but no economy provider could be resolved: "
                            + t.getMessage(), true);
            economy = null;
        }
        return available;
    }

    public static boolean isAvailable() {
        if (!available || economy == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(isEnabled.invoke(economy));
        } catch (Throwable t) {
            return false;
        }
    }

    public static double getBalance(final Player player) {
        if (!isAvailable()) {
            return 0.0D;
        }
        try {
            return ((Number) getBalance.invoke(economy, player)).doubleValue();
        } catch (Throwable t) {
            return 0.0D;
        }
    }

    public static boolean deposit(final Player player, final double amount) {
        if (!isAvailable() || amount <= 0.0D) {
            return false;
        }
        try {
            final Object response = depositPlayer.invoke(economy, player, amount);
            return response == null || Boolean.TRUE.equals(
                    response.getClass().getMethod("transactionSuccess").invoke(response));
        } catch (Throwable t) {
            GalaxyMines.getInstance().printException(
                    "Could not deposit money through Vault for " + player.getName(), t);
            return false;
        }
    }

    public static boolean withdraw(final Player player, final double amount) {
        if (!isAvailable() || amount <= 0.0D) {
            return false;
        }
        try {
            final Object response = withdrawPlayer.invoke(economy, player, amount);
            return response == null || Boolean.TRUE.equals(
                    response.getClass().getMethod("transactionSuccess").invoke(response));
        } catch (Throwable t) {
            GalaxyMines.getInstance().printException(
                    "Could not withdraw money through Vault for " + player.getName(), t);
            return false;
        }
    }

    public static String format(final double amount) {
        if (format != null && isAvailable()) {
            try {
                return String.valueOf(format.invoke(economy, amount));
            } catch (Throwable ignored) {
            }
        }
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }
}
