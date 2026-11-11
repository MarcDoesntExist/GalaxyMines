package me.marcdoesntexist.galaxymines.gui.objects;

import me.marcdoesntexist.galaxymines.api.utils.inventory.InventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicClickableInventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.gui.GUIResourcesUtils;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionContext;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionRegistry;
import me.marcdoesntexist.galaxymines.gui.menus.MainMenu;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.inventory.ItemStack;

public class MineDisplay extends BasicClickableInventoryObject {
    private final Mine mine;

    public MineDisplay(Mine m, InventoryMenu menu) {
        super(m.getIcon(), menu);
        this.mine = m;
        this.interactListener = e -> MenuActionRegistry.execute(
                MenuConfig.action("main", "mine", "open_mine"),
                new MenuActionContext(e.getPlayer(), e.getItemInHand(), this.mine, null, null,
                        MenuConfig.actionArgument("main", "mine", null)));
    }

    public MineDisplay(final Mine mine, ItemStack is, MainMenu menu) {
        super(ItemUtils.addToLore(is, GUIResourcesUtils.CLICK_TO_EDIT), menu);
        this.mine = mine;
        this.interactListener = e -> MenuActionRegistry.execute(
                MenuConfig.action("main", "mine", "open_mine"),
                new MenuActionContext(e.getPlayer(), e.getItemInHand(), mine, null, null,
                        MenuConfig.actionArgument("main", "mine", null)));
    }

    public Mine getMine() {
        return this.mine;
    }

}

