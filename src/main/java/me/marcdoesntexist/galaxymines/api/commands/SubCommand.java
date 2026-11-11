package me.marcdoesntexist.galaxymines.api.commands;

import me.marcdoesntexist.galaxymines.api.CustomPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class SubCommand {
    public final CustomCommand command;
    protected final String name;
    protected List<String> usageMessages;
    protected List<String> aliases;

    public SubCommand(final String name, final CustomCommand permission) {
        this.name = name;
        this.command = permission;
    }

    public SubCommand(final String name, final CustomCommand command, final List<String> aliases) {
        this.aliases = aliases;
        this.command = command;
        this.name = name;
    }

    public static boolean isInteger(final String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException | NullPointerException ex) {
            return false;
        }
        return true;
    }

    public List<String> getUsageMessages() {
        return this.usageMessages;
    }

    public CustomCommand getCommand() {
        return this.command;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public void setAliases(final List<String> aliases) {
        this.aliases = aliases;
    }

    public String getName() {
        return this.name;
    }

    public boolean checkPerm(final Player p) {
        return p.hasPermission(this.command.permission);
    }

    public void sendUsage(final Player p) {
        CustomPlugin.chatManager.sendMessage(p, this.usageMessages);
    }

    public abstract void handleExecute(final CommandSender p0, final String[] p1);
}
