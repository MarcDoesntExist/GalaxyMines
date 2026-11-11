package me.marcdoesntexist.galaxymines.api.utils.inventory.objects;

import me.marcdoesntexist.galaxymines.api.utils.inventory.InventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.interfaces.ClickableInventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.interfaces.InventoryObjectClickListener;
import org.bukkit.inventory.ItemStack;

public class BasicClickableInventoryObject extends BasicInventoryItem
        implements ClickableInventoryObject {
    protected InventoryObjectClickListener interactListener;

    public BasicClickableInventoryObject(ItemStack itemStack,
                                         InventoryObjectClickListener interactListener, InventoryMenu menu) {
        super(menu, itemStack);
        this.interactListener = interactListener;
    }

    public BasicClickableInventoryObject(ItemStack itemStack, InventoryMenu menu) {
        super(menu, itemStack);
    }

    public InventoryObjectClickListener getInteractListener() {
        return this.interactListener;
    }

    public void setInteractListener(InventoryObjectClickListener interactListener) {
        this.interactListener = interactListener;
    }
}
