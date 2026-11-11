package me.marcdoesntexist.galaxymines.gui.objects;

import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.InventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicClickableInventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionContext;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionRegistry;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MaterialCompositionChanger extends BasicClickableInventoryObject {
    private final Mine mine;
    private final double percentage;
    private final Material material;

    public MaterialCompositionChanger(final Material material, final InventoryMenu menu,
                                      final Mine m, final double percentage) {
        super(generateCompositionChangeItemStack(percentage, m, material), e ->
                MenuActionRegistry.execute(MenuConfig.action("composition", "changers",
                                "change_percentage"),
                        new MenuActionContext(e.getPlayer(), e.getItemInHand(), m,
                                menu instanceof BasicInventoryMenu ? (BasicInventoryMenu) menu : null,
                                new MenuActionContext.PercentageChange(material, percentage),
                                MenuConfig.actionArgument("composition", "changers", null))), menu);
        this.material = material;
        this.mine = m;
        this.percentage = percentage;
    }

    private static ItemStack generateCompositionChangeItemStack(final double change,
                                                                final Mine mine, final Material material) {
        final boolean add = change > 0.0;
        final Material m = add ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        final String itemName = Messages.get(add ? "messages.gui.composition.increase"
                        : "messages.gui.composition.decrease", add ? "&a+%change%%" : "&c%change%%",
                "%change%", change);
        ItemStack is = ItemUtils.createItem(m, itemName);
        double finalP = mine.getPercentage(material) + change;
        if (finalP < 0.0) {
            finalP = 0.0;
        }
        final ArrayList<String> lore = new ArrayList<>();
        lore.add(Messages.get("messages.gui.composition.current",
                "Current percentage: %current%%", "%current%", mine.getPercentage(material)));
        lore.add(Messages.get("messages.gui.composition.final",
                "Final percentage: %final%%", "%final%", finalP));
        lore.add(Messages.get("messages.gui.composition.total",
                "Total percentage: %total%%", "%total%", mine.getTotalPercentage()));
        if (mine.isExceedingMaterials()) {
            lore.add(Messages.get("messages.gui.composition.exceeding",
                    "The mine exceeds its materials by %excess%!",
                    "%excess%", mine.getExceedingTotal()));
        } else {
            final double free = mine.getFreePercentage();
            if (free == 0.0) {
                lore.add(Messages.get("messages.gui.composition.full",
                        "The mine is completely full."));
            } else {
                lore.add(Messages.get("messages.gui.composition.free",
                        "Free space: %free%%", "%free%", free));
            }
        }
        lore.add(Messages.get("messages.gui.composition.otherMaterials", "Other materials:"));
        final List<Material> materials = mine.getMaterials();
        if (materials.size() - 1 <= 0) {
            lore.add(Messages.get("messages.gui.composition.noOtherMaterials",
                    "There are no other materials."));
        } else {
            for (final Material other : materials) {
                if (other != material) {
                    lore.add(generateItemPrefix(other, mine));
                }
            }
        }
        ItemUtils.addToLore(is, lore);
        return is;
    }

    private static String generateItemPrefix(final Material material, final Mine mine) {
        return Messages.get("messages.gui.composition.entry",
                "* %material%-%percentage%%", "%material%", material.name(),
                "%percentage%", mine.getPercentage(material));
    }

    public double getPercentage() {
        return this.percentage;
    }

    @Override
    public void update() {
        this.icon = generateCompositionChangeItemStack(this.percentage, this.mine, this.material);
    }

    public Mine getMine() {
        return this.mine;
    }

    public Material getMaterial() {
        return this.material;
    }
}
