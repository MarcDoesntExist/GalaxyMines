package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.events.mine.MineBlockBreakEvent;
import me.marcdoesntexist.galaxymines.luckyblocks.LuckyBlockManager;
import me.marcdoesntexist.galaxymines.stats.PlayerStatsManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.*;

import java.util.Iterator;
import java.util.List;


public final class MineListener implements Listener {
    private final BreakRateGuard breakGuard = new BreakRateGuard();

    private static boolean pistonMovesDeniedBlock(final List<Block> blocks,
                                                  final BlockFace direction) {
        if (blocks == null || direction == null) {
            return false;
        }
        for (final Block block : blocks) {
            if (placeDenied(block.getRelative(direction), block.getType())) {
                return true;
            }
        }
        return false;
    }


    private static boolean placeDenied(final Block destination, final Material material) {
        if (destination == null || material == null || material.isAir()) {
            return false;
        }
        final Mine mine = MineManager.getMineAt(destination);
        if (mine == null) {
            return false;
        }
        final MineConstraints constraints = mine.getBlockConstraints();
        return constraints != null && constraints.isPlaceEnabled()
                && constraints.deniesPlace(material);
    }

    private static boolean mark(final Mine mine, final Block block, final Player source) {
        if (mine == null || mine.isResetInProgress()) {
            return false;
        }
        final int before = mine.getMinedBlocks();
        mine.setBlockAsBroken(block);

        if (source != null && mine.getMinedBlocks() > before) {
            PlayerStatsManager.addMined(source, mine);
        }
        return mine.getMinedBlocks() > before;
    }

    private static int countOutsideMine(final Block source, final List<Block> blocks) {
        int outside = MineManager.getMineAt(source) == null ? 1 : 0;
        for (final Block block : blocks) {
            if (MineManager.getMineAt(block) == null) {
                ++outside;
            }
        }
        return outside;
    }

    private static void logExplosion(final String source, final Location location,
                                     final int reported, final int counted, final int outside) {
        final GalaxyMines plugin = GalaxyMines.getInstance();
        if (plugin == null || location == null) {
            return;
        }
        final String world = location.getWorld() == null
                ? "unknown" : location.getWorld().getName();
        plugin.debug("Explosion " + source + " at " + world + " "
                + location.getBlockX() + "," + location.getBlockY() + ","
                + location.getBlockZ() + ": reported=" + reported
                + ", counted=" + counted + ", outsideMine=" + outside, 3);
    }


    private static boolean explosionAllowed(final Mine mine, final Block block) {
        if (mine == null) {
            return true;
        }
        final MineConstraints constraints = mine.getBlockConstraints();
        if (constraints != null && constraints.deniesBreak(block.getType())) {
            return false;
        }
        return !mine.isCompositionOnly() || mine.containsMaterial(block.getType());
    }

