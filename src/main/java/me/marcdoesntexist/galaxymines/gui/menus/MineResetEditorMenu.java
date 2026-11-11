package me.marcdoesntexist.galaxymines.gui.menus;

import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicInventoryContainer;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.gui.GUIResourcesUtils;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionButton;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionContext;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;










public final class MineResetEditorMenu extends BasicInventoryMenu {
    private final Mine mine;
    private final MineMenu parent;
    private final BasicInventoryContainer preCommands;
    private final BasicInventoryContainer postCommands;
    private final MenuActionButton defaultButton;
    private final MenuActionButton minButton;
    private final MenuActionButton maxButton;
    private final MenuActionButton intervalButton;
    private final MenuActionButton addPreButton;
    private final MenuActionButton addPostButton;
    private final MenuActionButton backButton;
    private final int preStart;
    private final int postStart;
    private final int infoSlot;
    private final int backSlot;

    public MineResetEditorMenu(final Mine mine, final MineMenu parent) {
        super(title(mine), MenuConfig.rows("reseteditor", 6));
        this.mine = mine;
        this.parent = parent;
        final int[] preRange = MenuConfig.range("reseteditor", "preList", 19, 25);
        final int[] postRange = MenuConfig.range("reseteditor", "postList", 28, 34);
        this.preStart = preRange[0];
        this.postStart = postRange[0];
        this.preCommands = new BasicInventoryContainer(this, preRange[0], preRange[1]);
        this.postCommands = new BasicInventoryContainer(this, postRange[0], postRange[1]);
        this.infoSlot = MenuConfig.slot("reseteditor", "info", 40);
        this.backSlot = MenuConfig.slot("reseteditor", "back", 45);
        this.defaultButton = this.button("default", GUIResourcesUtils.RESET_THRESHOLD_BUTTON,
                "set_reset_default", null);
        this.minButton = this.button("min", GUIResourcesUtils.RESET_MIN_BUTTON,
                "set_reset_min", null);
        this.maxButton = this.button("max", GUIResourcesUtils.RESET_MAX_BUTTON,
                "set_reset_max", null);
        this.intervalButton = this.button("interval", GUIResourcesUtils.RESET_INTERVAL_BUTTON,
                "set_reset_interval", null);
        this.addPreButton = this.button("preAdd", GUIResourcesUtils.ADD_RESET_COMMAND_BUTTON,
                "add_reset_command", "pre");
        this.addPostButton = this.button("postAdd", GUIResourcesUtils.ADD_RESET_COMMAND_BUTTON,
                "add_reset_command", "post");
        this.backButton = new MenuActionButton(this.icon("back", GUIResourcesUtils.BACK_BUTTON),
                MenuConfig.action("reseteditor", "back", "back"), this, mine, parent, null);
    }

    private static String title(final Mine mine) {
        return MenuConfig.title("reseteditor", "<dark_blue><bold>Reset: %mine%")
                .replace("%mine%", mine.getAlias());
    }

