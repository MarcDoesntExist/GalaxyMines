package me.marcdoesntexist.galaxymines.integration.customblocks;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;





abstract class ReflectiveCustomBlockProvider implements CustomBlockProvider {
    private final Plugin plugin;
    private final String pluginName;
    private final String namespace;
    private final Set<String> warned = ConcurrentHashMap.newKeySet();
    private boolean available;

    protected ReflectiveCustomBlockProvider(final Plugin plugin, final String pluginName,
                                            final String namespace) {
        this.plugin = plugin;
        this.pluginName = pluginName;
        this.namespace = namespace;
        final Plugin target = plugin.getServer().getPluginManager().getPlugin(pluginName);
        this.available = target != null && target.isEnabled();
    }

    protected static Class<?> findClass(final String... names) {
        for (final String name : names) {
            try {
                return Class.forName(name);
            } catch (final Throwable ignored) {
            }
        }
        return null;
    }

    protected static Method staticMethod(final Class<?> type, final String name,
                                         final Class<?>... parameters) {
        if (type == null) {
            return null;
        }
        try {
            final Method method = type.getMethod(name, parameters);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        } catch (final NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }

    protected static Method instanceMethod(final Class<?> type, final String name,
                                           final Class<?>... parameters) {
        if (type == null) {
            return null;
        }
        try {
            return type.getMethod(name, parameters);
        } catch (final NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }

    protected static Method methodByName(final Object target, final String... names) {
        if (target == null) {
            return null;
        }
        for (final String name : names) {
            for (final Method method : target.getClass().getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 0) {
                    return method;
                }
            }
        }
        return null;
    }

    protected static ItemStack asItemStack(final Object value) {
        return value instanceof ItemStack stack ? stack : null;
    }

    private static String describe(final Throwable throwable) {
        final String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName()
                : throwable.getClass().getSimpleName() + ": " + message;
    }

    @Override
    public final String namespace() {
        return this.namespace;
    }

    @Override
    public final boolean available() {
        return this.available;
    }

    protected final void requireApi(final boolean condition, final String message) {
        if (!this.available || condition) {
            return;
        }
        this.available = false;
        this.warn(message);
    }

    protected final void warn(final String message) {
        if (this.warned.add(message)) {
            this.plugin.getLogger().warning("[" + this.pluginName + "] " + message);
        }
    }

    protected Object call(final Method method, final Object target, final Object... arguments) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target, arguments);
        } catch (final InvocationTargetException ex) {
            this.warn(method.getName() + " failed: " + describe(ex.getTargetException()));
            return null;
        } catch (final ReflectiveOperationException | RuntimeException ex) {
            this.warn(method.getName() + " failed: " + describe(ex));
            return null;
        }
    }





    protected boolean callBoolean(final Method method, final Object target,
                                  final Object... arguments) {
        if (method == null) {
            return false;
        }
        final Object result = this.call(method, target, arguments);
        if (method.getReturnType() == void.class) {
            return true;
        }
        if (result instanceof Boolean value) {
            return value;
        }
        return result != null;
    }

    protected String callString(final Object target, final String... names) {
        final Object result = this.call(methodByName(target, names), target);
        return result == null ? null : result.toString();
    }
}
