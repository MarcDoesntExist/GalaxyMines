package me.marcdoesntexist.galaxymines.integration.drops;

import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;


public interface DropSink {


    String id();


    default int priority() {
        return 0;
    }


    List<ItemStack> offer(Player player, Mine mine, List<ItemStack> drops);
}
