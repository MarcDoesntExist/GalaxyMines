package me.marcdoesntexist.galaxymines.integration.customblocks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;


public interface CustomBlockProvider {
    String namespace();

    boolean available();


    boolean place(String id, Location location);


    ItemStack item(String id);


    default String itemId(final ItemStack item) {
        return null;
    }

    boolean matches(Block block, String id);
}
