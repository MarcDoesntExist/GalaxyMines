package me.marcdoesntexist.galaxymines.gui.menus;

import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicClickableInventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicInventoryContainer;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.gui.GUIResourcesUtils;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionButton;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionContext;
import me.marcdoesntexist.galaxymines.gui.objects.MaterialCompositionChanger;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MineCompositionEditorMenu extends BasicInventoryMenu {
    private final Material material;
    private final Mine mine;
    private final MineMenu mineMenu;
    private final BasicClickableInventoryObject backButton;
    private final BasicInventoryContainer options;
    private final BasicClickableInventoryObject removeButton;
    private final List<Double> percentages;
    private final BasicInventoryContainer changers;
    private final int optionsStart;
    private final int statsSlot;
    private final int backSlot;

    public MineCompositionEditorMenu(final Mine owner, final Material material,
                                     final MineMenu menu) {
        super(buildTitle(material), MenuConfig.rows("composition", 6));
        this.percentages = MenuConfig.percentageSteps("composition",
                new double[]{50.0, 25.0, 10.0, 5.0, 2.0, 1.0, 0.5, 0.1});
        this.mine = owner;
        this.mineMenu = menu;
        this.material = material;
        this.backSlot = MenuConfig.slot("composition", "back", 45);
        this.statsSlot = MenuConfig.slot("composition", "stats", 43);
        this.backButton = new MenuActionButton(GUIResourcesUtils.BACK_BUTTON,
                MenuConfig.action("composition", "back", "back"), this, this.mine,
                this.mineMenu, MenuConfig.actionArgument("composition", "back", null));
        this.removeButton = new MenuActionButton(GUIResourcesUtils.REMOVE_MATERIAL_BUTTON,
                MenuConfig.action("composition", "remove", "remove_material"), this, this.mine,
                new MenuActionContext.Removal(this.material, this.mineMenu),
                MenuConfig.actionArgument("composition", "remove", null));
        final int[] changerRange = MenuConfig.range("composition", "changers", 9, 25);
        final int[] optionRange = MenuConfig.range("composition", "options", 36, 43);
        this.changers = new BasicInventoryContainer(this, changerRange[0], changerRange[1]);
        this.options = new BasicInventoryContainer(this, optionRange[0], optionRange[1]);
        this.optionsStart = optionRange[0];
    }

    private static String buildTitle(final Material material) {
        return MenuConfig.title("composition", "&2&l%material%")
                .replace("%material%", material.name());
    }

    @Override
    protected void setupItems() {
        final ItemStack fill = MenuConfig.fill("composition");
        if (fill != null) {
            this.fill(fill);
        }
        this.registerInventoryObject(this.changers,
                MenuConfig.range("composition", "changers", 9, 25)[0]);
        this.registerInventoryObject(this.options,
                MenuConfig.range("composition", "options", 36, 43)[0]);
        this.registerInventoryObject(this.backButton, this.backSlot);
        this.options.clearAndFill(GUIResourcesUtils.EMPTY_NEUTRAL);
        this.options.addObject(this.removeButton);
        int slot = 0;
        for (final double percentage : this.percentages) {
            this.changers.setObject(slot,
                    new MaterialCompositionChanger(this.material, this, this.mine, percentage));
            this.changers.setObject(slot + 8,
                    new MaterialCompositionChanger(this.material, this, this.mine, -percentage));
            ++slot;
        }
    }

    @Override
    public void update() {
        this.changers.update();
        this.options.setItem(this.statsSlot - this.optionsStart, this.generateItemStat());
    }

    private ItemStack generateItemStat() {
        final ItemStack is = ItemUtils.createItem(new ItemStack(this.material),
                MenuConfig.title("composition", "&2&l%material%")
                        .replace("%material%", this.material.name()));
        final List<String> lore = new ArrayList<>();
        lore.add(Messages.get("messages.gui.composition.total",
                "Total percentage: %total%%", "%total%", this.mine.getTotalPercentage()));
        if (this.mine.isExceedingMaterials()) {
            lore.add(Messages.get("messages.gui.composition.exceeding",
                    "The mine exceeds its materials by %excess%!",
                    "%excess%", this.mine.getExceedingTotal()));
        } else {
            final double free = this.mine.getFreePercentage();
            if (free == 0.0) {
                lore.add(Messages.get("messages.gui.composition.full",
                        "The mine is completely full."));
            } else {
                lore.add(Messages.get("messages.gui.composition.free",
                        "Free space: %free%%", "%free%", free));
            }
        }
        lore.add(Messages.get("messages.gui.composition.otherMaterials", "Other materials:"));
        final List<Material> materials = this.mine.getMaterials();
        if (materials.size() - 1 <= 0) {
            lore.add(Messages.get("messages.gui.composition.noOtherMaterials",
                    "There are no other materials."));
        } else {
            for (final Material other : materials) {
                if (other != this.material) {
                    lore.add(this.generateItemPrefix(other));
                }
            }
        }
        ItemUtils.addToLore(is, lore);
        return is;
    }

    private String generateItemPrefix() {
        return generateItemPrefix(this.material);
    }

    private String generateItemPrefix(final Material item) {
        return Messages.get("messages.gui.composition.entry",
                "* %material%-%percentage%%", "%material%", item.name(),
                "%percentage%", this.mine.getPercentage(item));
    }
}
