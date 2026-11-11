package me.marcdoesntexist.galaxymines;

import me.marcdoesntexist.galaxymines.messages.GalaxyMineMessageManager;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.mines.MineManager;
import me.marcdoesntexist.galaxymines.storage.StorageManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class GalaxyMineAPI {
    public static Mine getMine(final String name) {
        return MineManager.getMine(name);
    }

    public static Mine getMineWith(final Player player) {
        return MineManager.getMineWith(player);
    }


    public static Mine getMineAt(final Location location) {
        return MineManager.getMineAt(location);
    }


    public static List<Mine> getMines() {
        return MineManager.getMines();
    }


    public static List<Mine> getMinesInWorld(final World world) {
        return me.marcdoesntexist.galaxymines.mines.MineIndex.getMinesInWorld(world);
    }


    public static Mine createMine(final String name, final Location first,
                                  final Location second) {
        return MineManager.createCuboid(name, first, second);
    }


    public static boolean deleteMine(final String name) {
        final Mine mine = MineManager.getMine(name);
        if (mine == null) {
            return false;
        }
        mine.delete();
        return true;
    }


    public static boolean resetMine(final String name) {
        final Mine mine = MineManager.getMine(name);
        if (mine == null) {
            return false;
        }
        mine.reset();
        return true;
    }


    public static boolean renameMine(final String name, final String newName) {
        return MineManager.renameMine(MineManager.getMine(name), newName);
    }


    public static boolean isValidMineName(final String name) {
        return MineManager.isValidName(name);
    }

    public static StorageManager.MineStats getStatistics(final Mine mine) {
        return StorageManager.getStats(mine);
    }

    public static boolean isPlayerInAMine(final Player player) {
        return MineManager.isPlayerInAMine(player);
    }

    public static String translateTags(final String input) {
        return GalaxyMineMessageManager.getInstance().translateAll(input);
    }

    public static String translateTagsWithMine(final Mine mine, final String input) {
        return GalaxyMineMessageManager.getInstance().translateAll(input, mine);
    }


    public static boolean registerCurrency(
            final me.marcdoesntexist.galaxymines.integration.economy.Currency currency) {
        return me.marcdoesntexist.galaxymines.integration.economy.CurrencyRegistry
                .register(currency);
    }


    public static List<String> registeredCurrencies() {
        return me.marcdoesntexist.galaxymines.integration.economy.CurrencyRegistry.ids();
    }


    public static boolean registerMultiplierSource(
            final me.marcdoesntexist.galaxymines.multiplier.MultiplierSource source) {
        return me.marcdoesntexist.galaxymines.multiplier.MultiplierSources.get().register(source);
    }


    public static List<String> registeredMultiplierSources() {
        return me.marcdoesntexist.galaxymines.multiplier.MultiplierSources.get().ids();
    }


    public static boolean registerDropSink(
            final me.marcdoesntexist.galaxymines.integration.drops.DropSink sink) {
        return me.marcdoesntexist.galaxymines.integration.drops.DropSinks.get().register(sink);
    }


    public static boolean unregisterDropSink(final String id) {
        return me.marcdoesntexist.galaxymines.integration.drops.DropSinks.get().unregister(id);
    }


    public static List<String> registeredDropSinks() {
        return me.marcdoesntexist.galaxymines.integration.drops.DropSinks.get().ids();
    }
}
