package me.marcdoesntexist.galaxymines.api.utils.inventory;

import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;

public class InventoryUtils {
    public static void clearInventory(final Player p) {
        final PlayerInventory inventory = p.getInventory();
        inventory.clear();
        inventory.setArmorContents(null);
    }

    public static void dropPlayer(final Player p) {
        clearInventory(p);
        final PlayerInventory inventory = p.getInventory();
        final Location location = p.getLocation();
        dropInventory(inventory, location);
        final World world = location.getWorld();
        for (final ItemStack itemStack : inventory.getArmorContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                world.dropItemNaturally(location, itemStack);
            }
        }
    }

    public static void dropInventory(final Inventory i, final Location l) {
        final World world = l.getWorld();
        for (final ItemStack itemStack : i) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                world.dropItem(l, itemStack);
            }
        }
    }

    public static boolean wasClickedInLane(final Inventory i, final int clickedSlot,
                                           final int lane) {
        return Arrays.asList(getLane(i, lane)).contains(clickedSlot);
    }

    public static Inventory createInventory(final String title, final int totalLanes) {
        return createInventory(title, totalLanes, null);
    }

    public static Inventory createInventory(final String title, final int totalLanes,
                                            final InventoryHolder holder) {
        return Bukkit.createInventory(holder, totalLanes * 9, Text.parse(title));
    }

    public static int getMiddlePoint(final Inventory m) {
        return getMiddlePoint(m.getSize());
    }

    public static int getBottomMiddlePoint(final Inventory m) {
        return getBottomMiddlePoint(m.getSize());
    }

    public static int getTopMiddlePoint(final Inventory m) {
        return getTopMiddlePoint(m.getSize());
    }

    public static void drawSquare(final Inventory inv, final int pos1, final int pos2,
                                  final ItemStack item) {
        drawSquare(inv, pos1, pos2, item, true);
    }

    public static void drawSquare(final Inventory inv, final int pos1, final int pos2,
                                  final ItemStack item, final boolean replace) {
        for (final int i : getSquare(inv, pos1, pos2)) {
            if (replace) {
                inv.setItem(i, item);
            } else if (inv.getItem(i) != null) {
                inv.setItem(i, item);
            }
        }
    }

    public static int getMiddlePoint(final int size) {
        if (size % 9 != 0 || size == 0) {
            return 0;
        }
        final int totalLanes = size / 9;
        final int middleLane = totalLanes / 2 + 1;
        return middleLane * 9 - 5;
    }

    public static int getBottomMiddlePoint(final int size) {
        if (size % 9 != 0 || size == 0) {
            return 0;
        }
        final int totalLanes = size / 9;
        return totalLanes * 9 - 5;
    }

    public static int getTopMiddlePoint(final int size) {
        if (size % 9 != 0 || size == 0) {
            return 0;
        }
        return 4;
    }

    public static Integer[] getSquare(final Inventory inv, final int pos1, final int pos2) {
        return getSquare(inv.getSize(), pos1, pos2);
    }

    public static Integer[] getSquare(final int size, final int pos1, final int pos2) {
        final int minx = Math.min(pos1 % 9, pos2 % 9);
        final int miny = Math.min(pos1 / 9, pos2 / 9);
        final int maxx = Math.max(pos1 % 9, pos2 % 9);
        final int maxy = Math.max(pos1 / 9, pos2 / 9);
        final ArrayList<Integer> points = new ArrayList<Integer>();
        for (int i = miny; i <= maxy; ++i) {
            for (int j = minx; j <= maxx; ++j) {
                points.add(i * 9 + j);
            }
        }
        return points.toArray(new Integer[points.size()]);
    }

    public static int[] getReferencePoints(final Inventory i, final int pos1, final int pos2) {
        return getReferencePoints(i.getSize(), pos1, pos2);
    }

    public static int[] getReferencePoints(final int size, final int pos1, final int pos2) {
        final int minx = Math.min(pos1 % 9, pos2 % 9);
        final int miny = Math.min(pos1 / 9, pos2 / 9);
        final int maxx = Math.max(pos1 % 9, pos2 % 9);
        final int maxy = Math.max(pos1 / 9, pos2 / 9);
        return new int[]{minx, miny, maxx, maxy};
    }

    public static Integer[] getColumn(final Inventory i, final int lane) {
        return getColumn(i.getSize(), lane);
    }

    public static Integer[] getColumn(final int size, final int column)
            throws IllegalArgumentException {
        if (size % 9 != 0 || size == 0) {
            throw new IllegalArgumentException(
                    "Inventory size " + size + " is invalid! It must be divisible by 9!");
        }
        final int totalCollums = 9;
        if (column > totalCollums) {
            throw new IllegalArgumentException("Column number must be contained within 0-8!");
        }
        final int startingpoint = column;
        final ArrayList<Integer> locations = new ArrayList<Integer>();
        for (int i = startingpoint; i < size; i += 9) {
            locations.add(i);
        }
        return locations.toArray(new Integer[locations.size()]);
    }

    public static Integer[] getLane(final int size, final int lane)
            throws IllegalArgumentException {
        if (size % 9 != 0 || size == 0) {
            throw new IllegalArgumentException(
                    "Inventory size " + size + " is invalid! It must be divisible by 9!");
        }
        final int totalLanes = size / 9;
        if (lane > totalLanes) {
            throw new IllegalArgumentException(
                    "There isn't a lane " + lane + " in this inventory!");
        }
        final int startingpoint = lane * 9;
        final int endpoint = startingpoint + 9;
        final ArrayList<Integer> locations = new ArrayList<Integer>();
        for (int i = startingpoint; i < endpoint; ++i) {
            locations.add(i);
        }
        return locations.toArray(new Integer[locations.size()]);
    }

    public static Integer[] getLane(final Inventory i, final int lane) {
        return getLane(i.getSize(), lane);
    }

    public static Integer[] getPartialLane(final Inventory i, final int lane, final int start,
                                           final int end) {
        return getPartialLane(i.getSize(), lane, start, end);
    }

    public static Integer[] getPartialLane(final int size, final int lane, final int start,
                                           final int end) {
        if (size % 9 != 0 || size == 0) {
            return null;
        }
        final int totalLanes = size / 9;
        if (lane > totalLanes) {
            return null;
        }
        final int startingpoint = lane * 9 + start;
        final int endpoint = lane * 9 + end + 1;
        final ArrayList<Integer> locations = new ArrayList<Integer>();
        for (int i = startingpoint; i < endpoint; ++i) {
            locations.add(i);
        }
        return locations.toArray(new Integer[locations.size()]);
    }

    public static int getTotalLanes(final Inventory i) {
        return getTotalLanes(i.getSize());
    }

    public static int getTotalLanes(final int i) {
        return i / 9;
    }

    public static int getFirstSlotInLane(final int lane) {
        return lane * 9;
    }

    public static int getLastSlotInLane(final int lane) {
        return lane * 9 + 8;
    }

    public static boolean hasInInventory(final ItemStack i, final Inventory inv) {
        return getWithSimilarItem(i, inv).length > 0;
    }

    public static Integer[] getWithSimilarItem(final ItemStack reference, final Inventory inv) {
        final ArrayList<Integer> locations = new ArrayList<Integer>();
        for (int i = 0; i < inv.getSize(); ++i) {
            final ItemStack item = inv.getItem(i);
            if (item != null && ItemUtils.equalMaterial(item, reference)) {
                locations.add(i);
            }
        }
        return locations.toArray(new Integer[locations.size()]);
    }
}
