package me.marcdoesntexist.galaxymines.gui.menus;

import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionButton;
import me.marcdoesntexist.galaxymines.messages.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;









public class SellShopMenu extends BasicInventoryMenu {
    public static final String MENU = "sellshop";
    private static final String PERMISSION = "galaxymines.sell";

    public SellShopMenu(final String name, final int rows) {
        super(name, rows);
    }

    public static boolean available(final boolean economyEnabled, final boolean shopEnabled) {
        return economyEnabled && shopEnabled;
    }





    public static double payout(final double pricePerItem, final int amount,
                                final double multiplier) {
        if (!(pricePerItem > 0.0D) || amount <= 0 || !(multiplier > 0.0D)) {
            return 0.0D;
        }
        return pricePerItem * amount * multiplier;
    }

    public static void openShop(final Player player) {
        if (player == null) {
            return;
        }
        final PluginSettings.Economy economy = PluginSettings.get().economy;
        if (!available(economy.enabled, economy.shopEnabled)) {
            player.sendMessage(Text.parse(Messages.get("messages.sell.shopDisabled",
                    "<red>The sell shop is disabled.")));
            return;
        }
        if (!player.hasPermission(PERMISSION) && !player.hasPermission("galaxymines.admin")) {
            player.sendMessage(Text.parse(Messages.get("messages.command.noPermission",
                    "You do not have permission to use this command.")));
            return;
        }
        final SellShopMenu menu = new SellShopMenu(
                MenuConfig.title(MENU, "&6&lSell shop"), MenuConfig.rows(MENU, 6));
        menu.initialSetup();
        menu.open(player);
    }

    @Override
    protected void setupItems() {
        final ItemStack fill = MenuConfig.fill(MENU);
        if (fill != null) {
            this.fill(fill);
        }
        for (final String key : MenuConfig.itemKeys(MENU)) {
            final int slot = MenuConfig.slot(MENU, key, -1);
            final String action = MenuConfig.action(MENU, key, null);
            if (slot < 0 || action == null) {
                continue;
            }
            final ItemStack icon = MenuConfig.item(MENU, key);
            if (icon == null) {
                continue;
            }
            this.registerInventoryObject(new MenuActionButton(icon, action, this, null, null,
                    MenuConfig.actionArgument(MENU, key, null)), slot);
        }
    }

    @Override
    public void update() {
    }
}
