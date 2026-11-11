package me.marcdoesntexist.galaxymines.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;


public class SellCommandTree {
    private static final String PERMISSION = "galaxymines.sell";

    private SellCommandTree() {
    }

    public static List<LiteralCommandNode<CommandSourceStack>> build(final SellCommand command) {
        final LiteralCommandNode<CommandSourceStack> sell = Commands.literal("sell")
                .requires(SellCommandTree::hasAccess)
                .executes(context -> sellHand(command, context))
                .then(Commands.literal("hand")
                        .executes(context -> sellHand(command, context)))
                .then(Commands.literal("all")
                        .executes(context -> sellAll(command, context)))
                .build();
        final LiteralCommandNode<CommandSourceStack> sellAll = Commands.literal("sellall")
                .requires(SellCommandTree::hasAccess)
                .executes(context -> sellAll(command, context))
                .build();
        final LiteralCommandNode<CommandSourceStack> sellHand = Commands.literal("sellhand")
                .requires(SellCommandTree::hasAccess)
                .executes(context -> sellHand(command, context))
                .build();
        return List.of(sell, sellAll, sellHand);
    }

    private static boolean hasAccess(final CommandSourceStack source) {
        return source.getSender().hasPermission(PERMISSION)
                || source.getSender().hasPermission("galaxymines.admin");
    }

    private static int sellHand(final SellCommand command,
                                final CommandContext<CommandSourceStack> context) {
        command.sellHand(context.getSource().getSender());
        return Command.SINGLE_SUCCESS;
    }

    private static int sellAll(final SellCommand command,
                               final CommandContext<CommandSourceStack> context) {
        command.sellAll(context.getSource().getSender());
        return Command.SINGLE_SUCCESS;
    }
}
