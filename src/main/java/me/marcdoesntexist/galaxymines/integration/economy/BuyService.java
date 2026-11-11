package me.marcdoesntexist.galaxymines.integration.economy;

import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.messages.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Locale;
import java.util.Map;











public final class BuyService {

    public enum Result {
        BOUGHT, DISABLED, PROVIDER_MISSING, UNKNOWN_ITEM, NOT_PRICED, FULL, INSUFFICIENT
    }

    private BuyService() {
    }

    public static boolean isEnabled() {
        final PluginSettings.Economy economy = PluginSettings.get().economy;
        return economy.enabled && economy.buyEnabled && EconomyService.isAvailable();
    }

    



    public static double quote(final Material material, final int amount, final double markup) {
        if (material == null || amount <= 0) {
            return 0.0D;
        }
        final double unit = PriceRegistry.price(material);
        if (unit <= 0.0D) {
            return 0.0D;
        }
        final double safeMarkup = Double.isFinite(markup) && markup > 0.0D ? markup : 1.0D;
        return Math.round(unit * amount * safeMarkup * 100.0D) / 100.0D;
    }

    public static Result buy(final Player player, final String rawMaterial, final int amount) {
        if (!isEnabled()) {
            final Result result = EconomyService.isAvailable()
                    ? Result.DISABLED : Result.PROVIDER_MISSING;
            player.sendMessage(Messages.get(result == Result.DISABLED
                    ? "messages.buy.disabled" : "messages.buy.providerMissing",
                    result == Result.DISABLED
                            ? "<red>The buy shop is disabled."
                            : "<red>No economy provider is available."));
            return result;
        }
        final Material material = parse(rawMaterial);
        if (material == null || !material.isItem() || amount <= 0) {
            player.sendMessage(Messages.get("messages.buy.unknownItem",
                    "<red>%item% is not a buyable item.", "%item%", String.valueOf(rawMaterial)));
            return Result.UNKNOWN_ITEM;
        }
        final PluginSettings.Economy economy = PluginSettings.get().economy;
        final double cost = quote(material, amount, economy.buyMarkup);
        if (cost <= 0.0D) {
            player.sendMessage(Messages.get("messages.buy.notPriced",
                    "<red>%item% has no price in prices.yml.",
                    "%item%", material.name().toLowerCase(Locale.ROOT)));
            return Result.NOT_PRICED;
        }
        if (!hasRoom(player.getInventory(), material, amount)) {
            player.sendMessage(Messages.get("messages.buy.full",
                    "<red>Your inventory has no room for %amount%x %item%.",
                    "%amount%", Integer.toString(amount),
                    "%item%", material.name().toLowerCase(Locale.ROOT)));
            return Result.FULL;
        }
        if (!EconomyService.withdraw(player, cost)) {
            player.sendMessage(Messages.get("messages.buy.insufficient",
                    "<red>You need %price% to buy that.",
                    "%price%", EconomyService.format(cost)));
            return Result.INSUFFICIENT;
        }
        give(player, material, amount);
        player.sendMessage(Messages.get("messages.buy.bought",
                "<green>Bought %amount%x %item% for %price%.",
                "%amount%", Integer.toString(amount),
                "%item%", material.name().toLowerCase(Locale.ROOT),
                "%price%", EconomyService.format(cost)));
        return Result.BOUGHT;
    }

    



    static boolean hasRoom(final PlayerInventory inventory, final Material material, final int amount) {
        if (inventory == null || material == null || amount <= 0) {
            return false;
        }
        final int max = Math.max(1, material.getMaxStackSize());
        int space = 0;
        for (final ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                space += max;
            } else if (slot.getType() == material) {
                space += Math.max(0, slot.getMaxStackSize() - slot.getAmount());
            }
            if (space >= amount) {
                return true;
            }
        }
        return space >= amount;
    }

    private static void give(final Player player, final Material material, final int amount) {
        int remaining = amount;
        final int max = Math.max(1, material.getMaxStackSize());
        while (remaining > 0) {
            final int size = Math.min(max, remaining);
            final Map<Integer, ItemStack> leftover =
                    player.getInventory().addItem(new ItemStack(material, size));
            for (final ItemStack rest : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
            remaining -= size;
        }
    }

    static Material parse(final String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Material.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
