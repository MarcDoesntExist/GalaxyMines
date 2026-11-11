package me.marcdoesntexist.galaxymines.gui;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.utils.inventory.MenuListener;
import me.marcdoesntexist.galaxymines.config.GalaxyMinesConfiguration;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.gui.actions.MenuActionRegistry;
import me.marcdoesntexist.galaxymines.gui.menus.MainMenu;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class GUIManager {
    public static boolean ready = false;
    public static MainMenu mainMenu;
    private static boolean listenerRegistered;

    public static void setup() {
        if (!listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(new MenuListener(), GalaxyMines.getInstance());
            listenerRegistered = true;
        }
        GuiChatInput.register(GalaxyMines.getInstance());
        GuiChatInput.clearAll();
        MenuConfig.load(GalaxyMinesConfiguration.guiConfig);
        for (final String warning : MenuConfig.validate()) {
            GalaxyMines.instance.debug("Menu.yml: " + warning, true);
        }
        for (final String warning : MenuActionRegistry.validate()) {
            GalaxyMines.instance.debug("Menu.yml: " + warning, true);
        }
        GUIResourcesUtils.reload();
        final String mainMenuName = MenuConfig.title("main", "&1&lGalaxyMines");
        GalaxyMines.instance.debug("Loading GUI title ---> " + mainMenuName, 3);
        if (mainMenu != null) {
            mainMenu.disable();
        }
        mainMenu = new MainMenu(mainMenuName, MenuConfig.rows("main", 6));
        mainMenu.initialSetup();
        ready = true;
    }

    public static void registerItem(String name, ItemStack i) {
        GUIResourcesUtils.customItems.put(name.toUpperCase(), i);
    }

    public static ItemStack getItem(String name) {
        return GUIResourcesUtils.customItems.get(name);
    }

}
