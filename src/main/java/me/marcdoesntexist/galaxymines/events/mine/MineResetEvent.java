package me.marcdoesntexist.galaxymines.events.mine;

import me.marcdoesntexist.galaxymines.api.events.CustomEvent;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class MineResetEvent extends CustomEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Mine mine;
    private boolean cancelled;

    public MineResetEvent(final Mine mine) {
        this.mine = mine;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Mine getMine() {
        return this.mine;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
