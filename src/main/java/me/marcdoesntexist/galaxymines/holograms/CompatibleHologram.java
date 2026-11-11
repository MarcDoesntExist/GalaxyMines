package me.marcdoesntexist.galaxymines.holograms;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

public interface CompatibleHologram {
    void clearLines();

    void appendItemLine(final ItemStack p0);

    void appendTextLine(final String p0);

    void setLine(final int p0, final String p1);

    void delete();

    void removeLine(final int p0);

    void move(final World p0, final double p1, final double p2, final double p3);

    void move(final Location p0);

    Location getLocation();

    int size();




    default boolean isSpawned() {
        return true;
    }




    default void recreate() {
    }
}
