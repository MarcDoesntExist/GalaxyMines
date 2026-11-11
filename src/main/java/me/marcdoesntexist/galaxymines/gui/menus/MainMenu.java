package me.marcdoesntexist.galaxymines.gui.menus;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicClickableInventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicInventoryContainer;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicInventoryItem;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.compat.Materials;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.config.MineConfig;
import me.marcdoesntexist.galaxymines.gui.GUIManager;
import me.marcdoesntexist.galaxymines.gui.GUIResourcesUtils;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionButton;
import me.marcdoesntexist.galaxymines.gui.objects.MineDisplay;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.mines.MineManager;
import me.marcdoesntexist.galaxymines.thread.MineFilesFinder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class MainMenu extends BasicInventoryMenu {
    protected final HashMap<Mine, MineMenu> inventories = new HashMap<>();
    private final BasicInventoryContainer mineList;
    private final BasicInventoryContainer unloadedMineList;
    private final BasicClickableInventoryObject refreshButton;
    private final int mineListStart;
    private final int unloadedListStart;
    private final int refreshSlot;
    private final int infoSlot;

    public MainMenu(final String name, final int rows) {
        super(name, rows);
        final int[] mineRange = MenuConfig.range("main", "mineList", 0, 24);
        final int[] unloadedRange = MenuConfig.range("main", "unloadedList", 36, 44);
        this.mineListStart = mineRange[0];
        this.unloadedListStart = unloadedRange[0];
        this.mineList = new BasicInventoryContainer(this, mineRange[0], mineRange[1]);
        this.unloadedMineList = new BasicInventoryContainer(this, unloadedRange[0],
                unloadedRange[1]);
        this.refreshSlot = MenuConfig.slot("main", "refresh", 26);
        this.infoSlot = MenuConfig.slot("main", "info", 49);
        this.refreshButton = new MenuActionButton(GUIResourcesUtils.REFRESH_MENU,
                MenuConfig.action("main", "refresh", "refresh"), this, null, null,
                MenuConfig.actionArgument("main", "refresh", null));
    }

    @Override
    public void update() {
        this.mineList.clearAndFill(GUIResourcesUtils.NO_MINE_TO_DISPLAY);
        for (final Mine mine : MineManager.getMines()) {
            try {
                this.mineList.addObject(new MineDisplay(mine, new ItemStack(mine.getIcon()), this));
            } catch (Exception e) {
                GalaxyMines.instance.printException(
                        "There was a problem loading the icon for mine " + mine.getName(),
                        e);
            }
        }
        this.unloadedMineList.clearAndFill(GUIResourcesUtils.NOT_LOADED_MINES);
        final GalaxyMines plugin = GalaxyMines.getInstance();
        new MineFilesFinder(plugin).find().thenAccept(result ->
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
                    if (!this.equals(GUIManager.mainMenu)) {
                        return;
                    }
                    this.unloadedMineList.clearAndFill(GUIResourcesUtils.NOT_LOADED_MINES);
                    this.unloadedMineList.setObject(8,
                            new BasicInventoryItem(this, this.notLoadedItem(result)));
                    for (final org.bukkit.configuration.file.FileConfiguration file : result.files()) {
                        try {
                            final MineConfig mine = new MineConfig(file);
                            if (MineManager.getMine(mine.getName()) != null) {
                                continue;
                            }
                            ItemStack icon = this.generateItemStack(mine);
                            icon = ItemUtils.addToLore(icon, GUIResourcesUtils.CLICK_TO_LOAD);
                            this.unloadedMineList.addObject(new MenuActionButton(icon,
                                    MenuConfig.action("main", "unloadedMine", "load_mine"), this,
                                    null, mine,
                                    MenuConfig.actionArgument("main", "unloadedMine", null)));
                        } catch (RuntimeException ex) {
                            plugin.debug("Invalid mine file during GUI scan: "
                                    + ex.getMessage(), true);
                        }
                    }
                    plugin.debug("Found " + result.files().size() + " files in "
                            + result.elapsedMillis() + "ms");
                })).exceptionally(ex -> {
            plugin.printException("Could not scan mine files", ex);
            return null;
        });
    }

    @Override
    protected void setupItems() {
        final ItemStack fill = MenuConfig.fill("main");
        this.fill(fill == null ? GUIResourcesUtils.EMPTY_NEUTRAL : fill);
        this.setItem(GUIResourcesUtils.MAIN_ICON, this.infoSlot);
        this.registerInventoryObject(this.refreshButton, this.refreshSlot);
        this.registerInventoryObject(this.mineList, this.mineListStart);
        this.registerInventoryObject(this.unloadedMineList, this.unloadedListStart);
    }

    private ItemStack notLoadedItem(final MineFilesFinder.Result result) {
        ItemStack item = new ItemStack(GUIResourcesUtils.FILES_SEARCH_RESULT);
        if (result.notLoadedFiles().isEmpty()) {
            item = ItemUtils.addToLore(item, GUIResourcesUtils.NO_MISFORMATTED_FILES);
        } else {
            item = ItemUtils.addToLore(item, result.notLoadedFiles());
        }
        return ItemUtils.addToLore(item, GUIResourcesUtils.FOUND_MINE_FILES
                .replace("%total%", String.valueOf(result.files().size())));
    }

    private ItemStack generateItemStack(final MineConfig config) {
        final FileConfiguration mineConfig = config.getConfig();
        Material iconMaterial = Materials.resolve(mineConfig.getString("icon.type"),
                mineConfig.getInt("icon.data"));
        if (iconMaterial == null) {
            iconMaterial = Material.STONE;
        }
        ItemStack item = ItemUtils.createItem(new ItemStack(iconMaterial, 1),
                Messages.get("messages.gui.unloadedMine.name", "&a%name%.yml",
                        "%name%", config.getName()));
        item = ItemUtils.addToLore(item,
                Messages.get("messages.gui.unloadedMine.alias", "&7Alias: %alias%",
                        "%alias%", config.getAlias()),
                Messages.get("messages.gui.unloadedMine.type", "&7Type: %type%",
                        "%type%", config.getType().name()),
                Messages.get("messages.gui.unloadedMine.world", "&7World: %world%",
                        "%world%", config.getWorld().getName()));
        return item;
    }

    public void openMineMenu(final Mine m, final Player p) {
        this.getMineInventory(m).open(p);
    }

    public MineMenu getMineInventory(final Mine m) {
        if (this.inventories.containsKey(m)) {
            return this.inventories.get(m);
        }
        GalaxyMines.instance.debug("Creating a menu for mine " + m.getName(), 2);
        final MineMenu inv = new MineMenu(m);
        inv.initialSetup();
        this.inventories.put(m, inv);
        return inv;
    }
}