    private static boolean movedBlock(final Location from, final Location to) {
        if (to == null) {
            return false;
        }
        return from.getWorld() != to.getWorld()
                || from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    private static int antiClickerLimit() {
        try {
            return GalaxyMines.getInstance().getConfig()
                    .getInt("anti-clicker.max-breaks-per-second", 0);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String antiClickerMessage() {
        try {
            return GalaxyMines.getInstance().getConfig().getString("anti-clicker.message",
                    "<red>Slow down: that is faster than a human can mine.</red>");
        } catch (Throwable ignored) {
            return "<red>Slow down: that is faster than a human can mine.</red>";
        }
    }

    private boolean allowBreakRate(final Player player) {
        final int limit = antiClickerLimit();
        if (limit <= 0) {
            return true;
        }
        final long now = System.currentTimeMillis();
        if (breakGuard.allow(player.getUniqueId(), now, limit)) {
            return true;
        }
        if (breakGuard.shouldWarn(player.getUniqueId(), now)) {
            final String message = antiClickerMessage();
            if (!message.isBlank()) {
                try {
                    player.sendMessage(Text.parse(message));
                } catch (Throwable ignored) {

                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Player player = event.getPlayer();
        final Mine mine = MineManager.getMineAt(block);
        if (mine == null) {
            if (PluginSettings.get().protection.cancelBlockBreakOutsideMines
                    && MineIndex.hasMinesInWorld(block.getWorld())) {
                event.setCancelled(true);
                MineAccess.denyOutside(player);
            }
            return;
        }
        if (!MineAccess.canBreak(player, mine)) {
            event.setCancelled(true);
            MineAccess.denyBreak(player, mine);
            return;
        }
        if (!allowBreakRate(player)) {
            event.setCancelled(true);
            return;
        }
        if (mine.isCompositionOnly() && !mine.containsBlock(block)) {
            event.setCancelled(true);
            MineAccess.denyComposition(player);
            return;
        }
        final MineConstraints constraints = mine.getBlockConstraints();
        if (constraints != null && constraints.deniesBreak(block.getType())) {
            event.setCancelled(true);
            MineAccess.denyConstraintBreak(player);
            return;
        }
        final MineBlockBreakEvent mineEvent = new MineBlockBreakEvent(mine, block, player, event);
        mineEvent.getDrops().addAll(MineDropProcessor.capture(block, player));
        mineEvent.call();
        if (event.isCancelled()) {
            return;
        }
        final LuckyBlockManager luckyBlocks = LuckyBlockManager.get();
        final boolean luckyBlock = luckyBlocks.isLuckyBlock(mine, block);
        if (luckyBlock && luckyBlocks.replacesDrops(mine, block)) {

            mineEvent.setDrops(List.of());
        }
        final boolean hasDropListeners =
                MineBlockBreakEvent.getHandlerList().getRegisteredListeners().length > 0;
        final boolean wouldCustomHandle = mine.getDrops().requiresCustomHandling()
                || hasDropListeners || mineEvent.isDropsModified();

        final boolean customHandling = wouldCustomHandle
                && mine.customBlockToken(block) == null;
        if (customHandling
                && !MineDropProcessor.canAccept(player, mine, mineEvent.getDrops())) {
            event.setCancelled(true);
            player.sendMessage(Text.parse(MineDropProcessor.fullInventoryMessage()));
            return;
        }


        if (luckyBlock) {
            luckyBlocks.trigger(player, mine, block);
        }
        if (!customHandling) {
            mine.setBlockAsBroken(block);
            PlayerStatsManager.addMined(player, mine);
            return;
        }
        final int experience = event.getExpToDrop();
        final Location dropLocation = block.getLocation().clone().add(0.5D, 0.5D, 0.5D);


        event.setCancelled(true);
        event.setExpToDrop(0);
        block.setBlockData(Material.AIR.createBlockData(), false);
        if (experience > 0) {
            block.getWorld().spawn(dropLocation, ExperienceOrb.class,
                    orb -> orb.setExperience(experience));
        }
        MineDropProcessor.deliver(player, mine, dropLocation, mineEvent.getDrops());
        mine.setBlockAsBroken(block);
        PlayerStatsManager.addMined(player, mine);
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Block block = event.getBlockPlaced();
        final Mine mine = MineManager.getMineAt(block);
        if (mine == null) {
            return;
        }

        final MineConstraints constraints = mine.getBlockConstraints();
        if (constraints == null || !constraints.isPlaceEnabled()) {
            return;
        }
        if (!constraints.deniesPlace(block.getType())) {
            return;
        }
        event.setCancelled(true);
        MineAccess.denyConstraintPlace(event.getPlayer());
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(final BlockPistonExtendEvent event) {
        if (pistonMovesDeniedBlock(event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(final BlockPistonRetractEvent event) {
        if (pistonMovesDeniedBlock(event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallingBlockLand(final EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) {
            return;
        }
        if (placeDenied(event.getBlock(), event.getTo())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        final Block source = event.getBlock();
        final int reported = event.blockList().size() + 1;
        if (!explosionAllowed(MineManager.getMineAt(source), source)) {
            event.setCancelled(true);
            logExplosion("block " + source.getType(), source.getLocation(), reported, 0,
                    countOutsideMine(source, event.blockList()));
            return;
        }
        int counted = 0;
        int outside = 0;
        for (final Iterator<Block> iterator = event.blockList().iterator(); iterator.hasNext(); ) {
            final Block block = iterator.next();

            final Mine mine = MineManager.getMineAt(block);
            if (mine == null) {
                ++outside;
            }
            if (!explosionAllowed(mine, block)) {
                iterator.remove();
                continue;
            }
            if (mark(mine, block, null)) {
                ++counted;
            }
        }
        final Mine sourceMine = MineManager.getMineAt(source);
        if (sourceMine == null) {
            ++outside;
        }
        if (mark(sourceMine, source, null)) {
            ++counted;
        }
        logExplosion("block " + source.getType(), source.getLocation(), reported, counted, outside);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        final Player source = MineExplosions.sourcePlayer(event.getEntity());
        final int reported = event.blockList().size();
        int counted = 0;
        int outside = 0;
        for (final Iterator<Block> iterator = event.blockList().iterator(); iterator.hasNext(); ) {
            final Block block = iterator.next();
            final Mine mine = MineManager.getMineAt(block);
            if (mine == null) {
                ++outside;
            }
            if (!explosionAllowed(mine, block)) {
                iterator.remove();
                continue;
            }
            if (mark(mine, block, source)) {
                ++counted;
            }
        }
        final String type = event.getEntity() == null
                ? "unknown" : event.getEntity().getType().name();
        logExplosion("entity " + type, event.getLocation(), reported, counted, outside);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        TeleportService.handleMove(event.getPlayer(), event.getFrom(), event.getTo());
        if (!movedBlock(event.getFrom(), event.getTo())) {
            return;
        }
        MineTracker.update(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {

        org.bukkit.Bukkit.getScheduler().runTaskLater(
                me.marcdoesntexist.galaxymines.GalaxyMines.getInstance(),
                () -> MineTracker.refresh(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                me.marcdoesntexist.galaxymines.GalaxyMines.getInstance(),
                () -> MineTracker.refresh(event.getPlayer()), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        if (event.getTo() != null) {
            MineTracker.update(event.getPlayer(), event.getTo());
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        MineTracker.clear(event.getPlayer());
        MineAccess.clear(event.getPlayer());
        TeleportService.clear(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        MineTracker.clear(event.getPlayer());
    }
}
