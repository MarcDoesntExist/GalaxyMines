package me.marcdoesntexist.galaxymines.integration.customblocks;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;







public final class CustomBlockItems {
    private static final NamespacedKey TOKEN_KEY = Objects.requireNonNull(
            NamespacedKey.fromString("galaxymines:custom_block"));
    private static final Material FALLBACK = Material.NOTE_BLOCK;

    private CustomBlockItems() {
    }



    public static ItemStack resolve(final String token) {
        final String normalized = CustomBlockTokens.normalize(token);
        if (normalized == null) {
            return null;
        }
        final ItemStack item = CustomBlockRegistry.item(normalized);
        return item == null ? null : mark(item, normalized, false);
    }




    public static ItemStack placeholder(final String token) {
        final String normalized = CustomBlockTokens.normalize(token);
        if (normalized == null) {
            return null;
        }
        final ItemStack resolved = resolve(normalized);
        return resolved != null ? resolved : mark(new ItemStack(FALLBACK), normalized, true);
    }

    public static String tokenOf(final ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return CustomBlockTokens.normalize(meta.getPersistentDataContainer()
                .get(TOKEN_KEY, PersistentDataType.STRING));
    }




    public static boolean matches(final ItemStack first, final ItemStack second) {
        final String firstToken = tokenOf(first);
        final String secondToken = tokenOf(second);
        if (firstToken != null || secondToken != null) {
            return firstToken != null && firstToken.equalsIgnoreCase(secondToken);
        }
        return first != null && second != null && first.getType() == second.getType();
    }



    public static String serialize(final ItemStack item) {
        final String token = tokenOf(item);
        return token != null ? token : item.getType().name();
    }

    public static String display(final ItemStack item) {
        return serialize(item);
    }

    private static ItemStack mark(final ItemStack item, final String token, final boolean label) {
        final ItemStack copy = item.clone();
        copy.setAmount(1);
        final ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(TOKEN_KEY, PersistentDataType.STRING, token);
            if (label && !meta.hasDisplayName()) {
                meta.displayName(Component.text(token));
            }
            copy.setItemMeta(meta);
        }
        return copy;
    }
}
