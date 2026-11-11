package me.marcdoesntexist.galaxymines.api.utils.inventory;

import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.InventoryObject;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public interface InventoryMenu extends Listener {
    void open(final Player p0);

    void disable();

    InventoryMenu initialSetup();

    Inventory getBukkitInventory();

    Map<Integer, InventoryObject> getMenuMap();

    void registerInventoryObject(final InventoryObject p0, final int p1)
            throws IllegalArgumentException;

    void setItem(final ItemStack p0, final int p1);

    void update();

    boolean hasObjectInSlot(final int p0);

    InventoryObject getInventoryObject(final int p0);
}
