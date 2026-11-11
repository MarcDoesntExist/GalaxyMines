package me.marcdoesntexist.galaxymines.gui;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.messages.GalaxyMineMessageManager;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;


public class GUIResourcesUtils {
    public static final int INVENTORY_SIZE = 6;
    static final HashMap<String, ItemStack> customItems = new HashMap<>();

    public static ItemStack SPLITTER;
    public static ItemStack EMPTY_MATERIAL;
    public static ItemStack EMPTY_EFFECT;
    public static ItemStack EMPTY_NEUTRAL;
    public static ItemStack BACK_BUTTON;
    public static ItemStack TELEPORT_TO_MINE_BUTTON;
    public static ItemStack TOGGLE_MATERIALS_EFFECTS_SELECTION;
    public static ItemStack FILL_MINE;
    public static ItemStack BALANCE_MATERIALS;
    public static ItemStack RESET_MATERIALS;
    public static ItemStack REMOVE_MATERIAL_BUTTON;
    public static ItemStack RESET_MINE_BUTTON;
    public static ItemStack CLEAR_MINE;
    public static ItemStack DELETE_MINE_BUTTON;
    public static ItemStack MINE_CLEAR_MATERIALS;
    public static ItemStack DISABLE_MINE_BUTTON;
    public static ItemStack NOT_LOADED_MINES;
    public static ItemStack NO_MINE_TO_DISPLAY;
    public static ItemStack NOT_LOADED_ICON;
    public static ItemStack MINE_COMPOSITION_INFORMATION;
    public static ItemStack REFRESH_MENU;
    public static ItemStack FILES_SEARCH_RESULT;
    public static ItemStack MAIN_ICON;
    public static ItemStack RESET_EDITOR_BUTTON;
    public static ItemStack RESET_THRESHOLD_BUTTON;
    public static ItemStack RESET_MIN_BUTTON;
    public static ItemStack RESET_MAX_BUTTON;
    public static ItemStack RESET_INTERVAL_BUTTON;
    public static ItemStack ADD_RESET_COMMAND_BUTTON;
    public static ItemStack RESET_COMMAND_ICON;
    public static ItemStack CONSTRAINTS_EDITOR_BUTTON;
    public static ItemStack CONSTRAINT_ALLOW_BUTTON;
    public static ItemStack CONSTRAINT_DENY_BUTTON;
    public static ItemStack CONSTRAINT_CLEAR_BUTTON;
    public static ItemStack CONSTRAINT_PLACE_BUTTON;
    public static ItemStack CONSTRAINT_ENTRY_ICON;
    public static ItemStack WAVE_DIRECTION_BUTTON;
    public static ItemStack WAVE_LAYERS_BUTTON;
    public static ItemStack WAVE_EDITOR_BUTTON;
    public static ItemStack WAVE_MINUS_BUTTON;
    public static ItemStack WAVE_PLUS_BUTTON;
    public static ItemStack WAVE_SET_BUTTON;
    public static ItemStack WAVE_LAYER_ON;
    public static ItemStack WAVE_LAYER_OFF;
    public static ItemStack TIER_EDITOR_BUTTON;
    public static ItemStack TIER_LEVEL_BUTTON;
    public static ItemStack TIER_BLOCKS_BUTTON;
    public static ItemStack TIER_EXPAND_BUTTON;
    public static ItemStack TIER_CONFIRM_BUTTON;
    public static ItemStack MINE_PAUSE_BUTTON;

    public static String FOUND_MINE_FILES;
    public static String NO_MISFORMATTED_FILES;
    public static String CLICK_TO_EDIT;
    public static String CLICK_TO_SEE;
    public static String CLICK_TO_LOAD;
    public static String CLICK_TO_REMOVE;

    private GUIResourcesUtils() {
    }


