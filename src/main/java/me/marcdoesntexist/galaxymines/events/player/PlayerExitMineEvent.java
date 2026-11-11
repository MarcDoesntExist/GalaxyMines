package me.marcdoesntexist.galaxymines.events.player;

import me.marcdoesntexist.galaxymines.api.events.CustomEvent;
import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;




public class PlayerExitMineEvent extends CustomEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Mine mine;

    public PlayerExitMineEvent(final Player player, final Mine mine) {
        this.player = player;
        this.mine = mine;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Mine getMine() {
        return this.mine;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
