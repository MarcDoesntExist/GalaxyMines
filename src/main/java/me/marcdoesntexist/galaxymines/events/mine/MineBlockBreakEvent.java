package me.marcdoesntexist.galaxymines.events.mine;

import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MineBlockBreakEvent extends CancellableMineEvent<MineBlockBreakEvent> {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Block block;
    private final Player player;
    private final Cancellable event;
    private final List<ItemStack> drops = new ArrayList<>();
    private boolean dropsModified;

    public MineBlockBreakEvent(Mine mine, Block block, Player player, Cancellable e) {
        super(mine);
        this.block = block;
        this.player = player;
        this.event = e;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Block getBlock() {
        return block;
    }

    public Player getPlayer() {
        return player;
    }


    public List<ItemStack> getDrops() {
        return this.drops;
    }

    public void setDrops(final Collection<ItemStack> newDrops) {
        this.drops.clear();
        if (newDrops != null) {
            for (final ItemStack stack : newDrops) {
                if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
                    this.drops.add(stack);
                }
            }
        }
        this.dropsModified = true;
    }

    public void addDrop(final ItemStack stack) {
        if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
            this.drops.add(stack);
            this.dropsModified = true;
        }
    }

    public boolean isDropsModified() {
        return this.dropsModified;
    }

    @Override
    public void setCancelled(boolean cancel) {
        super.setCancelled(cancel);
        event.setCancelled(cancel);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
