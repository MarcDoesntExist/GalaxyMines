package me.clip.placeholderapi.expansion;

import org.bukkit.OfflinePlayer;

public abstract class PlaceholderExpansion {
    public abstract String getIdentifier();

    public abstract String getAuthor();

    public abstract String getVersion();

    public boolean persist() {
        return false;
    }

    public boolean canRegister() {
        return true;
    }

    public String getRequiredPlugin() {
        return null;
    }

    public String onRequest(final OfflinePlayer player, final String params) {
        return null;
    }
}
