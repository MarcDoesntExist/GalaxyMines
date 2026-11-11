package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.api.CustomPlugin;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.messages.Messages;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class MineAccess {
    private static final Map<DenyKey, Long> DENY_COOLDOWN = new ConcurrentHashMap<>();

    private MineAccess() {
    }

    public static void register(final Mine mine) {
        final String name = normalize(mine.getName());
        CustomPlugin.registerPermission("galaxymines.mine." + name, PermissionDefault.FALSE);
        CustomPlugin.registerPermission("galaxymines.mine." + name + ".break",
                PermissionDefault.FALSE);
    }

    public static void unregister(final String mineName) {
        final String name = normalize(mineName);
        org.bukkit.Bukkit.getPluginManager().removePermission("galaxymines.mine." + name);
        org.bukkit.Bukkit.getPluginManager().removePermission(
                "galaxymines.mine." + name + ".break");
    }

    public static boolean canAccess(final Player player, final Mine mine) {
        if (isBypassing(player)) {
            return true;
        }
        if (mine.requiresAccessPermission()
                && !player.hasPermission("galaxymines.mine." + normalize(mine.getName()))
                && !player.hasPermission("galaxymines.mine.*")) {
            return false;
        }
        if (!mine.getRequiredPermission().isBlank()
                && !player.hasPermission(mine.getRequiredPermission())) {
            return false;
        }
        if (!mine.getRequiredRank().isBlank()) {
            final String rank = mine.getRequiredRank();
            final String permission = rank.contains(".")
                    ? rank
                    : "galaxymines.rank." + rank.toLowerCase(Locale.ROOT);
            if (!player.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canBreak(final Player player, final Mine mine) {
        if (isBypassing(player)) {
            return true;
        }
        if (!canAccess(player, mine)) {
            return false;
        }
        if (mine.requiresBreakPermission()) {
            final String name = normalize(mine.getName());
            return player.hasPermission("galaxymines.mine." + name + ".break")
                    || player.hasPermission("galaxymines.mine.*.break")
                    || player.hasPermission("galaxymines.mine.*");
        }
        return true;
    }

    public static void denyAccess(final Player player, final Mine mine) {
        if (!shouldNotify(player, Denial.ACCESS)) {
            return;
        }
        player.sendMessage(Text.parse(Messages.get("messages.mine.accessDenied",
                "You do not have access to this mine.", "%mine%", mine.getName())));
        final Vector direction = player.getLocation().toVector()
                .subtract(mine.getCenter().toVector()).setY(0.0D);
        if (direction.lengthSquared() < 0.01D) {
            direction.setX(1.0D);
        }
        direction.normalize().multiply(1.1D).setY(0.35D);
        player.setVelocity(direction);
    }

    public static void denyBreak(final Player player, final Mine mine) {
        if (!shouldNotify(player, Denial.BREAK)) {
            return;
        }
        player.sendMessage(Text.parse(Messages.get("messages.mine.breakDenied",
                "You cannot break blocks in this mine.", "%mine%", mine.getName())));
    }

    public static void denyComposition(final Player player) {
        if (!shouldNotify(player, Denial.COMPOSITION)) {
            return;
        }
        player.sendMessage(Text.parse(Messages.get("messages.mine.compositionProtected",
                "Only blocks from this mine's composition can be broken here.")));
    }

    public static void denyConstraintBreak(final Player player) {
        if (!shouldNotify(player, Denial.CONSTRAINT_BREAK)) {
            return;
        }
        player.sendMessage(Text.parse(Messages.get("messages.mine.constraintBreakDenied",
                "This block cannot be broken in this mine.")));
    }

    public static void denyConstraintPlace(final Player player) {
        if (!shouldNotify(player, Denial.CONSTRAINT_PLACE)) {
            return;
        }
        player.sendMessage(Text.parse(Messages.get("messages.mine.constraintPlaceDenied",
                "You cannot place this block in this mine.")));
    }

    public static void denyOutside(final Player player) {
        if (!shouldNotify(player, Denial.OUTSIDE)) {
            return;
        }
        player.sendMessage(Text.parse(Messages.get("messages.mine.outsideProtected",
                "You cannot break blocks in this managed world outside a mine.")));
    }

    public static void clear(final Player player) {
        final UUID uuid = player.getUniqueId();
        DENY_COOLDOWN.keySet().removeIf(key -> key.player().equals(uuid));
    }

    private static boolean isBypassing(final Player player) {
        return player.hasPermission("galaxymines.admin")
                || player.hasPermission("galaxymines.bypass");
    }

    private static boolean shouldNotify(final Player player, final Denial denial) {
        final int cooldown = Math.max(0, me.marcdoesntexist.galaxymines.config.PluginSettings.get()
                .protection.denyCooldownSeconds);
        if (cooldown == 0) {
            return true;
        }
        final long now = System.currentTimeMillis();
        final DenyKey key = new DenyKey(player.getUniqueId(), denial);
        final Long next = DENY_COOLDOWN.get(key);
        if (next != null && next > now) {
            return false;
        }
        DENY_COOLDOWN.put(key, now + cooldown * 1000L);
        return true;
    }

    private static String normalize(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }


    private enum Denial {
        ACCESS, BREAK, COMPOSITION, CONSTRAINT_BREAK, CONSTRAINT_PLACE, OUTSIDE
    }

    private record DenyKey(UUID player, Denial denial) {
    }
}
