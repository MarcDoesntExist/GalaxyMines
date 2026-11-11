package me.marcdoesntexist.galaxymines.bomb;

import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.mines.MineDropProcessor;
import me.marcdoesntexist.galaxymines.mines.MineManager;
import me.marcdoesntexist.galaxymines.stats.PlayerStatsManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;








public final class BombService {
    private static final BombService INSTANCE = new BombService();

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private boolean enabled = true;
    private int maxRadius = 8;
    private int cooldownSeconds = 5;

    private BombService() {
    }

    public static BombService get() {
        return INSTANCE;
    }

    private static int clamp(final int value, final int minimum, final int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public synchronized void load(final ConfigurationSection config) {
        this.enabled = config == null || config.getBoolean("enabled", true);
        this.maxRadius = clamp(config == null ? 8 : config.getInt("max-radius", 8), 1, 12);
        this.cooldownSeconds = clamp(
                config == null ? 5 : config.getInt("cooldown-seconds", 5), 0, 3600);
    }

    public synchronized boolean isEnabled() {
        return this.enabled;
    }

    public int maxRadius() {
        return this.maxRadius;
    }

    public int clampRadius(final int radius) {
        return clamp(radius, 1, this.maxRadius);
    }



    public long cooldownRemaining(final Player player) {
        if (player == null || this.cooldownSeconds <= 0) {
            return 0L;
        }
        final Long until = this.cooldowns.get(player.getUniqueId());
        if (until == null) {
            return 0L;
        }
        final long remaining = until - System.currentTimeMillis();
        return remaining <= 0L ? 0L : (remaining + 999L) / 1000L;
    }




    public int explode(final Player player, final Location center, final int radius) {
        if (player == null || center == null || center.getWorld() == null) {
            return 0;
        }
        final int limit = this.clampRadius(radius);
        final Map<Mine, List<ItemStack>> drops = new LinkedHashMap<>();
        int broken = 0;
        for (final int[] offset : BombShape.offsets(limit)) {
            final Block block = center.clone().add(offset[0], offset[1], offset[2]).getBlock();
            if (block.getType().isAir()) {
                continue;
            }
            final Mine mine = MineManager.getMineAt(block);
            if (mine == null || mine.isResetInProgress()) {
                continue;
            }
            final List<ItemStack> captured = MineDropProcessor.capture(block, player);
            if (!MineDropProcessor.canAccept(player, mine, captured)) {
                continue;
            }
            block.setBlockData(Material.AIR.createBlockData(), false);
            mine.setBlockAsBroken(block);
            PlayerStatsManager.addMined(player, mine);
            drops.computeIfAbsent(mine, ignored -> new ArrayList<>()).addAll(captured);
            ++broken;
        }
        for (final Map.Entry<Mine, List<ItemStack>> entry : drops.entrySet()) {
            MineDropProcessor.deliver(player, entry.getKey(), center, entry.getValue());
        }
        center.getWorld().createExplosion(center, 0.0F, false, false);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
        if (this.cooldownSeconds > 0) {
            this.cooldowns.put(player.getUniqueId(),
                    System.currentTimeMillis() + this.cooldownSeconds * 1000L);
        }
        return broken;
    }

    public void clearCooldowns() {
        this.cooldowns.clear();
    }
}
