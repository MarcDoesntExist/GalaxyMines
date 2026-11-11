package me.marcdoesntexist.galaxymines.api.chat;

import me.marcdoesntexist.galaxymines.api.Manager;

import java.util.List;

public interface MessageManager extends Manager {
    String getPluginPrefix();

    String getMessageSeparator();

    String translateTags(final String p0);

    String translateColors(final String p0);

    String translateAll(final String p0);

    List<String> translateTags(final Iterable<String> p0);

    List<String> translateColors(final Iterable<String> p0);

    List<String> translateAll(final Iterable<String> p0);
}
