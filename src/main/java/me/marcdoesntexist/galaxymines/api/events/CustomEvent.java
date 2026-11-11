package me.marcdoesntexist.galaxymines.api.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CustomEvent extends Event {
    private static final HandlerList handlers;

    static {
        handlers = new HandlerList();
    }

    public static HandlerList getHandlerList() {
        return CustomEvent.handlers;
    }

    public CustomEvent call() {
        Bukkit.getPluginManager().callEvent(this);
        return this;
    }

    public HandlerList getHandlers() {
        return CustomEvent.handlers;
    }
}
