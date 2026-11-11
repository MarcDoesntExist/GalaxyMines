package me.marcdoesntexist.galaxymines.api.chat;

import me.marcdoesntexist.galaxymines.api.Manager;
import org.bukkit.entity.Player;

import java.util.List;

public interface ChatManager extends Manager {
    void sendMessage(final Player p0, final String p1);

    void sendMessage(final Player p0, final String[] p1);

    void sendMessage(final Player p0, final List<String> p1);
}
