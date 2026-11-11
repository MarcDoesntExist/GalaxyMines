package me.marcdoesntexist.galaxymines.api.events.inventory;

import me.marcdoesntexist.galaxymines.api.events.CustomEvent;
import me.marcdoesntexist.galaxymines.api.utils.inventory.InventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.InventoryObject;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;

public class InventoryObjectClickEvent extends CustomEvent implements Cancellable {
    private final InventoryObject object;
    private final InventoryMenu menu;
    private final Player player;
    private final int clickedSlot;
    private final InteractionType clickType;
    private final Block placedBlock;
    private final ItemStack itemInHand;
    private boolean cancelled;

    public InventoryObjectClickEvent(final InventoryObject object, final int slot,
                                     final Player clicker, final InteractionType click, final Block block,
                                     final ItemStack itemInHand) {
        this.object = object;
        this.menu = object.getMenu();
        this.player = clicker;
        this.clickedSlot = slot;
        this.clickType = click;
        this.placedBlock = block;
        this.itemInHand = itemInHand;
    }

    public InventoryMenu getMenu() {
        return this.menu;
    }

    public InventoryObject getObject() {
        return this.object;
    }

    public Player getPlayer() {
        return this.player;
    }

    public int getClickedSlot() {
        return this.clickedSlot;
    }

    public InteractionType getClickType() {
        return this.clickType;
    }

    public Block getPlacedBlock() {
        return this.placedBlock;
    }

    public ItemStack getItemInHand() {
        return this.itemInHand;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    public enum InteractionType {
        INVENTORY_CLICK_RIGHT, INVENTORY_CLICK_LEFT, INTERACT_CLICK_RIGHT, INTERACT_CLICK_LEFT
    }
}
