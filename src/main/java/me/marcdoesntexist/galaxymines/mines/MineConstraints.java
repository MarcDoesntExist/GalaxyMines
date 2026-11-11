package me.marcdoesntexist.galaxymines.mines;

import org.bukkit.Material;

import java.util.*;


public final class MineConstraints {


    public static final MineConstraints NONE = new MineConstraints(
            Set.of(), Set.of(), false, Set.of(), Set.of());

    private final Set<Material> breakAllowed;
    private final Set<Material> breakDenied;
    private final boolean placeEnabled;
    private final Set<Material> placeAllowed;
    private final Set<Material> placeDenied;

    public MineConstraints(final Collection<Material> breakAllowed,
                           final Collection<Material> breakDenied,
                           final boolean placeEnabled,
                           final Collection<Material> placeAllowed,
                           final Collection<Material> placeDenied) {
        this.breakAllowed = immutable(breakAllowed);
        this.breakDenied = immutable(breakDenied);
        this.placeEnabled = placeEnabled;
        this.placeAllowed = immutable(placeAllowed);
        this.placeDenied = immutable(placeDenied);
    }


    public static String format(final Collection<Material> materials) {
        if (materials == null || materials.isEmpty()) {
            return "-";
        }
        final StringBuilder builder = new StringBuilder();
        for (final Material material : materials) {
            if (material == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(material.name().toLowerCase(Locale.ROOT));
        }
        return builder.length() == 0 ? "-" : builder.toString();
    }

    private static Set<Material> immutable(final Collection<Material> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        final EnumSet<Material> copy = EnumSet.noneOf(Material.class);
        for (final Material material : source) {
            if (material != null) {
                copy.add(material);
            }
        }
        return copy.isEmpty() ? Set.of() : Collections.unmodifiableSet(copy);
    }


    public boolean deniesBreak(final Material material) {
        if (material == null) {
            return false;
        }
        if (this.breakDenied.contains(material)) {
            return true;
        }
        return !this.breakAllowed.isEmpty() && !this.breakAllowed.contains(material);
    }


    public boolean deniesPlace(final Material material) {
        if (!this.placeEnabled || material == null) {
            return false;
        }
        if (this.placeDenied.contains(material)) {
            return true;
        }
        return !this.placeAllowed.isEmpty() && !this.placeAllowed.contains(material);
    }

    public boolean restrictsBreak() {
        return !this.breakAllowed.isEmpty() || !this.breakDenied.isEmpty();
    }

    public boolean isPlaceEnabled() {
        return this.placeEnabled;
    }

    public Set<Material> breakAllowed() {
        return this.breakAllowed;
    }

    public Set<Material> breakDenied() {
        return this.breakDenied;
    }

    public Set<Material> placeAllowed() {
        return this.placeAllowed;
    }

    public Set<Material> placeDenied() {
        return this.placeDenied;
    }
}
