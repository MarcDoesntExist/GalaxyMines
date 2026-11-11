package me.marcdoesntexist.galaxymines.gui.actions;

import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicClickableInventoryObject;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.inventory.ItemStack;


public class MenuActionButton extends BasicClickableInventoryObject {
    public MenuActionButton(final ItemStack icon, final String actionId,
                            final BasicInventoryMenu menu, final Mine mine, final Object payload,
                            final String argument) {
        super(icon, e -> MenuActionRegistry.execute(actionId,
                        new MenuActionContext(e.getPlayer(), e.getItemInHand(), mine, menu, payload, argument)),
                menu);
    }
}
