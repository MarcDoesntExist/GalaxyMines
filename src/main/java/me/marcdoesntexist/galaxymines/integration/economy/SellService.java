package me.marcdoesntexist.galaxymines.integration.economy;

import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.multiplier.BoosterService;
import me.marcdoesntexist.galaxymines.multiplier.BoosterType;
import me.marcdoesntexist.galaxymines.stats.PlayerStatsManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public final class SellService {
    private SellService() {
    }

    public static Sale sellHand(final Player player, final Mine mine) {
        return sellSingle(player, mine, player.getInventory().getItemInMainHand());
    }


    public static Sale sellInventory(final Player player, final Mine mine) {
        final Currency currency = currencyFor(mine);
        if (currency == null) {
            return new Sale(0.0D, 0);
        }
        final SellPriority priority = priorityFor(mine);
        final double multiplier = multiplier(player, mine);
        final List<ItemStack> sold = new ArrayList<>();
        double total = 0.0D;
        int count = 0;
        final ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; ++slot) {
            final ItemStack stack = contents[slot];
            if (stack == null) {
                continue;
            }
            final double price = PriceRegistry.price(stack, priority);
            if (price <= 0.0D) {
                continue;
            }
            total += price * stack.getAmount() * multiplier;
            count += stack.getAmount();
            sold.add(stack.clone());
            contents[slot] = null;
        }
        if (count > 0) {

            player.getInventory().setStorageContents(contents);
        }
        return finish(player, mine, total, count, sold, currency);
    }


    public static Sale sellDrops(final Player player, final Mine mine, final List<ItemStack> drops) {
        final Currency currency = currencyFor(mine);
        if (currency == null) {
            return new Sale(0.0D, 0);
        }
        final SellPriority priority = priorityFor(mine);
        final double multiplier = multiplier(player, mine);
        double total = 0.0D;
        int count = 0;
        final List<ItemStack> sold = new ArrayList<>();
        final Iterator<ItemStack> iterator = drops.iterator();
        while (iterator.hasNext()) {
            final ItemStack stack = iterator.next();
            final double price = PriceRegistry.price(stack, priority);
            if (price <= 0.0D) {
                continue;
            }
            total += price * stack.getAmount() * multiplier;
            count += stack.getAmount();
            sold.add(stack.clone());
            iterator.remove();
        }
        return finish(player, mine, total, count, sold, currency);
    }

    private static Sale sellSingle(final Player player, final Mine mine, final ItemStack stack) {
        final Currency currency = currencyFor(mine);
        if (currency == null || stack == null) {
            return new Sale(0.0D, 0);
        }
        final double price = PriceRegistry.price(stack, priorityFor(mine));
        if (price <= 0.0D) {
            return new Sale(0.0D, 0);
        }
        final ItemStack soldStack = stack.clone();
        final int amount = stack.getAmount();
        player.getInventory().setItemInMainHand(null);
        return finish(player, mine, price * amount * multiplier(player, mine), amount,
                List.of(soldStack), currency);
    }

    private static Sale finish(final Player player, final Mine mine, final double total,
                               final int count, final List<ItemStack> sold,
                               final Currency currency) {
        final String currencyId = currency == null
                ? CurrencyRegistry.defaultId() : currency.id();
        if (count == 0 || total <= 0.0D || currency == null || !currency.available()
                || !currency.deposit(player, total)) {
            returnItems(player, sold);
            return new Sale(0.0D, 0, currencyId);
        }


        if (currency.id().equals(CurrencyRegistry.defaultId())) {
            PlayerStatsManager.addSold(player, mine, count, total);
        }
        me.marcdoesntexist.galaxymines.history.HistoryService.get().record(player.getUniqueId(),
                player.getName(), mine == null ? "" : mine.getName(), total, count,
                currency.id());
        return new Sale(total, count, currency.id());
    }

    private static void returnItems(final Player player, final List<ItemStack> stacks) {
        for (final ItemStack stack : stacks) {
            final Map<Integer, ItemStack> leftover =
                    player.getInventory().addItem(stack.clone());
            for (final ItemStack dropped : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), dropped);
            }
        }
    }


    public static boolean isEnabled() {
        return PluginSettings.get().economy.enabled
                && CurrencyRegistry.available(CurrencyRegistry.defaultId());
    }


    static Currency currencyFor(final Mine mine) {
        final String id = mine == null
                ? CurrencyRegistry.defaultId() : mine.getSellCurrency();
        final Currency currency = CurrencyRegistry.get(id);
        if (currency != null && currency.available()) {
            return currency;
        }
        final Currency fallback = CurrencyRegistry.defaultCurrency();
        return fallback != null && fallback.available() ? fallback : null;
    }


    static SellPriority priorityFor(final Mine mine) {
        final SellPriority global = PluginSettings.get().economy.sellPriority;
        if (mine == null) {
            return global == null ? SellPriority.PROVIDER_FIRST : global;
        }
        final SellPriority own = mine.getSellPriority();
        return own == null || own == SellPriority.GLOBAL ? global : own;
    }


    public static double playerMultiplier(final Player player) {
        return player == null ? 1.0D : multiplier(player, null);
    }

    public static double multiplier(final Player player, final Mine mine) {
        double multiplier = 1.0D;
        for (final Map.Entry<String, Double> entry
                : PluginSettings.get().economy.sellMultipliers.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                multiplier = Math.max(multiplier, entry.getValue());
            }
        }
        if (mine != null) {
            multiplier *= Math.max(0.0D, mine.getDrops().sellMultiplier());
            multiplier *= 1.0D + Math.max(0.0D, mine.getProgressionSellBonus());
            multiplier *= me.marcdoesntexist.galaxymines.events.MineEventManager.get()
                    .sellMultiplier(mine);
        }


        multiplier *= BoosterService.get().multiplier(player, BoosterType.SELL);


        multiplier *= me.marcdoesntexist.galaxymines.multiplier.MultiplierSources.get()
                .sell(player);
        return multiplier;
    }

    public record Sale(double amount, int itemCount, String currency) {
        public Sale(final double amount, final int itemCount) {
            this(amount, itemCount, CurrencyRegistry.defaultId());
        }

        public boolean isEmpty() {
            return this.itemCount == 0 || this.amount <= 0.0D;
        }
    }
}
