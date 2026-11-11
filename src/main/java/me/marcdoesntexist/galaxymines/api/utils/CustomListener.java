package me.marcdoesntexist.galaxymines.api.utils;

import me.marcdoesntexist.galaxymines.api.CustomPlugin;
import org.bukkit.event.Listener;

public class CustomListener implements Listener {
    public void register() {
        CustomPlugin.registerListener(this);
    }

    public void unregister() {
        CustomPlugin.unregisterListener(this);
    }
}
