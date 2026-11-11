package me.marcdoesntexist.galaxymines.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.mines.MineBounds;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;






public final class WorldGuardRegionProtector implements MineRegionProtector {
    private static final String REGION_PREFIX = "galaxymines_";
    private final Plugin plugin;
    private final Logger logger;
    private Object regionContainer;

    public WorldGuardRegionProtector(final Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    private static Object convertFlagValue(final Object flag, final String rawValue) {
        final String value = rawValue == null ? "" : rawValue.trim();
        final String typeName = flag.getClass().getSimpleName();
        if (typeName.contains("StateFlag")) {
            try {
                final Class<?> state = Class.forName(
                        "com.sk89q.worldguard.protection.flags.StateFlag$State");
                for (final Object constant : state.getEnumConstants()) {
                    if (constant.toString().equalsIgnoreCase(value)) {
                        return constant;
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (typeName.contains("BooleanFlag")) {
            return Boolean.parseBoolean(value);
        }
        if (typeName.contains("IntegerFlag")) {
            return Integer.parseInt(value);
        }
        if (typeName.contains("DoubleFlag")) {
            return Double.parseDouble(value);
        }
        return value;
    }

    private static String regionId(final String mineName) {
        return REGION_PREFIX + mineName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_");
    }

    private static Object invoke(final Object target, final String method, final Object... args)
            throws Exception {
        for (final Method candidate : target.getClass().getMethods()) {
            if (candidate.getName().equals(method)
                    && candidate.getParameterCount() == args.length) {
                return candidate.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(method);
    }

    @Override
    public void reload() {
        try {
            final Class<?> worldGuard = Class.forName("com.sk89q.worldguard.WorldGuard");
            final Object instance = worldGuard.getMethod("getInstance").invoke(null);
            final Object platform = invoke(instance, "getPlatform");
            this.regionContainer = invoke(platform, "getRegionContainer");
        } catch (Throwable t) {
            this.regionContainer = null;
            this.logger.log(Level.WARNING,
                    "WorldGuard region container is unavailable: mine regions will not be created.",
                    t);
        }
    }

    @Override
    public void onMineRegistered(final Mine mine) {
        if (mine == null || !mine.isWorldGuardEnabled() || this.regionContainer == null) {
            return;
        }
        try {
            final Object manager = manager(mine.getWorld());
            if (manager == null) {
                return;
            }
            final String id = regionId(mine.getName());
            final Object existing = invoke(manager, "getRegion", id);
            final Object region = existing == null ? createRegion(mine, id) : existing;
            if (region == null) {
                return;
            }
            applyFlags(region, mine.getWorldGuardFlags());
            if (existing == null) {
                manager.getClass().getMethod("addRegion",
                                Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion"))
                        .invoke(manager, region);
            }
        } catch (Throwable t) {
            this.warn("Could not create the WorldGuard region for mine " + mine.getName(), t);
        }
    }

    @Override
    public void onMineDeleted(final Mine mine) {
        if (mine == null || this.regionContainer == null) {
            return;
        }
        removeRegion(mine.getWorld(), mine.getName());
    }

    @Override
    public void onMineRenamed(final Mine mine, final String oldName) {
        if (mine == null) {
            return;
        }
        removeRegion(mine.getWorld(), oldName);
        onMineRegistered(mine);
    }

    @Override
    public void close() {
        this.regionContainer = null;
    }

    private void removeRegion(final World world, final String mineName) {
        try {
            final Object manager = manager(world);
            if (manager == null) {
                return;
            }
            manager.getClass().getMethod("removeRegion", String.class)
                    .invoke(manager, regionId(mineName));
        } catch (Throwable t) {
            this.warn("Could not remove the WorldGuard region for mine " + mineName, t);
        }
    }

    private Object createRegion(final Mine mine, final String id) throws Exception {
        final MineBounds bounds = mine.getBounds();
        final Class<?> vector = Class.forName("com.sk89q.worldedit.math.BlockVector3");
        final Object minimum = vector.getMethod("at", int.class, int.class, int.class)
                .invoke(null, bounds.minX(), bounds.minY(), bounds.minZ());
        final Object maximum = vector.getMethod("at", int.class, int.class, int.class)
                .invoke(null, bounds.maxX(), bounds.maxY(), bounds.maxZ());
        final Class<?> regionClass =
                Class.forName("com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion");
        return regionClass.getConstructor(String.class, vector, vector)
                .newInstance(id, minimum, maximum);
    }

    private void applyFlags(final Object region, final Map<String, String> flags) throws Exception {
        if (flags == null || flags.isEmpty()) {
            return;
        }
        final Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
        final Method setFlag = region.getClass().getMethod("setFlag",
                Class.forName("com.sk89q.worldguard.protection.flags.Flag"), Object.class);
        for (final Map.Entry<String, String> entry : flags.entrySet()) {
            final String fieldName = entry.getKey().toUpperCase(Locale.ROOT)
                    .replace('-', '_').replace('.', '_');
            final Object flag;
            try {
                flag = flagsClass.getField(fieldName).get(null);
            } catch (NoSuchFieldException ex) {
                this.logger.fine("WorldGuard has no flag named " + entry.getKey() + "; ignoring it.");
                continue;
            }
            setFlag.invoke(region, flag, convertFlagValue(flag, entry.getValue()));
        }
    }

    private Object manager(final World world) throws Exception {
        if (this.regionContainer == null || world == null) {
            return null;
        }
        final Object worldEditWorld = BukkitAdapter.adapt(world);
        return this.regionContainer.getClass()
                .getMethod("get", Class.forName("com.sk89q.worldedit.world.World"))
                .invoke(this.regionContainer, worldEditWorld);
    }

    private void warn(final String message, final Throwable throwable) {
        this.logger.log(Level.FINE, message, throwable);
        this.logger.warning(message + " (" + throwable.getClass().getSimpleName() + ": "
                + throwable.getMessage() + ")");
    }
}
