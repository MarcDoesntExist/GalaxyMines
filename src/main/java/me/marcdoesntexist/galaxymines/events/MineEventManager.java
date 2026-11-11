package me.marcdoesntexist.galaxymines.events;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.events.MineEventDefinition.RareDrop;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockRegistry;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.utils.ScheduleTimes;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;


public final class MineEventManager {
    private static final MineEventManager INSTANCE = new MineEventManager();

    private final Map<String, MineEventDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, Long> activeUntil = new LinkedHashMap<>();
    private BukkitTask task;
    private long lastScheduleMinute = -1L;

    private MineEventManager() {
    }

    public static MineEventManager get() {
        return INSTANCE;
    }


    static ItemStack rareDropStack(final RareDrop rare) {
        if (rare.providerId() != null) {
            final ItemStack resolved = CustomBlockRegistry.item(rare.providerId());
            if (resolved == null || resolved.getType().isAir()) {
                if (GalaxyMines.getInstance() != null) {
                    GalaxyMines.getInstance().debug("Rare drop '" + rare.providerId()
                            + "' could not be resolved; skipping it.", true);
                }
                return null;
            }
            final ItemStack copy = resolved.clone();
            copy.setAmount(rare.amount());
            return copy;
        }
        return new ItemStack(rare.material(), rare.amount());
    }


    private static String prepareMessage(final MineEventDefinition definition,
                                         final String message) {
        return message
                .replace("%event%", Text.escape(definition.displayName()))
                .replace("%id%", Text.escape(definition.id()))
                .replace("%duration%", String.valueOf(definition.durationSeconds()));
    }

    private static Map<String, String> placeholders(final MineEventDefinition definition) {
        return Map.of(
                "event", definition.displayName(),
                "id", definition.id(),
                "duration", String.valueOf(definition.durationSeconds()));
    }

    private static String prepareCommand(final MineEventDefinition definition,
                                         final String command) {
        return command
                .replace("%event%", definition.displayName())
                .replace("%id%", definition.id())
                .replace("%duration%", String.valueOf(definition.durationSeconds()))
                .replace("<event>", definition.displayName())
                .replace("<id>", definition.id())
                .replace("<duration>", String.valueOf(definition.durationSeconds()))
                .trim();
    }

