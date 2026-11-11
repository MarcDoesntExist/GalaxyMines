package me.marcdoesntexist.galaxymines.integration.drops;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class DropSinks {

    private static final DropSinks INSTANCE = new DropSinks();

    private final Map<String, DropSink> sinks = new ConcurrentHashMap<>();

    private DropSinks() {
    }

    public static DropSinks get() {
        return INSTANCE;
    }

    private static void debug(final String message) {
        final GalaxyMines plugin = GalaxyMines.getInstance();
        if (plugin != null) {
            plugin.getLogger().warning(message);
        }
    }

    public boolean register(final DropSink sink) {
        if (sink == null || sink.id() == null || sink.id().isBlank()) {
            return false;
        }
        sinks.put(sink.id(), sink);
        return true;
    }

    public boolean unregister(final String id) {
        return id != null && sinks.remove(id) != null;
    }

    public boolean isEmpty() {
        return sinks.isEmpty();
    }

    public List<String> ids() {
        final List<String> out = new ArrayList<>();
        for (final DropSink sink : ordered()) {
            out.add(sink.id());
        }
        return List.copyOf(out);
    }

    public List<ItemStack> offer(final Player player, final Mine mine, final List<ItemStack> drops) {
        if (drops == null || drops.isEmpty() || sinks.isEmpty()) {
            return drops == null ? List.of() : drops;
        }
        List<ItemStack> remaining = List.copyOf(drops);
        for (final DropSink sink : ordered()) {
            if (remaining.isEmpty()) {
                break;
            }
            try {
                final List<ItemStack> leftover = sink.offer(player, mine, remaining);
                if (leftover != null) {
                    remaining = List.copyOf(leftover);
                }
            } catch (RuntimeException exception) {
                debug("Drop sink '" + sink.id() + "' failed: "
                        + exception.getClass().getSimpleName());
            }
        }
        return remaining;
    }

    private List<DropSink> ordered() {
        final List<DropSink> all = new ArrayList<>(sinks.values());
        all.sort(Comparator.<DropSink>comparingInt(DropSink::priority).reversed()
                .thenComparing(DropSink::id));
        return all;
    }
}
