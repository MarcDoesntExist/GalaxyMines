package me.marcdoesntexist.galaxymines.api.commands;

import me.marcdoesntexist.galaxymines.api.CustomPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class CustomCommand extends Command {
    private final ArrayList<SubCommand> subCommands;
    public String permission;
    protected List<String> usageMessages;

    public CustomCommand(final String name, final String permission) {
        super(name);
        this.subCommands = new ArrayList<SubCommand>();
        CustomPlugin.registerPermission(permission);
        this.permission = permission;
    }

    public CustomCommand(final String name, final String permission, final List<String> aliases) {
        super(name);
        this.subCommands = new ArrayList<SubCommand>();
        CustomPlugin.registerPermission(permission);
        this.setAliases(aliases);
        this.permission = permission;
    }

    public CustomCommand(final String name, final String permission, final List<String> aliases,
                         final String description) {
        super(name);
        this.subCommands = new ArrayList<SubCommand>();
        CustomPlugin.registerPermission(permission);
        this.setAliases(aliases);
        this.setDescription(description);
        this.permission = permission;
    }

    public CustomCommand(final String name, final String permission, final List<String> aliases,
                         final String description, final String usage) {
        super(name);
        this.subCommands = new ArrayList<SubCommand>();
        CustomPlugin.registerPermission(permission);
        this.setAliases(aliases);
        this.setDescription(description);
        this.setUsage(usage);
        this.permission = permission;
    }

    public static boolean isInteger(final String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException | NullPointerException ex) {
            return false;
        }
        return true;
    }

    public boolean execute(final CommandSender cs, final String string, final String[] strings) {
        return this.handleExecute(cs, strings);
    }

    public void addSubCommand(final SubCommand cmd) {
        this.subCommands.add(cmd);
    }

    public SubCommand getSubCommand(final String name) {
        for (final SubCommand command : this.subCommands) {
            if (command.getName().equals(name) || command.getAliases().contains(name)) {
                return command;
            }
        }
        return null;
    }

    public boolean checkPerm(final Player p) {
        return p.hasPermission(this.permission);
    }

    public void sendUsage(final Player p) {
        CustomPlugin.chatManager.sendMessage(p, this.usageMessages);
    }

    public void sendInvalidArguments(final Player p, final String msg) {
        CustomPlugin.chatManager.sendMessage(p, "§cInvalid arguments! §7" + msg);
    }

    public abstract boolean handleExecute(final CommandSender p0, final String[] p1);
}