    private static String formatNumber(final double value) {
        String formatted = String.format(Locale.ROOT, "%.2f", value);
        while (formatted.contains(".") && (formatted.endsWith("0") || formatted.endsWith("."))) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    private MenuActionButton button(final String key, final ItemStack fallback,
                                    final String action, final String fallbackArgument) {
        return new MenuActionButton(this.icon(key, fallback),
                MenuConfig.action("reseteditor", key, action), this, this.mine, null,
                MenuConfig.actionArgument("reseteditor", key, fallbackArgument));
    }

    private ItemStack icon(final String key, final ItemStack fallback) {
        final ItemStack configured = MenuConfig.item("reseteditor", key);
        if (configured != null) {
            return new ItemStack(configured);
        }
        return fallback == null ? new ItemStack(GUIResourcesUtils.EMPTY_NEUTRAL)
                : new ItemStack(fallback);
    }

    @Override
    protected void setupItems() {
        final ItemStack fill = MenuConfig.fill("reseteditor");
        this.fill(fill == null ? GUIResourcesUtils.EMPTY_NEUTRAL : fill);
        this.registerInventoryObject(this.preCommands, this.preStart);
        this.registerInventoryObject(this.postCommands, this.postStart);
        this.registerInventoryObject(this.defaultButton,
                MenuConfig.slot("reseteditor", "default", 10));
        this.registerInventoryObject(this.minButton,
                MenuConfig.slot("reseteditor", "min", 11));
        this.registerInventoryObject(this.maxButton,
                MenuConfig.slot("reseteditor", "max", 12));
        this.registerInventoryObject(this.intervalButton,
                MenuConfig.slot("reseteditor", "interval", 13));
        this.registerInventoryObject(this.addPreButton,
                MenuConfig.slot("reseteditor", "preAdd", 18));
        this.registerInventoryObject(this.addPostButton,
                MenuConfig.slot("reseteditor", "postAdd", 27));
        this.registerInventoryObject(this.backButton, this.backSlot);
    }

    @Override
    public void update() {
        this.updateButtons();
        this.mainInventory.setItem(MenuConfig.slot("reseteditor", "default", 10),
                this.defaultButton.getIcon());
        this.mainInventory.setItem(MenuConfig.slot("reseteditor", "min", 11),
                this.minButton.getIcon());
        this.mainInventory.setItem(MenuConfig.slot("reseteditor", "max", 12),
                this.maxButton.getIcon());
        this.mainInventory.setItem(MenuConfig.slot("reseteditor", "interval", 13),
                this.intervalButton.getIcon());
        this.mainInventory.setItem(MenuConfig.slot("reseteditor", "preAdd", 18),
                this.addPreButton.getIcon());
        this.mainInventory.setItem(MenuConfig.slot("reseteditor", "postAdd", 27),
                this.addPostButton.getIcon());
        this.preCommands.clearAndFill(GUIResourcesUtils.EMPTY_NEUTRAL);
        this.postCommands.clearAndFill(GUIResourcesUtils.EMPTY_NEUTRAL);
        this.fillCommands(this.preCommands, true, this.mine.getPreResetCommands());
        this.fillCommands(this.postCommands, false, this.mine.getPostResetCommands());
        if (!this.hasObjectInSlot(this.infoSlot)) {
            this.setItem(this.infoItem(), this.infoSlot);
        }
    }

    private void updateButtons() {
        this.defaultButton.setIcon(this.thresholdIcon("default",
                GUIResourcesUtils.RESET_THRESHOLD_BUTTON, "defaultName",
                "<yellow>Default reset threshold", "defaultValue",
                "<gray>Click to type a value. Current: <aqua>%value%%<gray>.",
                this.mine.getResetOnPercent()));
        this.minButton.setIcon(this.thresholdIcon("min", GUIResourcesUtils.RESET_MIN_BUTTON,
                "minName", "<yellow>Minimum reset threshold", "minValue",
                "<gray>Click to type a value. Current: <aqua>%value%%<gray>.",
                this.mine.getResetPercentMin()));
        this.maxButton.setIcon(this.thresholdIcon("max", GUIResourcesUtils.RESET_MAX_BUTTON,
                "maxName", "<yellow>Maximum reset threshold", "maxValue",
                "<gray>Click to type a value. Current: <aqua>%value%%<gray>.",
                this.mine.getResetPercentMax()));
        this.intervalButton.setIcon(this.thresholdIcon("interval",
                GUIResourcesUtils.RESET_INTERVAL_BUTTON, "intervalName",
                "<yellow>Threshold check interval", "intervalValue",
                "<gray>Click to type a value. Current: <aqua>%value%<gray> tick(s).",
                this.mine.getResetPercentCheckIntervalTicks()));
        this.addPreButton.setIcon(this.addIcon("preAdd",
                GUIResourcesUtils.ADD_RESET_COMMAND_BUTTON, "addPreName",
                "<green>Add a command before the reset", "addPreLore",
                "<gray>Click and type the console command in chat."));
        this.addPostButton.setIcon(this.addIcon("postAdd",
                GUIResourcesUtils.ADD_RESET_COMMAND_BUTTON, "addPostName",
                "<green>Add a command after the reset", "addPostLore",
                "<gray>Click and type the console command in chat."));
    }

    private ItemStack thresholdIcon(final String key, final ItemStack fallback,
                                    final String nameKey, final String nameFallback,
                                    final String valueKey, final String valueFallback,
                                    final double value) {
        final String formatted = formatNumber(value);
        final String name = Messages.get("messages.gui.resetEditor." + nameKey, nameFallback);
        final String lore = Messages.get("messages.gui.resetEditor." + valueKey, valueFallback,
                "%value%", formatted);
        return ItemUtils.createItem(this.icon(key, fallback), name, List.of(lore));
    }

    private ItemStack addIcon(final String key, final ItemStack fallback, final String nameKey,
                              final String nameFallback, final String loreKey,
                              final String loreFallback) {
        final String name = Messages.get("messages.gui.resetEditor." + nameKey, nameFallback);
        final String lore = Messages.get("messages.gui.resetEditor." + loreKey, loreFallback);
        return ItemUtils.createItem(this.icon(key, fallback), name, List.of(lore));
    }

    private void fillCommands(final BasicInventoryContainer container, final boolean pre,
                              final List<String> commands) {
        int index = 0;
        for (final String command : commands) {
            if (index >= container.getSize()) {
                break;
            }
            container.setObject(index, this.commandEntry(pre, index, command));
            ++index;
        }
    }

    private MenuActionButton commandEntry(final boolean pre, final int index,
                                          final String command) {
        final String name = Messages.get("messages.gui.resetEditor.entryName",
                "<yellow>#%index% <white>%command%",
                "%index%", Integer.toString(index + 1), "%command%", command);
        final List<String> lore = List.of(
                Messages.get("messages.gui.resetEditor.entryPhase",
                        "<gray>Phase: <aqua>%phase%",
                        "%phase%", pre ? "pre" : "post"),
                Messages.get("messages.gui.resetEditor.entryRemove",
                        "<red>Click to remove this command."));
        final ItemStack item = ItemUtils.createItem(
                this.icon("command", GUIResourcesUtils.RESET_COMMAND_ICON), name, lore);
        return new MenuActionButton(item,
                MenuConfig.action("reseteditor", "command", "remove_reset_command"),
                this, this.mine, new MenuActionContext.CommandRemoval(pre, index, this),
                MenuConfig.actionArgument("reseteditor", "command", null));
    }

    private ItemStack infoItem() {
        final String name = Messages.get("messages.gui.resetEditor.infoName",
                "<aqua><bold>Reset settings");
        final List<String> lore = new ArrayList<>();
        lore.add(Messages.get("messages.gui.resetEditor.infoMine", "<gray>Mine: <white>%mine%",
                "%mine%", this.mine.getAlias()));
        lore.add(Messages.get("messages.gui.resetEditor.infoTarget",
                "<gray>Current target: <aqua>%value%%",
                "%value%", formatNumber(this.mine.getResetOnPercent())));
        lore.add(Messages.get("messages.gui.resetEditor.infoRange",
                "<gray>Range: <aqua>%min%%<gray>-<aqua>%max%%",
                "%min%", formatNumber(this.mine.getResetPercentMin()),
                "%max%", formatNumber(this.mine.getResetPercentMax())));
        lore.add(Messages.get("messages.gui.resetEditor.infoInterval",
                "<gray>Check interval: <aqua>%value%<gray> tick(s)",
                "%value%", Integer.toString(this.mine.getResetPercentCheckIntervalTicks())));
        lore.add(Messages.get("messages.gui.resetEditor.infoCommands",
                "<gray>Commands: <aqua>%pre%<gray> pre, <aqua>%post%<gray> post",
                "%pre%", Integer.toString(this.mine.getPreResetCommands().size()),
                "%post%", Integer.toString(this.mine.getPostResetCommands().size())));
        return ItemUtils.createItem(this.icon("info", GUIResourcesUtils.RESET_THRESHOLD_BUTTON),
                name, lore);
    }
}
