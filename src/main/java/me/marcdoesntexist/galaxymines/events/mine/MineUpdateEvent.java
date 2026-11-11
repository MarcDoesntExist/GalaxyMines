package me.marcdoesntexist.galaxymines.events.mine;

import me.marcdoesntexist.galaxymines.api.events.CustomEvent;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.event.HandlerList;

public class MineUpdateEvent extends CustomEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Mine mine;

    public MineUpdateEvent(final Mine mine) {
        this.mine = mine;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Mine getMine() {
        return this.mine;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
