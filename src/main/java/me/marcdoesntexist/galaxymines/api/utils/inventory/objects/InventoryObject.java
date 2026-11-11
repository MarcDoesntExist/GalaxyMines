package me.marcdoesntexist.galaxymines.api.utils.inventory.objects;

import me.marcdoesntexist.galaxymines.api.utils.inventory.InventoryMenu;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface InventoryObject {
    InventoryMenu getMenu();

    boolean hasMultiSlots();

    List<Integer> getMultiSlots();

    boolean hasItemStack();

    ItemStack getIcon();

    void update();
}
