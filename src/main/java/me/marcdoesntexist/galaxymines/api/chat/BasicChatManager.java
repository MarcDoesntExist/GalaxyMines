package me.marcdoesntexist.galaxymines.api.chat;

import me.marcdoesntexist.galaxymines.api.CustomPlugin;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class BasicChatManager implements ChatManager {
    @Override
    public void sendMessage(final Player p, final String string) {
        final StringBuilder message = new StringBuilder();
        final MessageManager manager = CustomPlugin.messageManager;
        if (manager.getPluginPrefix() != null) {
            message.append(manager.getPluginPrefix());
        }
        if (manager.getMessageSeparator() != null) {
            message.append(manager.getMessageSeparator());
        }
        message.append(manager.translateAll(string));
        p.sendMessage(Text.parse(message.toString()));
    }

    @Override
    public void sendMessage(final Player p, final String[] messages) {
        for (final String usageMessage : messages) {
            this.sendMessage(p, usageMessage);
        }
    }

    @Override
    public void sendMessage(final Player p, final List<String> messages) {
        for (final String usageMessage : messages) {
            this.sendMessage(p, usageMessage);
        }
    }
}
