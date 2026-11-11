package me.marcdoesntexist.galaxymines.gui.menus;

import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.gui.GUIResourcesUtils;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionButton;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.mines.MineWaveDirection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;










public final class MineWaveEditorMenu extends BasicInventoryMenu {
    private final Mine mine;
    private final MineMenu parent;
    private final MenuActionButton directionButton;
    private final MenuActionButton decreaseButton;
    private final MenuActionButton setButton;
    private final MenuActionButton increaseButton;
    private final MenuActionButton backButton;
    private final int infoSlot;
    private final int backSlot;
    private final int arrowSlot;
    private final int previewStart;
    private final int previewSize;

    public MineWaveEditorMenu(final Mine mine, final MineMenu parent) {
        super(title(mine), MenuConfig.rows("waveeditor", 6));
        this.mine = mine;
        this.parent = parent;
        final int[] previewRange = MenuConfig.range("waveeditor", "preview", 28, 34);
        this.previewStart = previewRange[0];
        this.previewSize = previewRange[1] - previewRange[0] + 1;
        this.infoSlot = MenuConfig.slot("waveeditor", "info", 4);
        this.backSlot = MenuConfig.slot("waveeditor", "back", 0);
        this.arrowSlot = MenuConfig.slot("waveeditor", "arrow", 27);
        this.directionButton = this.button("direction",
                GUIResourcesUtils.WAVE_DIRECTION_BUTTON, "toggle_wave_direction", null);
        this.decreaseButton = this.button("decrease", GUIResourcesUtils.WAVE_MINUS_BUTTON,
                "adjust_wave_layers", "-1");
        this.setButton = this.button("set", GUIResourcesUtils.WAVE_SET_BUTTON,
                "set_wave_layers", null);
        this.increaseButton = this.button("increase", GUIResourcesUtils.WAVE_PLUS_BUTTON,
                "adjust_wave_layers", "+1");
        this.backButton = new MenuActionButton(this.icon("back", GUIResourcesUtils.BACK_BUTTON),
                MenuConfig.action("waveeditor", "back", "back"), this, mine, parent, null);
    }

    private static String title(final Mine mine) {
        return MenuConfig.title("waveeditor", "<dark_blue><bold>Wave: %mine%")
                .replace("%mine%", mine.getAlias());
    }




    public static int effectiveLayers(final int mineLayers) {
        if (mineLayers > 0) {
            return mineLayers;
        }
        return Math.max(0, PluginSettings.get().reset.waveLayersPerTick);
    }

    private MenuActionButton button(final String key, final ItemStack fallback,
                                    final String action, final String fallbackArgument) {
        return new MenuActionButton(this.icon(key, fallback),
                MenuConfig.action("waveeditor", key, action), this, this.mine, null,
                MenuConfig.actionArgument("waveeditor", key, fallbackArgument));
    }

    private ItemStack icon(final String key, final ItemStack fallback) {
        final ItemStack configured = MenuConfig.item("waveeditor", key);
        if (configured != null) {
            return new ItemStack(configured);
        }
        return fallback == null ? new ItemStack(GUIResourcesUtils.EMPTY_NEUTRAL)
                : new ItemStack(fallback);
    }

    @Override
    protected void setupItems() {
        final ItemStack fill = MenuConfig.fill("waveeditor");
        this.fill(fill == null ? GUIResourcesUtils.EMPTY_NEUTRAL : fill);
        this.registerInventoryObject(this.directionButton,
                MenuConfig.slot("waveeditor", "direction", 10));
        this.registerInventoryObject(this.decreaseButton,
                MenuConfig.slot("waveeditor", "decrease", 12));
        this.registerInventoryObject(this.setButton,
                MenuConfig.slot("waveeditor", "set", 13));
        this.registerInventoryObject(this.increaseButton,
                MenuConfig.slot("waveeditor", "increase", 14));
        this.registerInventoryObject(this.backButton, this.backSlot);
    }

    @Override
    public void update() {
        this.updateButtons();
        this.refresh(this.directionButton, "direction", 10);
        this.refresh(this.decreaseButton, "decrease", 12);
        this.refresh(this.setButton, "set", 13);
        this.refresh(this.increaseButton, "increase", 14);
        this.mainInventory.setItem(MenuConfig.slot("waveeditor", "arrow", 27),
                this.arrowItem());
        this.updatePreview();
        this.setItem(this.infoItem(), this.infoSlot);
    }

