package me.marcdoesntexist.galaxymines.integration.customblocks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;




final class NexoCustomBlockProvider extends ReflectiveCustomBlockProvider {
    private final Method placeMethod;
    private final Method isCustomBlockMethod;
    private final Method mechanicMethod;
    private final Method itemFromIdMethod;
    private final Method idFromItemMethod;

    NexoCustomBlockProvider(final Plugin plugin) {
        super(plugin, "Nexo", CustomBlockTokens.NEXO);
        final Class<?> blocks = findClass("com.nexomc.nexo.api.NexoBlocks");
        final Class<?> items = findClass("com.nexomc.nexo.api.NexoItems");
        this.placeMethod = staticMethod(blocks, "place", String.class, Location.class);
        this.isCustomBlockMethod = staticMethod(blocks, "isCustomBlock", Block.class);
        this.mechanicMethod = staticMethod(blocks, "customBlockMechanic", Block.class);
        this.itemFromIdMethod = staticMethod(items, "itemFromId", String.class);
        this.idFromItemMethod = staticMethod(items, "idFromItem", ItemStack.class);
        this.requireApi(this.placeMethod != null && this.isCustomBlockMethod != null
                        && this.mechanicMethod != null && this.itemFromIdMethod != null,
                "Nexo API not found; nexo composition entries keep their vanilla fallback.");
    }

    @Override
    public boolean place(final String id, final Location location) {
        return this.callBoolean(this.placeMethod, null, id, location);
    }

    @Override
    public ItemStack item(final String id) {
        final Object nexoItem = this.call(this.itemFromIdMethod, null, id);
        if (nexoItem == null) {
            return null;
        }
        return asItemStack(this.call(methodByName(nexoItem, "build"), nexoItem));
    }

    @Override
    public String itemId(final ItemStack item) {
        final Object result = this.call(this.idFromItemMethod, null, item);
        return result == null ? null : result.toString();
    }

    @Override
    public boolean matches(final Block block, final String id) {
        if (!Boolean.TRUE.equals(this.call(this.isCustomBlockMethod, null, block))) {
            return false;
        }
        final Object mechanic = this.call(this.mechanicMethod, null, block);
        final String found = this.callString(mechanic, "getItemID", "getBlockID");
        return found != null && found.equalsIgnoreCase(id);
    }
}
