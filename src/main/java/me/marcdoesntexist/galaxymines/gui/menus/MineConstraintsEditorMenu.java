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
import me.marcdoesntexist.galaxymines.mines.MineConstraints;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public final class MineConstraintsEditorMenu extends BasicInventoryMenu {
    private final Mine mine;
    private final MineMenu parent;
    private final BasicInventoryContainer breakAllow;
    private final BasicInventoryContainer breakDeny;
    private final BasicInventoryContainer placeAllow;
    private final BasicInventoryContainer placeDeny;
    private final MenuActionButton breakAllowButton;
    private final MenuActionButton breakDenyButton;
    private final MenuActionButton breakClearButton;
    private final MenuActionButton placeToggleButton;
    private final MenuActionButton placeAllowButton;
    private final MenuActionButton placeDenyButton;
    private final MenuActionButton placeClearButton;
    private final MenuActionButton waveEditorButton;
    private final MenuActionButton copyButton;
    private final MenuActionButton backButton;
    private final int breakAllowStart;
    private final int breakDenyStart;
    private final int placeAllowStart;
    private final int placeDenyStart;
    private final int infoSlot;
    private final int backSlot;

    public MineConstraintsEditorMenu(final Mine mine, final MineMenu parent) {
        super(title(mine), MenuConfig.rows("constraintseditor", 6));
        this.mine = mine;
        this.parent = parent;
        final int[] breakAllowRange =
                MenuConfig.range("constraintseditor", "breakAllowList", 18, 26);
        final int[] breakDenyRange =
                MenuConfig.range("constraintseditor", "breakDenyList", 27, 35);
        final int[] placeAllowRange =
                MenuConfig.range("constraintseditor", "placeAllowList", 36, 44);
        final int[] placeDenyRange =
                MenuConfig.range("constraintseditor", "placeDenyList", 45, 53);
        this.breakAllowStart = breakAllowRange[0];
        this.breakDenyStart = breakDenyRange[0];
        this.placeAllowStart = placeAllowRange[0];
        this.placeDenyStart = placeDenyRange[0];
        this.breakAllow = new BasicInventoryContainer(this, breakAllowRange[0],
                breakAllowRange[1]);
        this.breakDeny = new BasicInventoryContainer(this, breakDenyRange[0],
                breakDenyRange[1]);
        this.placeAllow = new BasicInventoryContainer(this, placeAllowRange[0],
                placeAllowRange[1]);
        this.placeDeny = new BasicInventoryContainer(this, placeDenyRange[0],
                placeDenyRange[1]);
        this.infoSlot = MenuConfig.slot("constraintseditor", "info", 8);
        this.backSlot = MenuConfig.slot("constraintseditor", "back", 0);
        this.breakAllowButton = this.button("breakAllow", GUIResourcesUtils.CONSTRAINT_ALLOW_BUTTON,
                "constraint_allow", "break");
        this.breakDenyButton = this.button("breakDeny", GUIResourcesUtils.CONSTRAINT_DENY_BUTTON,
                "constraint_deny", "break");
        this.breakClearButton = this.button("breakClear",
                GUIResourcesUtils.CONSTRAINT_CLEAR_BUTTON, "constraint_clear", "break");
        this.placeToggleButton = this.button("placeToggle",
                GUIResourcesUtils.CONSTRAINT_PLACE_BUTTON, "toggle_place_constraints", null);
        this.placeAllowButton = this.button("placeAllow",
                GUIResourcesUtils.CONSTRAINT_ALLOW_BUTTON, "constraint_allow", "place");
        this.placeDenyButton = this.button("placeDeny",
                GUIResourcesUtils.CONSTRAINT_DENY_BUTTON, "constraint_deny", "place");
        this.placeClearButton = this.button("placeClear",
                GUIResourcesUtils.CONSTRAINT_CLEAR_BUTTON, "constraint_clear", "place");
        this.waveEditorButton = this.button("waveEditor",
                GUIResourcesUtils.WAVE_EDITOR_BUTTON, "open_wave_editor", null);
        this.copyButton = this.button("copy", GUIResourcesUtils.CONSTRAINT_ENTRY_ICON,
                "copy_constraints", null);
        this.backButton = new MenuActionButton(this.icon("back", GUIResourcesUtils.BACK_BUTTON),
                MenuConfig.action("constraintseditor", "back", "back"), this, mine, parent,
                null);
    }

    private static String title(final Mine mine) {
        return MenuConfig.title("constraintseditor",
                        "<dark_blue><bold>Rules: %mine%")
                .replace("%mine%", mine.getAlias());
    }

    private MenuActionButton button(final String key, final ItemStack fallback,
                                    final String action, final String fallbackArgument) {
        return new MenuActionButton(this.icon(key, fallback),
                MenuConfig.action("constraintseditor", key, action), this, this.mine, null,
                MenuConfig.actionArgument("constraintseditor", key, fallbackArgument));
    }

    private ItemStack icon(final String key, final ItemStack fallback) {
        final ItemStack configured = MenuConfig.item("constraintseditor", key);
        if (configured != null) {
            return new ItemStack(configured);
        }
        return fallback == null ? new ItemStack(GUIResourcesUtils.EMPTY_NEUTRAL)
                : new ItemStack(fallback);
    }

    @Override
    protected void setupItems() {
        final ItemStack fill = MenuConfig.fill("constraintseditor");
        this.fill(fill == null ? GUIResourcesUtils.EMPTY_NEUTRAL : fill);
        this.registerInventoryObject(this.breakAllow, this.breakAllowStart);
        this.registerInventoryObject(this.breakDeny, this.breakDenyStart);
        this.registerInventoryObject(this.placeAllow, this.placeAllowStart);
        this.registerInventoryObject(this.placeDeny, this.placeDenyStart);
        this.registerInventoryObject(this.breakAllowButton,
                MenuConfig.slot("constraintseditor", "breakAllow", 9));
        this.registerInventoryObject(this.breakDenyButton,
                MenuConfig.slot("constraintseditor", "breakDeny", 10));
        this.registerInventoryObject(this.breakClearButton,
                MenuConfig.slot("constraintseditor", "breakClear", 11));
        this.registerInventoryObject(this.placeToggleButton,
                MenuConfig.slot("constraintseditor", "placeToggle", 12));
        this.registerInventoryObject(this.placeAllowButton,
                MenuConfig.slot("constraintseditor", "placeAllow", 13));
        this.registerInventoryObject(this.placeDenyButton,
                MenuConfig.slot("constraintseditor", "placeDeny", 14));
        this.registerInventoryObject(this.placeClearButton,
                MenuConfig.slot("constraintseditor", "placeClear", 15));
        this.registerInventoryObject(this.waveEditorButton,
                MenuConfig.slot("constraintseditor", "waveEditor", 16));
        this.registerInventoryObject(this.copyButton,
                MenuConfig.slot("constraintseditor", "copy", 17));
        this.registerInventoryObject(this.backButton, this.backSlot);
    }

    @Override
    public void update() {
        this.updateButtons();
        this.refresh(this.breakAllowButton, "breakAllow", 9);
        this.refresh(this.breakDenyButton, "breakDeny", 10);
        this.refresh(this.breakClearButton, "breakClear", 11);
        this.refresh(this.placeToggleButton, "placeToggle", 12);
        this.refresh(this.placeAllowButton, "placeAllow", 13);
        this.refresh(this.placeDenyButton, "placeDeny", 14);
        this.refresh(this.placeClearButton, "placeClear", 15);
        this.refresh(this.waveEditorButton, "waveEditor", 16);
        this.refresh(this.copyButton, "copy", 17);
        final MineConstraints constraints = this.mine.getBlockConstraints();
        this.fillEntries(this.breakAllow, true, true, constraints.breakAllowed());
        this.fillEntries(this.breakDeny, true, false, constraints.breakDenied());
        this.fillEntries(this.placeAllow, false, true, constraints.placeAllowed());
        this.fillEntries(this.placeDeny, false, false, constraints.placeDenied());
        if (!this.hasObjectInSlot(this.infoSlot)) {
            this.setItem(this.infoItem(), this.infoSlot);
        }
    }

    private void refresh(final MenuActionButton button, final String key, final int fallback) {
        this.mainInventory.setItem(MenuConfig.slot("constraintseditor", key, fallback),
                button.getIcon());
    }

    private void updateButtons() {
        final MineConstraints constraints = this.mine.getBlockConstraints();
        this.breakAllowButton.setIcon(this.listButton("breakAllow",
                GUIResourcesUtils.CONSTRAINT_ALLOW_BUTTON, "breakAllowName",
                "<green>Allow breaking a material", "breakAllowLore",
                "<gray>Click and type a block name to allow it.",
                constraints.breakAllowed().size()));
        this.breakDenyButton.setIcon(this.listButton("breakDeny",
                GUIResourcesUtils.CONSTRAINT_DENY_BUTTON, "breakDenyName",
                "<red>Deny breaking a material", "breakDenyLore",
                "<gray>Click and type a block name to deny it.",
                constraints.breakDenied().size()));
        this.breakClearButton.setIcon(this.clearButton("breakClear",
                GUIResourcesUtils.CONSTRAINT_CLEAR_BUTTON, "breakClearName",
                "<red>Clear the break lists", "breakClearLore",
                "<gray>Click to remove every break constraint."));
        this.placeToggleButton.setIcon(this.stateButton("placeToggle",
                GUIResourcesUtils.CONSTRAINT_PLACE_BUTTON, "placeToggleName",
                "<yellow>Placement checks: %state%", "placeToggleLore",
                "<gray>Click to enable or disable placement checks.",
                constraints.isPlaceEnabled() ? "on" : "off"));
        this.placeAllowButton.setIcon(this.listButton("placeAllow",
                GUIResourcesUtils.CONSTRAINT_ALLOW_BUTTON, "placeAllowName",
                "<green>Allow placing a material", "placeAllowLore",
                "<gray>Click and type a block name to allow it.",
                constraints.placeAllowed().size()));
        this.placeDenyButton.setIcon(this.listButton("placeDeny",
                GUIResourcesUtils.CONSTRAINT_DENY_BUTTON, "placeDenyName",
                "<red>Deny placing a material", "placeDenyLore",
                "<gray>Click and type a block name to deny it.",
                constraints.placeDenied().size()));
        this.placeClearButton.setIcon(this.clearButton("placeClear",
                GUIResourcesUtils.CONSTRAINT_CLEAR_BUTTON, "placeClearName",
                "<red>Clear the place lists", "placeClearLore",
                "<gray>Click to remove every placement constraint."));
        this.copyButton.setIcon(ItemUtils.createItem(
                this.icon("copy", GUIResourcesUtils.CONSTRAINT_ENTRY_ICON),
                Messages.get("messages.gui.constraintsEditor.copyName",
                        "<yellow>Copy the rules from another mine"),
                List.of(Messages.get("messages.gui.constraintsEditor.copyLore",
                        "<gray>Click and type the name of the mine to copy from."))));
    }

    private ItemStack listButton(final String key, final ItemStack fallback, final String nameKey,
                                 final String nameFallback, final String loreKey,
                                 final String loreFallback, final int size) {
        final String name = Messages.get("messages.gui.constraintsEditor." + nameKey,
                nameFallback);
        final String lore = Messages.get("messages.gui.constraintsEditor." + loreKey,
                loreFallback, "%count%", Integer.toString(size));
        return ItemUtils.createItem(this.icon(key, fallback), name, List.of(lore));
    }

    private ItemStack clearButton(final String key, final ItemStack fallback,
                                  final String nameKey, final String nameFallback,
                                  final String loreKey, final String loreFallback) {
        final String name = Messages.get("messages.gui.constraintsEditor." + nameKey,
                nameFallback);
        final String lore = Messages.get("messages.gui.constraintsEditor." + loreKey,
                loreFallback);
        return ItemUtils.createItem(this.icon(key, fallback), name, List.of(lore));
    }

    private ItemStack stateButton(final String key, final ItemStack fallback,
                                  final String nameKey, final String nameFallback,
                                  final String loreKey, final String loreFallback,
                                  final String state) {
        final String name = Messages.get("messages.gui.constraintsEditor." + nameKey,
                nameFallback, "%state%", state);
        final String lore = Messages.get("messages.gui.constraintsEditor." + loreKey,
                loreFallback);
        return ItemUtils.createItem(this.icon(key, fallback), name, List.of(lore));
    }

    private void fillEntries(final BasicInventoryContainer container, final boolean breakSection,
                             final boolean allow, final Set<Material> materials) {
        container.clearAndFill(GUIResourcesUtils.EMPTY_NEUTRAL);
        int index = 0;
        for (final Material material : materials) {
            if (index >= container.getSize()) {
                break;
            }
            container.setObject(index,
                    this.entry(breakSection, allow, material, index));
            ++index;
        }
    }

    private MenuActionButton entry(final boolean breakSection, final boolean allow,
                                   final Material material, final int index) {
        final String name = Messages.get("messages.gui.constraintsEditor.entryName",
                "<yellow>#%index% <white>%material%",
                "%index%", Integer.toString(index + 1),
                "%material%", material.name().toLowerCase(java.util.Locale.ROOT));
        final List<String> lore = List.of(
                Messages.get("messages.gui.constraintsEditor.entrySection",
                        "<gray>%section%, %list%",
                        "%section%", breakSection ? "break" : "place",
                        "%list%", allow ? "allow" : "deny"),
                Messages.get("messages.gui.constraintsEditor.entryRemove",
                        "<red>Click to remove this material."));
        final ItemStack item = ItemUtils.createItem(
                this.icon("entry", GUIResourcesUtils.CONSTRAINT_ENTRY_ICON), name, lore);
        return new MenuActionButton(item,
                MenuConfig.action("constraintseditor", "entry", "remove_constraint"),
                this, this.mine,
                new MenuActionContext.ConstraintRemoval(breakSection, allow, material, this),
                MenuConfig.actionArgument("constraintseditor", "entry", null));
    }

    private ItemStack infoItem() {
        final MineConstraints constraints = this.mine.getBlockConstraints();
        final String name = Messages.get("messages.gui.constraintsEditor.infoName",
                "<aqua><bold>Mine rules");
        final List<String> lore = new ArrayList<>();
        lore.add(Messages.get("messages.gui.constraintsEditor.infoMine",
                "<gray>Mine: <white>%mine%", "%mine%", this.mine.getAlias()));
        lore.add(Messages.get("messages.gui.constraintsEditor.infoBreak",
                "<gray>Break: <aqua>%allow%<gray> allowed, <aqua>%deny%<gray> denied",
                "%allow%", Integer.toString(constraints.breakAllowed().size()),
                "%deny%", Integer.toString(constraints.breakDenied().size())));
        lore.add(Messages.get("messages.gui.constraintsEditor.infoPlace",
                "<gray>Placement checks: <aqua>%state%<gray> (<aqua>%allow%<gray>/<aqua>%deny%<gray>)",
                "%state%", constraints.isPlaceEnabled() ? "on" : "off",
                "%allow%", Integer.toString(constraints.placeAllowed().size()),
                "%deny%", Integer.toString(constraints.placeDenied().size())));
        lore.add(Messages.get("messages.gui.constraintsEditor.infoWave",
                "<gray>Wave: <aqua>%direction%<gray>, <aqua>%layers%<gray>",
                "%direction%", this.mine.getWaveDirection().name(),
                "%layers%", this.mine.getWaveLayersPerTick() == 0
                        ? "global" : this.mine.getWaveLayersPerTick() + "/tick"));
        return ItemUtils.createItem(
                this.icon("info", GUIResourcesUtils.CONSTRAINT_ALLOW_BUTTON), name, lore);
    }
}
