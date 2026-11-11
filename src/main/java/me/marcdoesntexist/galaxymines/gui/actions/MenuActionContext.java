package me.marcdoesntexist.galaxymines.gui.actions;

import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;




public record MenuActionContext(Player player, ItemStack itemInHand, Mine mine,
                                BasicInventoryMenu menu, Object payload, String argument) {

    public record PercentageChange(Material material, double delta) {
    }

    public record Removal(Material material, BasicInventoryMenu returnMenu) {
    }



    public record CommandRemoval(boolean pre, int index, BasicInventoryMenu returnMenu) {
    }



    public record ConstraintRemoval(boolean breakSection, boolean allow, Material material,
                                    BasicInventoryMenu returnMenu) {
    }
}