    private void refresh(final MenuActionButton button, final String key, final int fallback) {
        this.mainInventory.setItem(MenuConfig.slot("waveeditor", key, fallback),
                button.getIcon());
    }

    private void updateButtons() {
        final boolean bottomUp = this.mine.getWaveDirection() == MineWaveDirection.BOTTOM_UP;
        this.directionButton.setIcon(ItemUtils.createItem(
                this.icon("direction", GUIResourcesUtils.WAVE_DIRECTION_BUTTON),
                Messages.get("messages.gui.waveEditor.directionName",
                        "<yellow>Direction: <aqua>%state%", "%state%", bottomUp
                                ? "bottom-up" : "top-down"),
                List.of(Messages.get("messages.gui.waveEditor.directionLore",
                        "<gray>Click to switch between top-down and bottom-up."))));
        this.decreaseButton.setIcon(ItemUtils.createItem(
                this.icon("decrease", GUIResourcesUtils.WAVE_MINUS_BUTTON),
                Messages.get("messages.gui.waveEditor.decreaseName",
                        "<red>One layer fewer"),
                List.of(Messages.get("messages.gui.waveEditor.decreaseLore",
                        "<gray>Click to subtract one layer per tick."))));
        this.setButton.setIcon(ItemUtils.createItem(
                this.icon("set", GUIResourcesUtils.WAVE_SET_BUTTON),
                Messages.get("messages.gui.waveEditor.setName",
                        "<yellow>Type an exact value"),
                List.of(Messages.get("messages.gui.waveEditor.setLore",
                        "<gray>Click and type layers per tick (0 = global)."))));
        this.increaseButton.setIcon(ItemUtils.createItem(
                this.icon("increase", GUIResourcesUtils.WAVE_PLUS_BUTTON),
                Messages.get("messages.gui.waveEditor.increaseName",
                        "<green>One layer more"),
                List.of(Messages.get("messages.gui.waveEditor.increaseLore",
                        "<gray>Click to add one layer per tick."))));
    }

    private ItemStack arrowItem() {
        final boolean bottomUp = this.mine.getWaveDirection() == MineWaveDirection.BOTTOM_UP;
        return ItemUtils.createItem(
                this.icon("arrow", GUIResourcesUtils.WAVE_DIRECTION_BUTTON),
                Messages.get("messages.gui.waveEditor.arrowName",
                        "<yellow>Preview: <aqua>%direction%", "%direction%", bottomUp
                                ? "bottom-up" : "top-down"),
                List.of(Messages.get("messages.gui.waveEditor.arrowLore",
                        "<gray>The marked slots are the layers filled by one tick.")));
    }

    private void updatePreview() {
        final int layers = effectiveLayers(this.mine.getWaveLayersPerTick());
        for (int index = 0; index < this.previewSize; ++index) {
            final ItemStack pane = index < layers
                    ? GUIResourcesUtils.WAVE_LAYER_ON : GUIResourcesUtils.WAVE_LAYER_OFF;
            this.mainInventory.setItem(this.previewStart + index,
                    pane == null ? new ItemStack(GUIResourcesUtils.EMPTY_NEUTRAL)
                            : new ItemStack(pane));
        }
    }

    private ItemStack infoItem() {
        final int configured = this.mine.getWaveLayersPerTick();
        final int effective = effectiveLayers(configured);
        final String name = Messages.get("messages.gui.waveEditor.infoName",
                "<aqua><bold>Wave settings");
        final List<String> lore = new ArrayList<>();
        lore.add(Messages.get("messages.gui.waveEditor.infoMine",
                "<gray>Mine: <white>%mine%", "%mine%", this.mine.getAlias()));
        lore.add(Messages.get("messages.gui.waveEditor.infoDirection",
                "<gray>Direction: <aqua>%direction%",
                "%direction%", this.mine.getWaveDirection().name()));
        lore.add(Messages.get("messages.gui.waveEditor.infoMineLayers",
                "<gray>Mine layers: <aqua>%value%",
                "%value%", configured == 0 ? "global" : Integer.toString(configured)));
        lore.add(Messages.get("messages.gui.waveEditor.infoEffective",
                "<gray>Effective layers: <aqua>%value%<gray> per tick",
                "%value%", Integer.toString(effective)));
        return ItemUtils.createItem(
                this.icon("info", GUIResourcesUtils.WAVE_SET_BUTTON), name, lore);
    }
}
