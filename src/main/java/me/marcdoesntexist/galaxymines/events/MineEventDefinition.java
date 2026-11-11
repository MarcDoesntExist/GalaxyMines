package me.marcdoesntexist.galaxymines.events;

import org.bukkit.Material;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public record MineEventDefinition(
        String id,
        String displayName,
        int durationSeconds,
        List<LocalTime> times,
        List<String> mines,
        boolean autoStart,
        boolean broadcast,
        String startMessage,
        String stopMessage,
        List<String> startCommands,
        List<String> stopCommands,
        EventActions actions) {

    public MineEventDefinition {
        id = Objects.requireNonNull(id, "id").trim().toLowerCase(Locale.ROOT);
        displayName = displayName == null || displayName.isBlank() ? id : displayName.trim();
        final List<String> normalizedMines = new ArrayList<>();
        if (mines != null) {
            for (final String mine : mines) {
                if (mine != null && !mine.isBlank()) {
                    normalizedMines.add(mine.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        mines = List.copyOf(normalizedMines);
        times = times == null ? List.of() : List.copyOf(times);
        startCommands = startCommands == null ? List.of() : List.copyOf(startCommands);
        stopCommands = stopCommands == null ? List.of() : List.copyOf(stopCommands);
        actions = actions == null ? EventActions.NONE : actions;
    }

    public boolean appliesTo(final String mineName) {
        if (this.mines.isEmpty()) {
            return true;
        }
        return mineName != null
                && this.mines.contains(mineName.toLowerCase(Locale.ROOT));
    }


    public record EventActions(double dropMultiplier, double sellMultiplier,
                               List<RareDrop> rareDrops) {
        public static final EventActions NONE = new EventActions(1.0D, 1.0D, List.of());

        public EventActions {
            dropMultiplier = Double.isFinite(dropMultiplier)
                    ? Math.max(1.0D, dropMultiplier) : 1.0D;
            sellMultiplier = Double.isFinite(sellMultiplier)
                    ? Math.max(1.0D, sellMultiplier) : 1.0D;
            rareDrops = rareDrops == null ? List.of() : List.copyOf(rareDrops);
        }
    }

    public record RareDrop(Material material, String providerId, double chance, int amount) {


        public RareDrop(final Material material, final double chance, final int amount) {
            this(material, null, chance, amount);
        }

        public RareDrop {
            providerId = providerId == null || providerId.isBlank()
                    ? null : providerId.trim().toLowerCase(Locale.ROOT);
            if (providerId == null) {
                Objects.requireNonNull(material, "material");
            }
            chance = Double.isFinite(chance)
                    ? Math.max(0.0D, Math.min(100.0D, chance)) : 0.0D;
            amount = Math.max(1, amount);
        }
    }
}
