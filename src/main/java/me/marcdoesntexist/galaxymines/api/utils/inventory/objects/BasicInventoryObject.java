package me.marcdoesntexist.galaxymines.api.utils.inventory.objects;

import me.marcdoesntexist.galaxymines.api.utils.inventory.InventoryMenu;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class BasicInventoryObject implements InventoryObject {
    protected final InventoryMenu menu;

    public BasicInventoryObject(final InventoryMenu menu) {
        this.menu = menu;
    }

    @Override
    public InventoryMenu getMenu() {
        return this.menu;
    }

    @Override
    public boolean hasMultiSlots() {
        return false;
    }

    @Override
    public List<Integer> getMultiSlots() {
        return null;
    }

    @Override
    public boolean hasItemStack() {
        return false;
    }

    @Override
    public ItemStack getIcon() {
        return null;
    }

    @Override
    public void update() {
    }
}
