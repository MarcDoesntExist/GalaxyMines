package me.marcdoesntexist.galaxymines.mines;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.messages.Messages;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;





public final class MineWandManager implements Listener {
    private final NamespacedKey key;
    private final Map<UUID, Selection> selections = new HashMap<>();
    private BukkitTask previewTask;

    public MineWandManager() {
        this.key = new NamespacedKey(GalaxyMines.getInstance(), "mine_wand");
    }

    private static void spawn(final Player player, final org.bukkit.World world,
                              final int x, final int y, final int z) {
        final Location location = new Location(world, x + 0.5D, y + 0.5D, z + 0.5D);
        if (location.distanceSquared(player.getLocation()) > 4096.0D) {
            return;
        }
        player.spawnParticle(Particle.END_ROD, location, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public void start() {
        stop();
        this.previewTask = Bukkit.getScheduler().runTaskTimer(GalaxyMines.getInstance(),
                this::drawPreviews, 10L, 10L);
    }

    public void stop() {
        if (this.previewTask != null) {
            this.previewTask.cancel();
            this.previewTask = null;
        }
        this.selections.clear();
    }

    public int selectionCount() {
        return this.selections.size();
    }

    public void give(final Player player) {
        final ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        final ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.displayName(me.marcdoesntexist.galaxymines.api.chat.Text.parse(
                    Messages.get("messages.wand.itemName", "<gold>Mine selection wand")));
            meta.lore(java.util.List.of(me.marcdoesntexist.galaxymines.api.chat.Text.parse(
                            Messages.get("messages.wand.itemLore1",
                                    "<gray>Left click: first corner")),
                    me.marcdoesntexist.galaxymines.api.chat.Text.parse(
                            Messages.get("messages.wand.itemLore2",
                                    "<gray>Right click: second corner"))));
            meta.getPersistentDataContainer().set(this.key, PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }
        final Map<Integer, ItemStack> leftover = player.getInventory().addItem(wand);
        for (final ItemStack stack : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
        player.sendMessage(me.marcdoesntexist.galaxymines.api.chat.Text.parse(
                Messages.get("messages.wand.given",
                        "Selection wand added. Left click and right click two corners.")));
    }

    public boolean isWand(final ItemStack stack) {
        if (stack == null || stack.getType() != Material.BLAZE_ROD || !stack.hasItemMeta()) {
            return false;
        }
        final ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer()
                .has(this.key, PersistentDataType.BYTE);
    }

    public CuboidRegion selection(final Player player) {
        final Selection selection = this.selections.get(player.getUniqueId());
        if (selection == null || selection.first == null || selection.second == null) {
            return null;
        }
        return new CuboidRegion(selection.first, selection.second);
    }

    public void clear(final Player player) {
        this.selections.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isWand(event.getItem())) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedBlock() == null) {
            return;
        }
        createSelection(event.getPlayer()).set(event.getAction(), event.getClickedBlock()
                .getX(), event.getClickedBlock().getY(), event.getClickedBlock().getZ());
        final Selection selection = this.selections.get(event.getPlayer().getUniqueId());
        event.getPlayer().sendMessage(me.marcdoesntexist.galaxymines.api.chat.Text.parse(
                Messages.get("messages.wand.position", "Corner %corner% set to %x%, %y%, %z%.",
                        "%corner%", event.getAction() == Action.LEFT_CLICK_BLOCK ? "1" : "2",
                        "%x%", String.valueOf(event.getClickedBlock().getX()),
                        "%y%", String.valueOf(event.getClickedBlock().getY()),
                        "%z%", String.valueOf(event.getClickedBlock().getZ()))));
        if (selection != null && selection.first != null && selection.second != null) {
            event.getPlayer().sendMessage(me.marcdoesntexist.galaxymines.api.chat.Text.parse(
                    Messages.get("messages.wand.ready",
                            "Selection ready. Use /mine create <name>.")));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(final BlockBreakEvent event) {
        if (isWand(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        this.selections.remove(event.getPlayer().getUniqueId());
    }

    private Selection createSelection(final Player player) {
        return this.selections.computeIfAbsent(player.getUniqueId(),
                ignored -> new Selection());
    }

    private void drawPreviews() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final Selection selection = this.selections.get(player.getUniqueId());
            if (selection == null || selection.first == null || selection.second == null
                    || !isWand(player.getInventory().getItemInMainHand())) {
                continue;
            }
            drawOutline(player, selection);
        }
    }

    private void drawOutline(final Player player, final Selection selection) {
        final int minX = Math.min(selection.first.x(), selection.second.x());
        final int minY = Math.min(selection.first.y(), selection.second.y());
        final int minZ = Math.min(selection.first.z(), selection.second.z());
        final int maxX = Math.max(selection.first.x(), selection.second.x());
        final int maxY = Math.max(selection.first.y(), selection.second.y());
        final int maxZ = Math.max(selection.first.z(), selection.second.z());
        final int step = Math.max(1, Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ)
                / 40);
        final org.bukkit.World world = player.getWorld();
        for (int x = minX; x <= maxX; x += step) {
            spawn(player, world, x, minY, minZ);
            spawn(player, world, x, minY, maxZ);
            spawn(player, world, x, maxY, minZ);
            spawn(player, world, x, maxY, maxZ);
        }
        for (int y = minY; y <= maxY; y += step) {
            spawn(player, world, minX, y, minZ);
            spawn(player, world, minX, y, maxZ);
            spawn(player, world, maxX, y, minZ);
            spawn(player, world, maxX, y, maxZ);
        }
        for (int z = minZ; z <= maxZ; z += step) {
            spawn(player, world, minX, minY, z);
            spawn(player, world, maxX, minY, z);
            spawn(player, world, minX, maxY, z);
            spawn(player, world, maxX, maxY, z);
        }
    }

    private static final class Selection {
        private BlockVector3 first;
        private BlockVector3 second;

        private void set(final Action action, final int x, final int y, final int z) {
            if (action == Action.LEFT_CLICK_BLOCK) {
                this.first = BlockVector3.at(x, y, z);
            } else {
                this.second = BlockVector3.at(x, y, z);
            }
        }
    }
}
