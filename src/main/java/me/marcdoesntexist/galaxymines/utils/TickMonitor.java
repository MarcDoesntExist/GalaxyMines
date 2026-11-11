package me.marcdoesntexist.galaxymines.utils;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.mines.ResetMetrics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;












public final class TickMonitor implements Listener {
    private static final int MAX_SAMPLES = 4096;
    private static final TickMonitor INSTANCE = new TickMonitor();

    private final long[] samples = new long[MAX_SAMPLES];
    private int cursor;
    private int filled;
    private boolean registered;

    private TickMonitor() {
    }




    public static void register(final Plugin plugin) {
        if (plugin == null || INSTANCE.registered) {
            return;
        }
        try {
            Bukkit.getPluginManager().registerEvents(INSTANCE, plugin);
            INSTANCE.registered = true;
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Server tick timings are unavailable: " + ex.getMessage());
        }
    }



    public static double averageMillis() {
        return ResetMetrics.averageMillis(INSTANCE.window());
    }



    public static double p95Millis() {
        return ResetMetrics.percentileMillis(INSTANCE.window(), 0.95D);
    }



    public static int samples() {
        return INSTANCE.window().length;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTickEnd(final ServerTickEndEvent event) {
        if (!PluginSettings.get().timings.enabled) {
            return;
        }
        final double millis = event.getTickDuration();
        if (!Double.isFinite(millis) || millis < 0.0D) {
            return;
        }
        this.samples[this.cursor] = (long) (millis * 1_000_000.0D);
        this.cursor = (this.cursor + 1) % MAX_SAMPLES;
        if (this.filled < MAX_SAMPLES) {
            ++this.filled;
        }
    }

    private long[] window() {
        if (this.filled == 0) {
            return new long[0];
        }
        final int configured = Math.max(16, PluginSettings.get().timings.sampleSize);
        final int limit = Math.min(this.filled, Math.min(configured, MAX_SAMPLES));
        final int oldest = this.filled == MAX_SAMPLES
                ? this.cursor
                : this.filled - limit;
        final int base = (oldest + (this.filled == MAX_SAMPLES ? this.filled - limit : 0))
                % MAX_SAMPLES;
        final long[] window = new long[limit];
        for (int i = 0; i < limit; ++i) {
            window[i] = this.samples[(base + i) % MAX_SAMPLES];
        }
        return window;
    }
}
