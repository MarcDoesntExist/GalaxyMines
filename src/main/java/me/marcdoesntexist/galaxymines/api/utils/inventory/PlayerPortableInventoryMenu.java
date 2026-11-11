package me.marcdoesntexist.galaxymines.api.utils.inventory;

import me.marcdoesntexist.galaxymines.api.events.inventory.InventoryObjectClickEvent;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.InventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.interfaces.ClickableInventoryObject;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public abstract class PlayerPortableInventoryMenu extends BasicInventoryMenu {
    public PlayerPortableInventoryMenu(final Player p) {
        super(p.getInventory());
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        final ItemStack item = e.getItem();
        final Player player = e.getPlayer();
        final PlayerInventory inventory = player.getInventory();
        if (inventory.equals(this.mainInventory)) {
            e.setCancelled(true);
            if (item != null) {
                final int slot = this.mainInventory.first(item);
                if (slot >= 0 && this.hasObjectInSlot(slot)) {
                    final Action action = e.getAction();
                    InventoryObjectClickEvent.InteractionType it;
                    if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
                        if (action != Action.RIGHT_CLICK_AIR
                                && action != Action.RIGHT_CLICK_BLOCK) {
                            return;
                        }
                    }
                    it = InventoryObjectClickEvent.InteractionType.INTERACT_CLICK_RIGHT;
                    Block finalBlock = null;
                    if (e.getClickedBlock() != null) {
                        finalBlock = e.getClickedBlock().getRelative(e.getBlockFace());
                    }
                    final PlayerInventory inv = player.getInventory();
                    final InventoryObject object = this.getInventoryObject(slot);
                    final InventoryObjectClickEvent event = new InventoryObjectClickEvent(object,
                            slot, player, it, finalBlock, inv.getItem(inv.getHeldItemSlot()));
                    if (object instanceof ClickableInventoryObject clickable
                            && clickable.getInteractListener() != null) {
                        clickable.getInteractListener().onInteract(event);
                    }
                    event.call();
                }
            }
        }
    }
}
