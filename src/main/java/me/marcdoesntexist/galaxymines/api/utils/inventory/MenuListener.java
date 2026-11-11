package me.marcdoesntexist.galaxymines.api.utils.inventory;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public final class MenuListener implements Listener {
    @EventHandler
    public void onClick(final InventoryClickEvent event) {
        final InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof MenuInventoryHolder menuHolder) {
            menuHolder.menu().handleClick(event);
        }
    }

    @EventHandler
    public void onDrag(final InventoryDragEvent event) {
        final InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof MenuInventoryHolder) {
            event.setCancelled(true);
        }
    }
}
