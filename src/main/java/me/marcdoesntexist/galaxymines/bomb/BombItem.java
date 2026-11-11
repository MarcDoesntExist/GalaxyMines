package me.marcdoesntexist.galaxymines.bomb;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;


public final class BombItem {
    private static final String RADIUS_KEY = "bomb_radius";

    private BombItem() {
    }

    public static ItemStack create(final int radius) {
        final ItemStack item = new ItemStack(Material.TNT);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.parse("<red>Mine Bomb <gray>(radius " + radius + ")"));
            meta.getPersistentDataContainer().set(key(), PersistentDataType.INTEGER, radius);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isBomb(final ItemStack stack) {
        if (stack == null || stack.getType() != Material.TNT) {
            return false;
        }
        final ItemMeta meta = stack.getItemMeta();
        return meta != null
                && meta.getPersistentDataContainer().has(key(), PersistentDataType.INTEGER);
    }

    public static int radius(final ItemStack stack) {
        final ItemMeta meta = stack == null ? null : stack.getItemMeta();
        if (meta == null) {
            return 0;
        }
        final Integer radius = meta.getPersistentDataContainer()
                .get(key(), PersistentDataType.INTEGER);
        return radius == null ? 0 : radius;
    }

    private static NamespacedKey key() {
        return new NamespacedKey(GalaxyMines.getInstance(), RADIUS_KEY);
    }
}
