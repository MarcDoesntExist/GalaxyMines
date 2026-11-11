package me.marcdoesntexist.galaxymines.gui.objects;

import me.marcdoesntexist.galaxymines.api.events.inventory.InventoryObjectClickEvent;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicClickableInventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.interfaces.InventoryObjectClickListener;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.gui.GUIResourcesUtils;
import me.marcdoesntexist.galaxymines.gui.menus.MineMenu;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.Mine;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MineMaterialDisplay extends BasicClickableInventoryObject {
    private final Mine mine;
    protected Material material = null;

    public MineMaterialDisplay(final Mine m, final MineMenu menu) {
        super(GUIResourcesUtils.EMPTY_MATERIAL, menu);
        this.interactListener = new InventoryObjectClickListener() {

            @Override
            public void onInteract(final InventoryObjectClickEvent e) {
                final Player p = e.getPlayer();
                if (MineMaterialDisplay.this.material == null) {
                    final ItemStack item = e.getItemInHand();
                    if (item != null && item.getType() != Material.AIR
                            && item.getType().isBlock()) {
                        MineMaterialDisplay.this.mine.addMaterial(item.getType());
                        menu.update();
                    }
                } else if (e.getClickType()
                        == InventoryObjectClickEvent.InteractionType.INVENTORY_CLICK_LEFT) {
                    menu.openCompositionEditor(MineMaterialDisplay.this.material, p);
                } else if (e.getClickType()
                        == InventoryObjectClickEvent.InteractionType.INVENTORY_CLICK_RIGHT) {
                    MineMaterialDisplay.this.mine.removeMaterial(MineMaterialDisplay.this.material);
                    menu.update();
                }
            }
        };
        this.mine = m;
    }

    private static ItemStack generateCompositionItemStack(final Mine m, final Material material) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(ItemUtils.itemText(Messages.get("messages.gui.composition.entry",
                "%material%-%percentage%%", "%material%", material.name(),
                "%percentage%", m.getComposition().get(material))));
        final List<Component> lore = new ArrayList<>();
        lore.add(ItemUtils.itemText(GUIResourcesUtils.CLICK_TO_EDIT));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public Material getMaterial() {
        return this.material;
    }

    public void setMaterial(final Material material) {
        this.material = material;
        this.update();
    }

    @Override
    public void update() {
        this.icon = this.material != null
                ? MineMaterialDisplay.generateCompositionItemStack(this.mine, this.material)
                : GUIResourcesUtils.EMPTY_MATERIAL;
        this.icon.setAmount(1);
    }

    public Mine getMine() {
        return this.mine;
    }
}
