package me.marcdoesntexist.galaxymines.api.utils.inventory.objects;

import me.marcdoesntexist.galaxymines.api.utils.inventory.InventoryMenu;
import org.bukkit.inventory.ItemStack;

public class BasicInventoryItem extends BasicInventoryObject {
    protected ItemStack icon;

    public BasicInventoryItem(final InventoryMenu menu, final ItemStack icon) {
        super(menu);
        this.icon = icon;
    }

    @Override
    public ItemStack getIcon() {
        return this.icon;
    }

    public void setIcon(final ItemStack icon) {
        this.icon = icon;
    }

    @Override
    public boolean hasItemStack() {
        return true;
    }
}
