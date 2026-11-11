package me.marcdoesntexist.galaxymines.gui.menus;

import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.gui.GUIResourcesUtils;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionButton;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.mines.MineExpansion;
import me.marcdoesntexist.galaxymines.mines.MineProgression;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;









public final class MineTierEditorMenu extends BasicInventoryMenu {
    private final Mine mine;
    private final MineMenu parent;
    private final MenuActionButton levelButton;
    private final MenuActionButton blocksButton;
    private final MenuActionButton expandButton;
    private final MenuActionButton confirmButton;
    private final MenuActionButton backButton;
    private final int infoSlot;
    private final int backSlot;
    private int pendingExpansion;

    public MineTierEditorMenu(final Mine mine, final MineMenu parent) {
        super(title(mine), MenuConfig.rows("tiereditor", 6));
        this.mine = mine;
        this.parent = parent;
        this.infoSlot = MenuConfig.slot("tiereditor", "info", 4);
        this.backSlot = MenuConfig.slot("tiereditor", "back", 0);
        this.levelButton = this.button("setLevel", GUIResourcesUtils.TIER_LEVEL_BUTTON,
                "tier_set_level");
        this.blocksButton = this.button("addBlocks", GUIResourcesUtils.TIER_BLOCKS_BUTTON,
                "tier_add_blocks");
        this.expandButton = this.button("expand", GUIResourcesUtils.TIER_EXPAND_BUTTON,
                "expand_mine");
        this.confirmButton = this.button("confirm", GUIResourcesUtils.TIER_CONFIRM_BUTTON,
                "confirm_expand_mine");
        this.backButton = new MenuActionButton(this.icon("back", GUIResourcesUtils.BACK_BUTTON),
                MenuConfig.action("tiereditor", "back", "back"), this, mine, parent, null);
    }

    private static String title(final Mine mine) {
        return MenuConfig.title("tiereditor", "<dark_blue><bold>Tier: %mine%")
                .replace("%mine%", mine.getAlias());
    }

    private MenuActionButton button(final String key, final ItemStack fallback,
                                    final String action) {
        return new MenuActionButton(this.icon(key, fallback),
                MenuConfig.action("tiereditor", key, action), this, this.mine, null, null);
    }

    private ItemStack icon(final String key, final ItemStack fallback) {
        final ItemStack configured = MenuConfig.item("tiereditor", key);
        if (configured != null) {
            return new ItemStack(configured);
        }
        return fallback == null ? new ItemStack(GUIResourcesUtils.EMPTY_NEUTRAL)
                : new ItemStack(fallback);
    }

    public int getPendingExpansion() {
        return this.pendingExpansion;
    }

    public void setPendingExpansion(final int blocks) {
        this.pendingExpansion = Math.max(0, blocks);
    }



    public int takePendingExpansion() {
        final int blocks = this.pendingExpansion;
        this.pendingExpansion = 0;
        return blocks;
    }

    @Override
    protected void setupItems() {
        final ItemStack fill = MenuConfig.fill("tiereditor");
        this.fill(fill == null ? GUIResourcesUtils.EMPTY_NEUTRAL : fill);
        this.registerInventoryObject(this.levelButton,
                MenuConfig.slot("tiereditor", "setLevel", 10));
        this.registerInventoryObject(this.blocksButton,
                MenuConfig.slot("tiereditor", "addBlocks", 12));
        this.registerInventoryObject(this.expandButton,
                MenuConfig.slot("tiereditor", "expand", 14));
        this.registerInventoryObject(this.confirmButton,
                MenuConfig.slot("tiereditor", "confirm", 16));
        this.registerInventoryObject(this.backButton, this.backSlot);
    }