    public void load(final File file, final boolean enabled) {
        stop();
        this.definitions.clear();
        if (!enabled || file == null || !file.isFile()) {
            return;
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        final ConfigurationSection section = config.getConfigurationSection("events");
        if (section == null) {
            return;
        }
        final Map<String, MineEventDefinition> parsed = new LinkedHashMap<>();
        for (final String id : section.getKeys(false)) {
            final ConfigurationSection event = section.getConfigurationSection(id);
            if (event == null) {
                continue;
            }
            final MineEventDefinition definition = MineEventParser.parse(id, event, message -> {
                if (GalaxyMines.getInstance() != null) {
                    GalaxyMines.getInstance().debug(message, true);
                }
            });
            if (definition != null) {
                parsed.put(definition.id(), definition);
            }
        }
        applyDefinitions(parsed);
        for (final MineEventDefinition definition : this.definitions.values()) {
            if (definition.autoStart()) {
                start(definition.id());
            }
        }
        this.task = Bukkit.getScheduler().runTaskTimer(GalaxyMines.getInstance(),
                this::tick, 20L, 20L);
    }


    public void applyDefinitions(final Map<String, MineEventDefinition> definitions) {
        stop();
        this.definitions.clear();
        if (definitions != null) {
            this.definitions.putAll(definitions);
        }
    }

    private void tick() {
        final LocalTime now = LocalTime.now();
        expire(System.currentTimeMillis());
        final long minute = now.getHour() * 60L + now.getMinute();
        if (minute == this.lastScheduleMinute) {
            return;
        }
        this.lastScheduleMinute = minute;
        for (final MineEventDefinition definition : this.definitions.values()) {
            if (ScheduleTimes.matches(definition.times(), now)) {
                start(definition.id());
            }
        }
    }


    void expire(final long nowMillis) {
        final List<String> expired = new ArrayList<>();
        for (final Map.Entry<String, Long> entry : this.activeUntil.entrySet()) {
            if (entry.getValue() <= nowMillis) {
                expired.add(entry.getKey());
            }
        }
        for (final String id : expired) {
            this.activeUntil.remove(id);
            final MineEventDefinition definition = this.definitions.get(id);
            if (definition != null) {
                notifyEvent(definition, "ended", definition.stopMessage(),
                        definition.stopCommands());
            }
        }
    }

    public boolean start(final String id) {
        final MineEventDefinition definition = this.definition(id);
        if (definition == null) {
            return false;
        }
        final boolean alreadyActive = isActive(definition.id());
        this.activeUntil.put(definition.id(),
                System.currentTimeMillis() + definition.durationSeconds() * 1000L);
        if (!alreadyActive) {
            notifyEvent(definition, "started", definition.startMessage(),
                    definition.startCommands());
        }
        return true;
    }

    public boolean stop(final String id) {
        if (id == null) {
            return false;
        }
        final String key = id.toLowerCase(Locale.ROOT);
        final Long until = this.activeUntil.remove(key);
        if (until == null) {
            return false;
        }
        final MineEventDefinition definition = this.definitions.get(key);
        if (definition != null && until > System.currentTimeMillis()) {
            notifyEvent(definition, "stopped", definition.stopMessage(),
                    definition.stopCommands());
        }
        return true;
    }

    public boolean isActive(final String id) {
        if (id == null) {
            return false;
        }
        final Long until = this.activeUntil.get(id.toLowerCase(Locale.ROOT));
        return until != null && until > System.currentTimeMillis();
    }

    public List<String> activeEvents() {
        final List<String> active = new ArrayList<>();
        final long now = System.currentTimeMillis();
        for (final Map.Entry<String, Long> entry : this.activeUntil.entrySet()) {
            if (entry.getValue() > now) {
                active.add(entry.getKey());
            }
        }
        return active;
    }

    public List<String> knownEvents() {
        return List.copyOf(this.definitions.keySet());
    }


    public Map<String, MineEventDefinition> definitions() {
        return Map.copyOf(this.definitions);
    }


    public List<String> activeDisplayNames() {
        final List<String> names = new ArrayList<>();
        for (final String id : activeEvents()) {
            final MineEventDefinition definition = this.definitions.get(id);
            names.add(definition == null ? id : definition.displayName());
        }
        return names;
    }


    public int remainingSeconds(final String id) {
        if (id == null) {
            return 0;
        }
        final Long until = this.activeUntil.get(id.toLowerCase(Locale.ROOT));
        if (until == null) {
            return 0;
        }
        final long remaining = until - System.currentTimeMillis();
        return remaining <= 0L ? 0 : (int) Math.ceil(remaining / 1000.0D);
    }


    public int secondsUntilNextExpiry() {
        final long now = System.currentTimeMillis();
        long soonest = Long.MAX_VALUE;
        for (final Long until : this.activeUntil.values()) {
            if (until > now && until < soonest) {
                soonest = until;
            }
        }
        if (soonest == Long.MAX_VALUE) {
            return 0;
        }
        return (int) Math.ceil((soonest - now) / 1000.0D);
    }

    public int knownEventCount() {
        return this.definitions.size();
    }


    public MineEventDefinition definition(final String id) {
        return id == null ? null : this.definitions.get(id.toLowerCase(Locale.ROOT));
    }

    public double dropMultiplier(final Mine mine) {
        return multiplier(mine, definition -> definition.actions().dropMultiplier());
    }

    public double sellMultiplier(final Mine mine) {
        return multiplier(mine, definition -> definition.actions().sellMultiplier());
    }


    public List<ItemStack> rareDrops(final Mine mine) {
        final List<ItemStack> drops = new ArrayList<>();
        if (this.definitions.isEmpty()) {
            return drops;
        }
        for (final MineEventDefinition definition : this.definitions.values()) {
            if (!isActive(definition.id()) || !appliesTo(definition, mine)) {
                continue;
            }
            for (final RareDrop rare : definition.actions().rareDrops()) {
                if (ThreadLocalRandom.current().nextDouble(100.0D) >= rare.chance()) {
                    continue;
                }
                final ItemStack item = rareDropStack(rare);
                if (item != null) {
                    drops.add(item);
                }
            }
        }
        return drops;
    }


    public ItemStack rareDrop(final Mine mine) {
        final List<ItemStack> drops = rareDrops(mine);
        return drops.isEmpty() ? null : drops.get(0);
    }

    private double multiplier(final Mine mine,
                              final ToDoubleFunction<MineEventDefinition> selector) {
        double multiplier = 1.0D;
        for (final MineEventDefinition definition : this.definitions.values()) {
            if (isActive(definition.id()) && appliesTo(definition, mine)) {
                multiplier = Math.max(multiplier, selector.applyAsDouble(definition));
            }
        }
        return multiplier;
    }

    private boolean appliesTo(final MineEventDefinition definition, final Mine mine) {
        return definition.mines().isEmpty()
                || (mine != null && definition.appliesTo(mine.getName()));
    }

    private void notifyEvent(final MineEventDefinition definition, final String state,
                             final String message, final List<String> commands) {
        if (definition.broadcast() && message != null && !message.isBlank()) {
            final Component component = Text.parse(prepareMessage(definition, message),
                    placeholders(definition));
            for (final Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(component);
            }
        }
        for (final String raw : commands) {
            String command = prepareCommand(definition, raw);
            if (command.isBlank()) {
                continue;
            }
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (RuntimeException ex) {
                if (GalaxyMines.getInstance() != null) {
                    GalaxyMines.getInstance().debug("Event command failed for '"
                            + definition.id() + "': " + ex.getMessage(), true);
                }
            }
        }
        if (GalaxyMines.getInstance() != null) {
            GalaxyMines.getInstance().debug(
                    "Event '" + definition.id() + "' " + state, 2);
        }
    }


    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        this.activeUntil.clear();
        this.lastScheduleMinute = -1L;
    }
}
