package me.marcdoesntexist.galaxymines.api.utils.inventory;

import me.marcdoesntexist.galaxymines.api.CustomPlugin;
import me.marcdoesntexist.galaxymines.api.events.inventory.InventoryObjectClickEvent;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.InventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.interfaces.ClickableInventoryObject;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public abstract class BasicInventoryMenu implements InventoryMenu {
    protected final HashMap<Integer, InventoryObject> objects;
    protected final Inventory mainInventory;
    protected final MenuInventoryHolder holder;

    public BasicInventoryMenu(final String name, final int totalLanes) {
        this.objects = new HashMap<Integer, InventoryObject>();
        this.holder = new MenuInventoryHolder(this);
        this.mainInventory = InventoryUtils.createInventory(name, totalLanes, this.holder);
        this.holder.setInventory(this.mainInventory);
    }

    public BasicInventoryMenu(final Inventory bukkitInventory) {
        this.objects = new HashMap<Integer, InventoryObject>();
        this.holder = null;
        this.mainInventory = bukkitInventory;
    }

    private static void dispatch(final InventoryObject object,
                                 final InventoryObjectClickEvent event) {
        if (object instanceof ClickableInventoryObject clickable
                && clickable.getInteractListener() != null) {
            clickable.getInteractListener().onInteract(event);
        }
    }

    public void fill(final ItemStack i) {
        for (int j = 0; j < this.mainInventory.getSize(); ++j) {
            this.mainInventory.setItem(j, i);
        }
    }

    public void clear() {
        this.mainInventory.clear();
        this.objects.clear();
    }

    public void clearAndFill(final ItemStack i) {
        this.clear();
        this.fill(i);
    }

    @Override
    public HashMap<Integer, InventoryObject> getMenuMap() {
        return this.objects;
    }

    @Override
    public void registerInventoryObject(final InventoryObject o, final int initialSlot)
            throws IllegalArgumentException {
        if (this.hasObjectInSlot(initialSlot)) {
            throw new IllegalArgumentException(
                    "Slot " + initialSlot + " is already in use by " + this
                            .getInventoryObject(initialSlot).getClass().getSimpleName() + "!");
        }
        this.objects.put(initialSlot, o);
        this.mainInventory.setItem(initialSlot, o.getIcon());
        if (o.hasMultiSlots()) {
            for (final int slot : o.getMultiSlots()) {
                if (slot != initialSlot) {
                    this.objects.put(slot, o);
                }
            }
        }
    }

    @Override
    public InventoryMenu initialSetup() {
        if (this.holder == null) {
            CustomPlugin.registerListener(this);
        }
        this.setupItems();
        return this;
    }

    protected abstract void setupItems();

    @Override
    public Inventory getBukkitInventory() {
        return this.mainInventory;
    }

    @Override
    public void open(final Player p) {
        this.update();
        p.openInventory(this.mainInventory);
    }

    public void handleClick(final InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(this.mainInventory)) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        final int slot = e.getSlot();
        final InventoryObject object = this.objects.get(slot);
        if (object == null) {
            return;
        }
        final InventoryObjectClickEvent.InteractionType type = e.isLeftClick()
                ? InventoryObjectClickEvent.InteractionType.INVENTORY_CLICK_LEFT
                : InventoryObjectClickEvent.InteractionType.INVENTORY_CLICK_RIGHT;
        final InventoryObjectClickEvent event = new InventoryObjectClickEvent(object, slot,
                player, type, null, e.getCursor());
        event.call();
        if (event.isCancelled()) {
            return;
        }
        dispatch(object, event);
    }

    @Override
    public void setItem(final ItemStack itemStack, final int slot)
            throws IllegalArgumentException {
        if (this.hasObjectInSlot(slot)) {
            throw new IllegalArgumentException(
                    "Slot " + slot + " is already in use by " + this.getInventoryObject(slot).getClass()
                            .getSimpleName() + "!");
        }
        this.mainInventory.setItem(slot, itemStack);
    }

    @Override
    public boolean hasObjectInSlot(final int slot) {
        return this.objects.containsKey(slot);
    }

    @Override
    public InventoryObject getInventoryObject(final int slot) {
        return this.objects.get(slot);
    }

    @Override
    public void disable() {
        if (this.holder == null) {
            CustomPlugin.unregisterListener(this);
        }
    }
}