    public static void reload() {
        customItems.clear();
        SPLITTER = item("splitter", Material.IRON_BARS);
        EMPTY_MATERIAL = item("emptyMaterial", Material.BLACK_STAINED_GLASS_PANE);
        EMPTY_EFFECT = item("emptyEffect", Material.BLACK_STAINED_GLASS_PANE);
        EMPTY_NEUTRAL = item("empty", Material.BLACK_STAINED_GLASS_PANE);
        BACK_BUTTON = item("back", Material.EMERALD);
        TELEPORT_TO_MINE_BUTTON = item("teleporter", Material.ENDER_PEARL);
        TOGGLE_MATERIALS_EFFECTS_SELECTION = item("toogleMaterialEditor", Material.SLIME_BALL);
        FILL_MINE = item("fillMine", Material.CHEST);
        BALANCE_MATERIALS = item("balanceMaterials", Material.IRON_INGOT);
        RESET_MATERIALS = item("resetMaterials", Material.GOLD_INGOT);
        REMOVE_MATERIAL_BUTTON = item("removeMaterial", Material.BARRIER);
        RESET_MINE_BUTTON = item("resetNow", Material.WOODEN_PICKAXE);
        CLEAR_MINE = item("clearMine", Material.REDSTONE_BLOCK);
        DELETE_MINE_BUTTON = item("deleteMine", Material.TNT);
        MINE_CLEAR_MATERIALS = item("clearMaterials", Material.BARRIER);
        DISABLE_MINE_BUTTON = item("disableMine", Material.REDSTONE);
        NOT_LOADED_MINES = item("unloadedMines", Material.BLACK_STAINED_GLASS_PANE);
        NO_MINE_TO_DISPLAY = item("noMine", Material.BLACK_STAINED_GLASS_PANE);
        NOT_LOADED_ICON = item("iconNotLoaded", Material.BARRIER);
        MINE_COMPOSITION_INFORMATION = item("info", Material.BOOK);
        REFRESH_MENU = item("refresh", Material.DIAMOND);
        FILES_SEARCH_RESULT = item("couldNotLoadFiles", Material.BOOK);
        RESET_EDITOR_BUTTON = item("resetEditor", Material.CLOCK);
        RESET_THRESHOLD_BUTTON = item("resetThreshold", Material.CLOCK);
        RESET_MIN_BUTTON = item("resetMin", Material.REDSTONE_TORCH);
        RESET_MAX_BUTTON = item("resetMax", Material.REDSTONE_TORCH);
        RESET_INTERVAL_BUTTON = item("resetInterval", Material.REPEATER);
        ADD_RESET_COMMAND_BUTTON = item("addResetCommand", Material.WRITABLE_BOOK);
        RESET_COMMAND_ICON = item("resetCommand", Material.PAPER);
        CONSTRAINTS_EDITOR_BUTTON = item("constraintsEditor", Material.IRON_DOOR);
        CONSTRAINT_ALLOW_BUTTON = item("constraintAllow", Material.LIME_DYE);
        CONSTRAINT_DENY_BUTTON = item("constraintDeny", Material.RED_DYE);
        CONSTRAINT_CLEAR_BUTTON = item("constraintClear", Material.BARRIER);
        CONSTRAINT_PLACE_BUTTON = item("constraintPlace", Material.LEVER);
        CONSTRAINT_ENTRY_ICON = item("constraintEntry", Material.PAPER);
        WAVE_DIRECTION_BUTTON = item("waveDirection", Material.COMPASS);
        WAVE_LAYERS_BUTTON = item("waveLayers", Material.REPEATER);
        WAVE_EDITOR_BUTTON = item("waveEditor", Material.SEA_LANTERN);
        WAVE_MINUS_BUTTON = item("waveMinus", Material.RED_DYE);
        WAVE_PLUS_BUTTON = item("wavePlus", Material.LIME_DYE);
        WAVE_SET_BUTTON = item("waveSet", Material.REPEATER);
        WAVE_LAYER_ON = item("waveLayerOn", Material.LIME_STAINED_GLASS_PANE);
        WAVE_LAYER_OFF = item("waveLayerOff", Material.GRAY_STAINED_GLASS_PANE);
        TIER_EDITOR_BUTTON = item("tierEditor", Material.EXPERIENCE_BOTTLE);
        TIER_LEVEL_BUTTON = item("tierLevel", Material.EXPERIENCE_BOTTLE);
        TIER_BLOCKS_BUTTON = item("tierBlocks", Material.STONE);
        TIER_EXPAND_BUTTON = item("tierExpand", Material.GRASS_BLOCK);
        TIER_CONFIRM_BUTTON = item("tierConfirm", Material.LIME_CONCRETE);
        MINE_PAUSE_BUTTON = item("pauseMine", Material.CLOCK);
        MAIN_ICON = MenuConfig.item("main", "info");
        if (MAIN_ICON == null) {
            MAIN_ICON = ItemUtils.createItem(Material.GOLDEN_PICKAXE, "<gold>GalaxyMines");
        }

        FOUND_MINE_FILES = string("foundFiles", "");
        NO_MISFORMATTED_FILES = string("noMisformattedFiles", "");
        CLICK_TO_EDIT = string("clickToEdit", "");
        CLICK_TO_SEE = string("clickToSeeMine", "");
        CLICK_TO_LOAD = string("clickToLoad", "");
        CLICK_TO_REMOVE = string("clickToRemove", "");
    }

    public static ItemStack generateNotYetLoadedIcon(final Mine m) {
        final ItemStack itemStack = new ItemStack(NOT_LOADED_ICON);
        return GalaxyMineMessageManager.getInstance().translateItemStack(itemStack, m);
    }


    public static double getCompositionChangeValue(final ItemStack i) {
        if (ItemUtils.checkDisplayName(i) && i.getItemMeta().getDisplayName() != null) {
            String intString = i.getItemMeta().getDisplayName().replace("\u00a7a+", "")
                    .replace("\u00a7c", "").replace("%", "");
            try {
                return Double.parseDouble(intString);
            } catch (NumberFormatException e) {
                GalaxyMines.getInstance().printException(
                        "Error getting change value for " + intString, e);
                return 0.0;
            }
        }
        return 0.0;
    }

    public static ItemStack generateInformationItem(final Mine mine) {
        return GalaxyMineMessageManager.getInstance()
                .translateItemStack(new ItemStack(MINE_COMPOSITION_INFORMATION), mine);
    }

    private static ItemStack item(final String key, final Material fallback) {
        final ItemStack item = MenuConfig.item(key);
        if (item != null) {
            return item;
        }
        GalaxyMines.instance.debug("Menu.yml: entry '" + key + "' missing, using " + fallback, true);
        return ItemUtils.createItem(fallback, "<red>Missing: " + key);
    }

    private static String string(final String key, final String fallback) {
        return MenuConfig.string("config." + key, fallback);
    }
}
