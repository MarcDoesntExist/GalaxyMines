package me.marcdoesntexist.galaxymines.integration.customblocks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;




final class OraxenCustomBlockProvider extends ReflectiveCustomBlockProvider {
    private final Method placeMethod;
    private final Method isOraxenBlockMethod;
    private final Method mechanicBlockMethod;
    private final Method mechanicLocationMethod;
    private final Method itemByIdMethod;
    private final Method idByItemMethod;

    OraxenCustomBlockProvider(final Plugin plugin) {
        super(plugin, "Oraxen", CustomBlockTokens.ORAXEN);
        final Class<?> blocks = findClass("io.th0rgal.oraxen.api.OraxenBlocks");
        final Class<?> items = findClass("io.th0rgal.oraxen.api.OraxenItems");
        this.placeMethod = staticMethod(blocks, "place", String.class, Location.class);
        this.isOraxenBlockMethod = staticMethod(blocks, "isOraxenBlock", Block.class);
        this.mechanicBlockMethod = staticMethod(blocks, "getOraxenBlock", Block.class);
        this.mechanicLocationMethod = staticMethod(blocks, "getOraxenBlock", Location.class);
        this.itemByIdMethod = staticMethod(items, "getItemById", String.class);
        this.idByItemMethod = staticMethod(items, "getIdByItem", ItemStack.class);
        this.requireApi(this.placeMethod != null && this.isOraxenBlockMethod != null
                        && this.itemByIdMethod != null
                        && (this.mechanicBlockMethod != null
                        || this.mechanicLocationMethod != null),
                "Oraxen API not found; oraxen composition entries keep their vanilla fallback.");
    }

    @Override
    public boolean place(final String id, final Location location) {
        return this.callBoolean(this.placeMethod, null, id, location);
    }

    @Override
    public ItemStack item(final String id) {
        final Object itemBuilder = this.call(this.itemByIdMethod, null, id);
        if (itemBuilder == null) {
            return null;
        }
        return asItemStack(this.call(methodByName(itemBuilder, "build"), itemBuilder));
    }

    @Override
    public String itemId(final ItemStack item) {
        final Object result = this.call(this.idByItemMethod, null, item);
        return result == null ? null : result.toString();
    }

    @Override
    public boolean matches(final Block block, final String id) {
        if (!Boolean.TRUE.equals(this.call(this.isOraxenBlockMethod, null, block))) {
            return false;
        }
        final Object mechanic = this.mechanicBlockMethod != null
                ? this.call(this.mechanicBlockMethod, null, block)
                : this.call(this.mechanicLocationMethod, null, block.getLocation());
        final String found = this.callString(mechanic, "getItemID");
        return found != null && found.equalsIgnoreCase(id);
    }
}
