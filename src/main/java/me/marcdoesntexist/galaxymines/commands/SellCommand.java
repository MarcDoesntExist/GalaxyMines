package me.marcdoesntexist.galaxymines.commands;

import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.integration.economy.CurrencyRegistry;
import me.marcdoesntexist.galaxymines.integration.economy.SellService;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.mines.MineManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public final class SellCommand {
    private static final String SELL_PERMISSION = "galaxymines.sell";

    public void sellHand(final CommandSender sender) {
        this.sell(sender, false);
    }

    public void sellAll(final CommandSender sender) {
        this.sell(sender, true);
    }

    private void sell(final CommandSender sender, final boolean all) {
        if (!(sender instanceof Player player)) {
            this.send(sender, "messages.command.playerOnly",
                    "<red>This command can only be used by a player.");
            return;
        }
        if (!player.hasPermission(SELL_PERMISSION)
                && !player.hasPermission("galaxymines.admin")) {
            this.send(sender, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        if (!SellService.isEnabled()) {
            this.send(sender, "messages.sell.disabled",
                    "<red>Selling is disabled: the economy is not available.");
            return;
        }
        final Mine mine = MineManager.getMineAt(player.getLocation());
        final SellService.Sale sale = all
                ? SellService.sellInventory(player, mine)
                : SellService.sellHand(player, mine);
        if (sale.isEmpty()) {
            this.send(sender, "messages.sell.nothing",
                    "<red>You have nothing that can be sold.");
            return;
        }
        this.send(sender, "messages.sell.success",
                "<green>Sold %items% items for %amount%.",
                "%items%", String.valueOf(sale.itemCount()),
                "%amount%", CurrencyRegistry.format(sale.currency(), sale.amount()));
    }

    private void send(final CommandSender sender, final String key, final String fallback,
                      final Object... replacements) {
        sender.sendMessage(Text.parse(Messages.get(key, fallback, replacements)));
    }
}
