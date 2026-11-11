package me.marcdoesntexist.galaxymines.api.utils.inventory.objects;

import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.interfaces.ClickableInventoryObject;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public interface InventoryContainer<T extends InventoryObject>
        extends InventoryObject, ClickableInventoryObject {
    void setItem(final int p0, final ItemStack p1);

    void addItem(final ItemStack p0);

    void setObject(final int p0, final T p1);

    void addObject(final T p0);

    boolean containsSlot(final int p0);

    boolean canPlaceIn(final int p0);

    void removeItem(final int p0);

    void clear();

    Map<Integer, T> getInventoryObjects();

    int getSize();
}
