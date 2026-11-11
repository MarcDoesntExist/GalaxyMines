package me.marcdoesntexist.galaxymines.api.chat;

import me.marcdoesntexist.galaxymines.api.CustomPlugin;
import me.marcdoesntexist.galaxymines.api.Manager;

public class PluginChatManager extends BasicChatManager {
    private static PluginChatManager instance;

    public PluginChatManager(final CustomPlugin plugin) {
    }

    public static PluginChatManager getInstance(final CustomPlugin plugin) {
        if (PluginChatManager.instance == null) {
            PluginChatManager.instance = new PluginChatManager(plugin);
        }
        return PluginChatManager.instance;
    }

    @Override
    public Manager setup() {
        return this;
    }
}
