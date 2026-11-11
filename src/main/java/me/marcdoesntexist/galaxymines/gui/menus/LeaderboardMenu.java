package me.marcdoesntexist.galaxymines.gui.menus;

import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicClickableInventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.stats.LeaderboardEntry;
import me.marcdoesntexist.galaxymines.stats.PlayerStatsManager;
import me.marcdoesntexist.galaxymines.stats.StatsPeriod;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;






public final class LeaderboardMenu extends BasicInventoryMenu {
    private static final int[] PERIOD_SLOTS = {0, 1, 2};
    private static final int CLOSE_SLOT = 8;
    private static final int[] ENTRY_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44,
            45, 46, 47, 48, 49, 50, 51, 52, 53
    };

    private StatsPeriod period;

    public LeaderboardMenu(final StatsPeriod period) {
        super(Messages.get("messages.leaderboard.title", "<dark_aqua>Leaderboard"), 6);
        this.period = period == null ? StatsPeriod.ALL_TIME : period;
        this.initialSetup();
    }



    public static List<String> describe(final StatsPeriod period, final List<LeaderboardEntry> entries) {
        final java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        for (int index = 0; index < entries.size(); ++index) {
            final LeaderboardEntry entry = entries.get(index);
            lines.add(Messages.get("messages.leaderboard.chatEntry",
                    "%rank%. %name% - %blocks% blocks, %earned% earned",
                    "%rank%", String.valueOf(index + 1), "%name%", entry.name(),
                    "%blocks%", String.valueOf(entry.blocks()),
                    "%earned%", String.format(Locale.ROOT, "%.2f", entry.earned())));
        }
        return lines;
    }

    @Override
    protected void setupItems() {
        final ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        this.fill(fill);
        this.registerInventoryObject(this.periodButton(StatsPeriod.DAILY, Material.CLOCK),
                PERIOD_SLOTS[0]);
        this.registerInventoryObject(this.periodButton(StatsPeriod.WEEKLY, Material.PAPER),
                PERIOD_SLOTS[1]);
        this.registerInventoryObject(this.periodButton(StatsPeriod.ALL_TIME, Material.NETHER_STAR),
                PERIOD_SLOTS[2]);
        final ItemStack close = ItemUtils.createItem(Material.BARRIER,
                Messages.get("messages.leaderboard.close", "<red>Close"));
        this.registerInventoryObject(new BasicClickableInventoryObject(close,
                event -> event.getPlayer().closeInventory(), this), CLOSE_SLOT);
    }

    private BasicClickableInventoryObject periodButton(final StatsPeriod type,
                                                       final Material material) {
        final ItemStack icon = ItemUtils.createItem(material, Messages.get(
                "messages.leaderboard.period." + type.name().toLowerCase(Locale.ROOT),
                switch (type) {
                    case DAILY -> "<aqua>Daily";
                    case WEEKLY -> "<aqua>Weekly";
                    case ALL_TIME -> "<aqua>All time";
                }));
        return new BasicClickableInventoryObject(icon, event -> {
            this.period = type;
            this.update();
        }, this);
    }

    @Override
    public void update() {
        for (final int slot : ENTRY_SLOTS) {
            this.mainInventory.setItem(slot, null);
        }
        if (!PlayerStatsManager.isEnabled()) {
            this.mainInventory.setItem(ENTRY_SLOTS[13], ItemUtils.createItem(Material.BARRIER,
                    Messages.get("messages.leaderboard.disabled",
                            "<red>Statistics are disabled on this server.")));
            return;
        }
        final StatsPeriod requested = this.period;
        PlayerStatsManager.leaderboard(requested, ENTRY_SLOTS.length, entries -> {
            if (this.period != requested) {
                return;
            }
            for (int index = 0; index < entries.size() && index < ENTRY_SLOTS.length; ++index) {
                this.mainInventory.setItem(ENTRY_SLOTS[index],
                        entryItem(index + 1, entries.get(index)));
            }
            if (entries.isEmpty()) {
                this.mainInventory.setItem(ENTRY_SLOTS[13], ItemUtils.createItem(Material.BARRIER,
                        Messages.get("messages.leaderboard.empty",
                                "<gray>No statistics are available yet.")));
            }
        });
    }

    private ItemStack entryItem(final int rank, final LeaderboardEntry entry) {
        final ItemStack item = ItemUtils.createItem(Material.PAPER,
                Messages.get("messages.leaderboard.entryName", "<aqua>%rank%. %name%",
                        "%rank%", String.valueOf(rank), "%name%", entry.name()));
        return ItemUtils.addToLore(item,
                Messages.get("messages.leaderboard.entryBlocks", "<gray>Blocks: <white>%blocks%",
                        "%blocks%", String.valueOf(entry.blocks())),
                Messages.get("messages.leaderboard.entryEarned", "<gray>Earned: <white>%earned%",
                        "%earned%", String.format(Locale.ROOT, "%.2f", entry.earned())));
    }

    public StatsPeriod period() {
        return this.period;
    }
}
