package me.marcdoesntexist.galaxymines.multiplier;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;















public final class MultiplierSources {
    private static final MultiplierSources INSTANCE = new MultiplierSources();
    private static final double DEFAULT_CAP = 3.0D;
    private static final double MIN_CAP = 1.0D;
    private static final double MAX_CAP = 100.0D;

    private final Map<String, MultiplierSource> sources = new LinkedHashMap<>();
    private final Set<String> disabled = new LinkedHashSet<>();
    private final Set<String> warned = new LinkedHashSet<>();
    private boolean enabled;
    private double dropCap = DEFAULT_CAP;
    private double sellCap = DEFAULT_CAP;
    private boolean clampReported;
    private Logger logger = Logger.getLogger("GalaxyMines");

    private MultiplierSources() {
    }

    public static MultiplierSources get() {
        return INSTANCE;
    }




    public synchronized void configure(final boolean enabled, final double dropCap,
                                       final double sellCap, final Collection<String> disabled,
                                       final Logger logger) {
        this.enabled = enabled;
        this.dropCap = clampCap(dropCap, DEFAULT_CAP);
        this.sellCap = clampCap(sellCap, DEFAULT_CAP);
        this.disabled.clear();
        if (disabled != null) {
            for (final String id : disabled) {
                if (id != null && !id.isBlank()) {
                    this.disabled.add(normalize(id));
                }
            }
        }
        if (logger != null) {
            this.logger = logger;
        }
        this.clampReported = false;
    }






    public synchronized boolean register(final MultiplierSource source) {
        if (source == null || source.id() == null || source.id().isBlank()) {
            return false;
        }
        this.sources.put(normalize(source.id()), source);
        this.warned.remove(normalize(source.id()));
        return true;
    }

    public synchronized boolean unregister(final String id) {
        return id != null && this.sources.remove(normalize(id)) != null;
    }



    public synchronized List<String> ids() {
        return List.copyOf(this.sources.keySet());
    }

    public synchronized double dropCap() {
        return this.dropCap;
    }




    public synchronized String summary() {
        return this.sources.isEmpty() ? "none" : String.join(", ", this.sources.keySet());
    }



    public String describe(final Player player) {
        return String.format(Locale.ROOT, "%.2fx / %.2fx", drop(player), sell(player));
    }

    public synchronized double sellCap() {
        return this.sellCap;
    }

    public synchronized boolean isEnabled() {
        return this.enabled;
    }



    public double drop(final Player player) {
        return this.combined(player, true);
    }



    public double sell(final Player player) {
        return this.combined(player, false);
    }

    private double combined(final Player player, final boolean drop) {
        final double cap;
        final List<MultiplierSource> active;
        synchronized (this) {
            if (!this.enabled || this.sources.isEmpty()) {
                return 1.0D;
            }
            cap = drop ? this.dropCap : this.sellCap;
            active = this.activeUnlocked();
        }
        double product = 1.0D;
        for (final MultiplierSource source : active) {
            final double value;
            try {
                value = drop ? source.dropMultiplier(player) : source.sellMultiplier(player);
            } catch (RuntimeException ex) {
                this.warnOnce(source, "could not be read", ex);
                continue;
            }
            if (!Double.isFinite(value) || value <= 0.0D) {
                this.warnOnce(source, "returned " + value, null);
                continue;
            }
            product *= value;
            if (!Double.isFinite(product)) {
                this.warnOnce(source, "overflowed the combined multiplier", null);
                return cap;
            }
        }
        if (product > cap) {
            this.reportClamp(cap);
            return cap;
        }
        return product;
    }

    private List<MultiplierSource> activeUnlocked() {
        final List<MultiplierSource> active = new ArrayList<>(this.sources.size());
        for (final Map.Entry<String, MultiplierSource> entry : this.sources.entrySet()) {
            if (!this.disabled.contains(entry.getKey())) {
                active.add(entry.getValue());
            }
        }
        return active;
    }

    private void warnOnce(final MultiplierSource source, final String reason, final Exception ex) {
        final String id = normalize(source.id());
        synchronized (this) {
            if (!this.warned.add(id)) {
                return;
            }
        }
        this.logger.log(Level.WARNING, "Multiplier source '" + id + "' " + reason
                + "; it is treated as 1.0 until the next reload.", ex);
    }

    private void reportClamp(final double cap) {
        synchronized (this) {
            if (this.clampReported) {
                return;
            }
            this.clampReported = true;
        }
        this.logger.info("The configured multiplier cap of " + cap + " was reached; "
                + "raise integrations.multipliers.drop-cap/sell-cap if this is unintended.");
    }

    private static double clampCap(final double value, final double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(MIN_CAP, Math.min(MAX_CAP, value));
    }

    private static String normalize(final String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }



    synchronized void resetForTesting() {
        this.sources.clear();
        this.disabled.clear();
        this.warned.clear();
        this.enabled = false;
        this.dropCap = DEFAULT_CAP;
        this.sellCap = DEFAULT_CAP;
        this.clampReported = false;
    }
}
