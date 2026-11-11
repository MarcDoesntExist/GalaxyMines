package me.marcdoesntexist.galaxymines.api.utils.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class MenuInventoryHolder implements InventoryHolder {
    private final BasicInventoryMenu menu;
    private Inventory inventory;

    public MenuInventoryHolder(final BasicInventoryMenu menu) {
        this.menu = menu;
    }

    public BasicInventoryMenu menu() {
        return this.menu;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    void setInventory(final Inventory inventory) {
        this.inventory = inventory;
    }
}
