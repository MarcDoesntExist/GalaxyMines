package me.marcdoesntexist.galaxymines.api.utils.inventory.objects;

import me.marcdoesntexist.galaxymines.api.CustomPlugin;
import me.marcdoesntexist.galaxymines.api.events.inventory.InventoryObjectClickEvent;
import me.marcdoesntexist.galaxymines.api.utils.inventory.InventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.InventoryUtils;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.interfaces.ClickableInventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.interfaces.InventoryObjectClickListener;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicInventoryContainer implements InventoryContainer {
    protected final int size;
    protected final int width;
    protected final int height;
    protected final Integer[] map;
    protected final Inventory inventory;
    protected final InventoryMenu menu;
    protected final Map<Integer, InventoryObject> inventoryObjects;
    private final InventoryObjectClickListener listener;

    public BasicInventoryContainer(final InventoryMenu menu, final int pos1, final int pos2) {
        this(menu, pos1, pos2, null);
    }

    public BasicInventoryContainer(final InventoryMenu menu, final int pos1, final int pos2,
                                   final InventoryObjectClickListener customListener) {
        this.inventoryObjects = new HashMap<Integer, InventoryObject>();
        this.menu = menu;
        this.inventory = menu.getBukkitInventory();
        this.map = InventoryUtils.getSquare(this.inventory, pos1, pos2);
        this.size = this.map.length;
        final int[] reference = InventoryUtils.getReferencePoints(this.inventory, pos1, pos2);
        this.width = reference[2] - reference[0] + 1;
        this.height = reference[3] - reference[1] + 1;
        this.listener = customListener == null ? new ContainerClickHandler() : customListener;
    }

    private static void debug(final String message) {
        if (CustomPlugin.instance != null) {
            CustomPlugin.instance.debug(message);
        }
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
    public void removeItem(final int slot) {
        if (this.hasObjectIn(slot)) {
            this.inventoryObjects.remove(slot);
        }
    }

    public void clearAndFill(final ItemStack item) {
        this.clear();
        this.fill(item);
    }

    public void fill(final ItemStack item) {
        for (int index = 0; index < this.map.length; ++index) {
            final int slot = this.map[index];
            if (this.canPlaceIn(slot)) {
                debug("Placing item " + ItemUtils.toString(item) + " in slot " + slot);
                this.setItem(index, item);
            } else {
                debug("Could not place item " + ItemUtils.toString(item) + " in slot " + slot
                        + " while filling");
            }
        }
    }

    @Override
    public int getSize() {
        return this.size;
    }


    @Override
    public void setItem(final int index, final ItemStack item) {
        if (this.map.length > index) {
            this.inventory.setItem(this.map[index], item);
        }
    }


    @Override
    public void setObject(final int id, final InventoryObject item) {
        if (this.map.length > id) {
            final int slot = this.map[id];
            this.inventoryObjects.put(slot, item);
            final boolean hasItemStack = item.hasItemStack();
            if (hasItemStack) {
                final ItemStack itemStack = item.getIcon();
                this.inventory.setItem(slot, itemStack);
            }
            if (item.hasMultiSlots()) {
                final List<Integer> slots = item.getMultiSlots();
                for (final int i : slots) {
                    if (i != slot) {
                        this.inventoryObjects.put(i, item);
                        if (!hasItemStack) {
                            continue;
                        }
                        this.inventory.setItem(i, item.getIcon());
                    }
                }
            }
        }
    }

    @Override
    public void clear() {
        for (final int i : this.map) {
            this.inventory.setItem(i, null);
            this.removeItem(i);
        }
    }

    public boolean hasObjectIn(final int slot) {
        return this.inventoryObjects.containsKey(slot);
    }


    @Override
    public boolean canPlaceIn(final int slot) {
        return !this.hasObjectIn(slot);
    }

    @Override
    public boolean containsSlot(final int slot) {
        for (final int m : this.map) {
            if (m == slot) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InventoryMenu getMenu() {
        return this.menu;
    }

    @Override
    public void addObject(final InventoryObject itemIcon) {
        final int index = this.firstFreeIndex();
        if (index >= 0) {
            this.setObject(index, itemIcon);
        }
    }

    @Override
    public void addItem(final ItemStack item) {
        final int index = this.firstFreeIndex();
        if (index >= 0) {
            this.setItem(index, item);
        }
    }


    private int firstFreeIndex() {
        for (int index = 0; index < this.map.length; ++index) {
            if (this.canPlaceIn(this.map[index])) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public InventoryObjectClickListener getInteractListener() {
        return this.listener;
    }

    @Override
    public boolean hasMultiSlots() {
        return true;
    }

    @Override
    public List<Integer> getMultiSlots() {
        return Arrays.asList(this.map);
    }

    @Override
    public String toString() {
        String title = "";
        if (!this.inventory.getViewers().isEmpty()) {
            title = this.inventory.getViewers().get(0).getOpenInventory().getTitle() + " - ";
        }
        return title + this.getClass().getSimpleName() + "@" + this.hashCode();
    }

    @Override
    public void update() {
        for (final int i : this.inventoryObjects.keySet()) {
            final InventoryObject o = this.inventoryObjects.get(i);
            o.update();
            this.menu.getBukkitInventory().setItem(i, o.getIcon());
        }
    }

    @Override
    public Map<Integer, InventoryObject> getInventoryObjects() {
        return this.inventoryObjects;
    }

    private class ContainerClickHandler implements InventoryObjectClickListener {
        @Override
        public void onInteract(final InventoryObjectClickEvent e) {
            final int clickedSlot = e.getClickedSlot();
            if (BasicInventoryContainer.this.inventoryObjects.containsKey(clickedSlot)) {
                final InventoryObject obj =
                        BasicInventoryContainer.this.inventoryObjects.get(clickedSlot);
                if (obj instanceof ClickableInventoryObject) {
                    final ClickableInventoryObject c = (ClickableInventoryObject) obj;
                    c.getInteractListener().onInteract(e);
                }
            }
        }
    }
}
