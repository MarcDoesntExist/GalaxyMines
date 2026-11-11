package me.marcdoesntexist.galaxymines.integration.customblocks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;


final class ItemsAdderCustomBlockProvider extends ReflectiveCustomBlockProvider {
    private final Method placeMethod;
    private final Method byPlacedMethod;
    private final Method blockGetInstanceMethod;
    private final Method stackGetInstanceMethod;
    private final Method stackByItemStackMethod;
    private final Method stackNamespacedIdMethod;
    private final Method blockItemStackMethod;
    private final Method stackItemStackMethod;

    ItemsAdderCustomBlockProvider(final Plugin plugin) {
        super(plugin, "ItemsAdder", CustomBlockTokens.ITEMSADDER);
        final Class<?> block = findClass("dev.lone.itemsadder.api.CustomBlock");
        final Class<?> stack = findClass("dev.lone.itemsadder.api.CustomStack");
        this.placeMethod = staticMethod(block, "place", String.class, Location.class);
        this.byPlacedMethod = staticMethod(block, "byAlreadyPlaced", Block.class);
        this.blockGetInstanceMethod = staticMethod(block, "getInstance", String.class);
        this.stackGetInstanceMethod = staticMethod(stack, "getInstance", String.class);
        this.stackByItemStackMethod = staticMethod(stack, "byItemStack", ItemStack.class);
        this.stackNamespacedIdMethod = instanceMethod(stack, "getNamespacedID");
        this.blockItemStackMethod = instanceMethod(block, "getItemStack");
        this.stackItemStackMethod = instanceMethod(stack, "getItemStack");
        this.requireApi(this.placeMethod != null && this.byPlacedMethod != null
                        && (this.blockGetInstanceMethod != null
                        || this.stackGetInstanceMethod != null)
                        && (this.blockItemStackMethod != null
                        || this.stackItemStackMethod != null),
                "ItemsAdder API not found; itemsadder composition entries keep their vanilla fallback.");
    }

    private static String fullId(final String id) {
        return id.indexOf(':') >= 0 ? id : CustomBlockTokens.ITEMSADDER + ":" + id;
    }

    @Override
    public boolean place(final String id, final Location location) {
        return this.callBoolean(this.placeMethod, null, fullId(id), location);
    }

    @Override
    public ItemStack item(final String id) {
        final Object custom = this.call(this.stackGetInstanceMethod, null, fullId(id));
        if (custom != null && this.stackItemStackMethod != null) {
            final ItemStack item = asItemStack(this.call(this.stackItemStackMethod, custom));
            if (item != null) {
                return item;
            }
        }
        final Object block = this.call(this.blockGetInstanceMethod, null, fullId(id));
        if (block != null && this.blockItemStackMethod != null) {
            return asItemStack(this.call(this.blockItemStackMethod, block));
        }
        return null;
    }

    @Override
    public String itemId(final ItemStack item) {
        final Object custom = this.call(this.stackByItemStackMethod, null, item);
        if (custom == null) {
            return null;
        }
        final Object id = this.call(this.stackNamespacedIdMethod, custom);
        return id == null ? null : id.toString();
    }

    @Override
    public boolean matches(final Block block, final String id) {
        final Object custom = this.call(this.byPlacedMethod, null, block);
        final String found = this.callString(custom, "getNamespacedID");
        if (found == null) {
            return false;
        }
        final String full = fullId(id);
        return found.equalsIgnoreCase(full) || found.equalsIgnoreCase(id)
                || found.toLowerCase(Locale.ROOT)
                .endsWith(":" + id.toLowerCase(Locale.ROOT));
    }
}
