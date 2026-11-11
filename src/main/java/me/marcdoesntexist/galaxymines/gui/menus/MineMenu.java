package me.marcdoesntexist.galaxymines.gui.menus;

import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicClickableInventoryObject;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.gui.GUIManager;
import me.marcdoesntexist.galaxymines.gui.GUIResourcesUtils;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionButton;
import me.marcdoesntexist.galaxymines.gui.objects.GMinesController;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class MineMenu extends BasicInventoryMenu {
    private final HashMap<Material, MineCompositionEditorMenu> compositionEditInventories;
    private final Mine mine;
    private final MenuActionButton backButton;
    private final BasicClickableInventoryObject resetMaterials;
    private final BasicClickableInventoryObject balanceMaterials;
    private final BasicClickableInventoryObject clearMine;
    private final BasicClickableInventoryObject teleporter;
    private final BasicClickableInventoryObject reseter;
    private final BasicClickableInventoryObject materialClearer;
    private final BasicClickableInventoryObject disableMineButton;
    private final BasicClickableInventoryObject deleteMineButton;
    private final BasicClickableInventoryObject fillMine;
    private final BasicClickableInventoryObject resetEditorButton;
    private final BasicClickableInventoryObject constraintsEditorButton;
    private final BasicClickableInventoryObject waveEditorButton;
    private final BasicClickableInventoryObject tierEditorButton;
    private final MenuActionButton pauseButton;
    private final GMinesController mineMEController;
    private final int infoSlot;
    private final int iconSlot;
    private final int backSlot;
    private MineResetEditorMenu resetEditor;
    private MineConstraintsEditorMenu constraintsEditor;
    private MineWaveEditorMenu waveEditor;
    private MineTierEditorMenu tierEditor;

    public MineMenu(final Mine m) {
        super(buildTitle(m), MenuConfig.rows("mine", 6));
        this.compositionEditInventories = new HashMap<>();
        this.mine = m;
        this.infoSlot = MenuConfig.slot("mine", "info", 53);
        this.iconSlot = MenuConfig.slot("mine", "icon", 49);
        this.backSlot = MenuConfig.slot("mine", "back", 45);
        this.backButton = new MenuActionButton(GUIResourcesUtils.BACK_BUTTON,
                MenuConfig.action("mine", "back", "back"), this, this.mine, GUIManager.mainMenu,
                MenuConfig.actionArgument("mine", "back", null));
        final int[] compositionRange = MenuConfig.range("mine", "composition", 36, 44);
        this.mineMEController =
                new GMinesController(this, this.mine, compositionRange[0], compositionRange[1]);
        this.teleporter = this.button("teleporter", GUIResourcesUtils.TELEPORT_TO_MINE_BUTTON,
                "teleport_mine");
        this.reseter = this.button("reset", GUIResourcesUtils.RESET_MINE_BUTTON, "reset_mine");
        this.clearMine = this.button("clear", GUIResourcesUtils.CLEAR_MINE, "clear_mine");
        this.fillMine = this.button("fill", GUIResourcesUtils.FILL_MINE, "fill_mine");
        this.resetEditorButton = this.button("resetEditor", GUIResourcesUtils.RESET_EDITOR_BUTTON,
                "open_reset_editor");
        this.constraintsEditorButton = this.button("constraintsEditor",
                GUIResourcesUtils.CONSTRAINTS_EDITOR_BUTTON, "open_constraints_editor");
        this.waveEditorButton = this.button("waveEditor", GUIResourcesUtils.WAVE_EDITOR_BUTTON,
                "open_wave_editor");
        this.tierEditorButton = this.button("tierEditor", GUIResourcesUtils.TIER_EDITOR_BUTTON,
                "open_tier_editor");
        this.pauseButton = new MenuActionButton(GUIResourcesUtils.MINE_PAUSE_BUTTON,
                MenuConfig.action("mine", "pause", "toggle_mine_pause"), this, this.mine,
                null, MenuConfig.actionArgument("mine", "pause", null));
        this.materialClearer = this.button("clearMaterials",
                GUIResourcesUtils.MINE_CLEAR_MATERIALS, "clear_materials");
        this.balanceMaterials = this.button("balance", GUIResourcesUtils.BALANCE_MATERIALS,
                "balance_materials");
        this.resetMaterials = this.button("resetMaterials", GUIResourcesUtils.RESET_MATERIALS,
                "reset_materials");
        this.disableMineButton = this.button("disable", GUIResourcesUtils.DISABLE_MINE_BUTTON,
                "disable_mine");
        this.deleteMineButton = this.button("delete", GUIResourcesUtils.DELETE_MINE_BUTTON,
                "delete_mine");
    }

    private static String buildTitle(final Mine mine) {
        return MenuConfig.title("mine", "&1&l%alias%").replace("%alias%", mine.getAlias());
    }

    private MenuActionButton button(final String key, final ItemStack icon,
                                    final String fallbackAction) {
        return new MenuActionButton(icon, MenuConfig.action("mine", key, fallbackAction), this,
                this.mine, null, MenuConfig.actionArgument("mine", key, null));
    }

    @Override
    protected void setupItems() {
        final ItemStack fill = MenuConfig.fill("mine");
        if (fill != null) {
            this.fill(fill);
        }
        this.registerInventoryObject(this.backButton, this.backSlot);
        this.registerInventoryObject(this.teleporter, MenuConfig.slot("mine", "teleporter", 0));
        this.registerInventoryObject(this.reseter, MenuConfig.slot("mine", "reset", 1));
        this.registerInventoryObject(this.clearMine, MenuConfig.slot("mine", "clear", 2));
        this.registerInventoryObject(this.fillMine, MenuConfig.slot("mine", "fill", 3));
        this.registerInventoryObject(this.resetEditorButton,
                MenuConfig.slot("mine", "resetEditor", 4));
        this.registerInventoryObject(this.constraintsEditorButton,
                MenuConfig.slot("mine", "constraintsEditor", 5));
        this.registerInventoryObject(this.waveEditorButton,
                MenuConfig.slot("mine", "waveEditor", 6));
        this.registerInventoryObject(this.pauseButton,
                MenuConfig.slot("mine", "pause", 7));
        this.registerInventoryObject(this.tierEditorButton,
                MenuConfig.slot("mine", "tierEditor", 8));
        this.registerInventoryObject(this.materialClearer,
                MenuConfig.slot("mine", "clearMaterials", 9));
        this.registerInventoryObject(this.balanceMaterials, MenuConfig.slot("mine", "balance", 10));
        this.registerInventoryObject(this.resetMaterials,
                MenuConfig.slot("mine", "resetMaterials", 11));
        this.registerInventoryObject(this.disableMineButton, MenuConfig.slot("mine", "disable", 25));
        this.registerInventoryObject(this.deleteMineButton, MenuConfig.slot("mine", "delete", 26));
        final int[] compositionRange = MenuConfig.range("mine", "composition", 36, 44);
        this.registerInventoryObject(this.mineMEController, compositionRange[0]);
        this.setItem(GUIResourcesUtils.generateInformationItem(this.mine), this.infoSlot);
        this.setItem(this.mine.getIcon(), this.iconSlot);
    }

    @Override
    public void update() {
        this.setItem(GUIResourcesUtils.generateInformationItem(this.mine), this.infoSlot);
        this.setItem(this.mine.getIcon(), this.iconSlot);
        this.pauseButton.setIcon(this.pauseIcon());
        this.mainInventory.setItem(MenuConfig.slot("mine", "pause", 7),
                this.pauseButton.getIcon());
        this.mineMEController.update();
    }

    private ItemStack pauseIcon() {
        final boolean paused = this.mine.isPaused();
        final String name = paused
                ? Messages.get("messages.gui.pauseNamePaused", "<green>Resume the mine")
                : Messages.get("messages.gui.pauseNameRunning", "<red>Pause the mine");
        return me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils.createItem(
                new ItemStack(GUIResourcesUtils.MINE_PAUSE_BUTTON), name,
                java.util.List.of(Messages.get("messages.gui.pauseLore",
                        "<gray>Click to pause or resume the mine.")));
    }

    public Mine getMine() {
        return this.mine;
    }


    public void openResetEditor(final Player p) {
        if (this.resetEditor == null) {
            this.resetEditor = new MineResetEditorMenu(this.mine, this);
            this.resetEditor.initialSetup();
        }
        this.resetEditor.open(p);
    }


    public void openConstraintsEditor(final Player p) {
        if (this.constraintsEditor == null) {
            this.constraintsEditor = new MineConstraintsEditorMenu(this.mine, this);
            this.constraintsEditor.initialSetup();
        }
        this.constraintsEditor.open(p);
    }


    public void openWaveEditor(final Player p) {
        if (this.waveEditor == null) {
            this.waveEditor = new MineWaveEditorMenu(this.mine, this);
            this.waveEditor.initialSetup();
        }
        this.waveEditor.open(p);
    }


    public void openTierEditor(final Player p) {
        if (this.tierEditor == null) {
            this.tierEditor = new MineTierEditorMenu(this.mine, this);
            this.tierEditor.initialSetup();
        }
        this.tierEditor.open(p);
    }

    public void openCompositionEditor(final Material material, final Player p) {
        this.getMineCompositionEditorMenu(material).open(p);
    }

    private MineCompositionEditorMenu getMineCompositionEditorMenu(final Material material) {
        MineCompositionEditorMenu menu = this.compositionEditInventories.get(material);
        if (menu == null) {
            menu = new MineCompositionEditorMenu(this.mine, material, this);
            menu.initialSetup();
            this.compositionEditInventories.put(material, menu);
        }
        return menu;
    }
}