    @Override
    public void update() {
        final MineProgression progression = this.mine.getProgression();
        this.levelButton.setIcon(ItemUtils.createItem(
                this.icon("setLevel", GUIResourcesUtils.TIER_LEVEL_BUTTON),
                Messages.get("messages.gui.tierEditor.levelName",
                        "<yellow>Set the tier level"),
                List.of(Messages.get("messages.gui.tierEditor.levelLore",
                        "<gray>Current: <aqua>%level%<gray>/<aqua>%max%<gray>. Click to type 1-%max%.",
                        "%level%", Integer.toString(progression.level()),
                        "%max%", Integer.toString(progression.maxLevel())))));
        this.blocksButton.setIcon(ItemUtils.createItem(
                this.icon("addBlocks", GUIResourcesUtils.TIER_BLOCKS_BUTTON),
                Messages.get("messages.gui.tierEditor.blocksName",
                        "<yellow>Add mined blocks"),
                List.of(Messages.get("messages.gui.tierEditor.blocksLore",
                        "<gray>Mined: <aqua>%mined%<gray>, next level in <aqua>%next%<gray>.",
                        "%mined%", Long.toString(progression.minedBlocks()),
                        "%next%", Long.toString(progression.blocksToNextLevel())))));
        this.expandButton.setIcon(ItemUtils.createItem(
                this.icon("expand", GUIResourcesUtils.TIER_EXPAND_BUTTON),
                Messages.get("messages.gui.tierEditor.expandName",
                        "<yellow>Expand the mine"),
                List.of(Messages.get("messages.gui.tierEditor.expandLore",
                        "<gray>Volume <aqua>%volume%<gray>, up to <aqua>%max%<gray> blocks per side.",
                        "%volume%", Long.toString(MineExpansion.volume(this.mine.getBounds())),
                        "%max%", Integer.toString(MineExpansion.MAX_BLOCKS_PER_SIDE)))));
        this.confirmButton.setIcon(ItemUtils.createItem(
                this.icon("confirm", GUIResourcesUtils.TIER_CONFIRM_BUTTON),
                Messages.get("messages.gui.tierEditor.confirmName",
                        "<green>Confirm the expansion"),
                List.of(this.pendingExpansion > 0
                        ? Messages.get("messages.gui.tierEditor.confirmLore",
                        "<gray>Grow by <aqua>%blocks%<gray> blocks per side. Click to confirm.",
                        "%blocks%", Integer.toString(this.pendingExpansion))
                        : Messages.get("messages.gui.tierEditor.confirmEmpty",
                        "<gray>Type an expansion first."))));
        this.refresh(this.levelButton, "setLevel", 10);
        this.refresh(this.blocksButton, "addBlocks", 12);
        this.refresh(this.expandButton, "expand", 14);
        this.refresh(this.confirmButton, "confirm", 16);
        if (!this.hasObjectInSlot(this.infoSlot)) {
            this.setItem(this.infoItem(), this.infoSlot);
        }
    }

    private void refresh(final MenuActionButton button, final String key, final int fallback) {
        this.mainInventory.setItem(MenuConfig.slot("tiereditor", key, fallback),
                button.getIcon());
    }

    private ItemStack infoItem() {
        final MineProgression progression = this.mine.getProgression();
        final String name = Messages.get("messages.gui.tierEditor.infoName",
                "<aqua><bold>Tier and bonuses");
        final List<String> lore = new ArrayList<>();
        lore.add(Messages.get("messages.gui.tierEditor.infoMine",
                "<gray>Mine: <white>%mine%", "%mine%", this.mine.getAlias()));
        lore.add(Messages.get("messages.gui.tierEditor.infoState",
                "<gray>Progression: <aqua>%state%",
                "%state%", progression.enabled() ? "enabled" : "disabled"));
        lore.add(Messages.get("messages.gui.tierEditor.infoLevel",
                "<gray>Level: <aqua>%level%<gray>/<aqua>%max%<gray>, mined <aqua>%mined%",
                "%level%", Integer.toString(progression.level()),
                "%max%", Integer.toString(progression.maxLevel()),
                "%mined%", Long.toString(progression.minedBlocks())));
        lore.add(Messages.get("messages.gui.tierEditor.infoNext",
                "<gray>Next level in <aqua>%next%<gray> blocks (<aqua>%progress%%<gray>)",
                "%next%", Long.toString(progression.blocksToNextLevel()),
                "%progress%", String.format(Locale.ROOT, "%.1f",
                        progression.progressPercent())));
        lore.add(Messages.get("messages.gui.tierEditor.infoBonus",
                "<gray>Bonus: <aqua>%drop%%<gray> drops, <aqua>%sell%%<gray> sell",
                "%drop%", String.format(Locale.ROOT, "%.2f", progression.dropBonus() * 100.0D),
                "%sell%", String.format(Locale.ROOT, "%.2f", progression.sellBonus() * 100.0D)));
        lore.add(Messages.get("messages.gui.tierEditor.infoExpansion",
                "<gray>Expansion: <aqua>%state%",
                "%state%", this.mine.supportsExpansion() ? "supported" : "unsupported"));
        return ItemUtils.createItem(this.icon("info", GUIResourcesUtils.TIER_LEVEL_BUTTON),
                name, lore);
    }
}
