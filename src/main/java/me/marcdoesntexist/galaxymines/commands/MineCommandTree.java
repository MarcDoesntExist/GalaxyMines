package me.marcdoesntexist.galaxymines.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.marcdoesntexist.galaxymines.events.MineEventManager;
import me.marcdoesntexist.galaxymines.gui.menus.SellShopMenu;
import me.marcdoesntexist.galaxymines.integration.economy.BuyService;
import me.marcdoesntexist.galaxymines.mines.MineManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class MineCommandTree {
    private static final String PERMISSION = "galaxymines.admin";

    private static final SuggestionProvider<CommandSourceStack> MINE_NAMES =
            (context, builder) -> {
                for (final var mine : MineManager.getMines()) {
                    builder.suggest(mine.getName());
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> PERIODS =
            (context, builder) -> {
                builder.suggest("daily");
                builder.suggest("weekly");
                builder.suggest("all");
                return builder.buildFuture();
            };

    



    private static final SuggestionProvider<CommandSourceStack> BUYABLE =
            (context, builder) -> {
                for (final String key
                        : me.marcdoesntexist.galaxymines.integration.economy.PriceRegistry
                        .snapshot().keySet()) {
                    if (!key.contains(":")) {
                        builder.suggest(key.toLowerCase(java.util.Locale.ROOT));
                    }
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> EVENT_IDS =
            (context, builder) -> {
                for (final String id : MineEventManager.get().knownEvents()) {
                    builder.suggest(id);
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> BOOSTER_TYPES =
            (context, builder) -> {
                builder.suggest("sell");
                builder.suggest("drop");
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS =
            (context, builder) -> {
                for (final var player : Bukkit.getOnlinePlayers()) {
                    builder.suggest(player.getName());
                }
                return builder.buildFuture();
            };

    private MineCommandTree() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(final GMineCommand command) {
        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("mine")
                .executes(context -> execute(command, context, List.of()));

        root.then(Commands.literal("create")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("create")))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .executes(context -> execute(command, context,
                                List.of("create", string(context, "name"))))
                        .then(Commands.argument("resetDelay", IntegerArgumentType.integer(1))
                                .executes(context -> execute(command, context,
                                        List.of("create", string(context, "name"),
                                                integer(context, "resetDelay"))))
                                .then(Commands.argument("broadcast", BoolArgumentType.bool())
                                        .executes(context -> executeCreate(context, command, "broadcast"))
                                        .then(Commands.argument("nearby", BoolArgumentType.bool())
                                                .executes(context -> executeCreate(context, command, "nearby"))
                                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.01D))
                                                        .executes(context -> executeCreate(context, command, "radius"))
                                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                                .executes(context -> executeCreate(context, command,
                                                                        "message")))))))));

        root.then(Commands.literal("tp")
                .requires(source -> source.getSender().hasPermission("galaxymines.tp")
                        || source.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .executes(context -> execute(command, context,
                                List.of("tp", string(context, "name"))))));

        root.then(Commands.literal("setspawn")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .executes(context -> execute(command, context,
                                List.of("setspawn", string(context, "name"))))));

        root.then(Commands.literal("pause")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .executes(context -> execute(command, context,
                                List.of("pause", string(context, "name"))))));

        root.then(Commands.literal("resume")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .executes(context -> execute(command, context,
                                List.of("resume", string(context, "name"))))));

        root.then(Commands.literal("stats")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .executes(context -> execute(command, context,
                                List.of("stats", string(context, "name"))))));

        root.then(Commands.literal("tier")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .executes(context -> execute(command, context,
                                List.of("tier", string(context, "name"))))
                        .then(Commands.literal("setlevel")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> execute(command, context,
                                                List.of("tier", string(context, "name"),
                                                        "setlevel", integer(context, "level"))))))
                        .then(Commands.literal("addblocks")
                                .then(Commands.argument("blocks",
                                                IntegerArgumentType.integer(1, 1_000_000_000))
                                        .executes(context -> execute(command, context,
                                                List.of("tier", string(context, "name"),
                                                        "addblocks", integer(context, "blocks"))))))));

        root.then(Commands.literal("expand")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .then(Commands.argument("blocks", IntegerArgumentType.integer(1, 64))
                                .executes(context -> execute(command, context,
                                        List.of("expand", string(context, "name"),
                                                integer(context, "blocks"))))
                                .then(Commands.literal("confirm")
                                        .executes(context -> execute(command, context,
                                                List.of("expand", string(context, "name"),
                                                        integer(context, "blocks"),
                                                        "confirm")))))));

        root.then(Commands.literal("currencies")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("currencies"))));

        root.then(Commands.literal("delete")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("delete")))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .executes(context -> execute(command, context,
                                List.of("delete", string(context, "name"))))));

        root.then(Commands.literal("info")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("info")))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .executes(context -> execute(command, context,
                                List.of("info", string(context, "name"))))));

        root.then(Commands.literal("reset")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("reset")))
                .then(Commands.literal("all")
                        .executes(context -> execute(command, context, List.of("reset", "all"))))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .executes(context -> execute(command, context,
                                List.of("reset", string(context, "name"))))));

        root.then(Commands.literal("list")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("list"))));

        root.then(Commands.literal("gui")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("gui"))));

        root.then(Commands.literal("menu")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("gui"))));

        root.then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("reload"))));

        root.then(Commands.literal("prices")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("prices")))
                .then(Commands.literal("gui")
                        .requires(source -> source.getSender()
                                .hasPermission("galaxymines.prices.edit"))
                        .executes(context -> execute(command, context,
                                List.of("prices", "gui"))))
                .then(Commands.literal("set")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .then(Commands.argument("value",
                                                DoubleArgumentType.doubleArg(0.0D))
                                        .executes(context -> execute(command, context,
                                                List.of("prices", "set", string(context, "key"),
                                                        Double.toString(DoubleArgumentType
                                                                .getDouble(context, "value"))))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .executes(context -> execute(command, context,
                                        List.of("prices", "remove",
                                                string(context, "key")))))));

        root.then(Commands.literal("milestones")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("milestones")))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYERS)
                        .executes(context -> execute(command, context,
                                List.of("milestones", string(context, "player"))))));

        root.then(Commands.literal("booster")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("booster")))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(BOOSTER_TYPES)
                                        .then(Commands.argument("multiplier",
                                                        DoubleArgumentType.doubleArg(1.0D))
                                                .then(Commands.argument("duration",
                                                                StringArgumentType.word())
                                                        .executes(context -> execute(command,
                                                                context,
                                                                List.of("booster", "give",
                                                                        string(context, "player"),
                                                                        string(context, "type"),
                                                                        Double.toString(
                                                                                DoubleArgumentType
                                                                                        .getDouble(
                                                                                                context,
                                                                                                "multiplier")),
                                                                        string(context,
                                                                                "duration")))))))))
                .then(Commands.literal("list")
                        .executes(context -> execute(command, context,
                                List.of("booster", "list")))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .executes(context -> execute(command, context,
                                        List.of("booster", "list",
                                                string(context, "player"))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .executes(context -> execute(command, context,
                                        List.of("booster", "clear",
                                                string(context, "player"))))
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(BOOSTER_TYPES)
                                        .executes(context -> execute(command, context,
                                                List.of("booster", "clear",
                                                        string(context, "player"),
                                                        string(context, "type"))))))));

        root.then(Commands.literal("history")
                .executes(context -> execute(command, context, List.of("history")))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYERS)
                        .executes(context -> execute(command, context,
                                List.of("history", string(context, "player"))))));

        root.then(Commands.literal("quests")
                .executes(context -> execute(command, context, List.of("quests")))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYERS)
                        .executes(context -> execute(command, context,
                                List.of("quests", string(context, "player")))))
                .then(Commands.literal("reset")
                        .requires(source -> source.getSender().hasPermission(PERMISSION))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .executes(context -> execute(command, context,
                                        List.of("quests", "reset",
                                                string(context, "player")))))));

        root.then(Commands.literal("daily")
                .executes(context -> execute(command, context, List.of("daily")))
                .then(Commands.literal("reset")
                        .requires(source -> source.getSender().hasPermission(PERMISSION))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .executes(context -> execute(command, context,
                                        List.of("daily", "reset",
                                                string(context, "player")))))));

        root.then(Commands.literal("pass")
                .executes(context -> execute(command, context, List.of("pass")))
                .then(Commands.literal("claim")
                        .executes(context -> execute(command, context,
                                List.of("pass", "claim"))))
                .then(Commands.literal("addxp")
                        .requires(source -> source.getSender().hasPermission(PERMISSION))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .then(Commands.argument("amount",
                                                IntegerArgumentType.integer(1))
                                        .executes(context -> execute(command, context,
                                                List.of("pass", "addxp",
                                                        string(context, "player"),
                                                        integer(context, "amount")))))))
                .then(Commands.literal("reset")
                        .requires(source -> source.getSender().hasPermission(PERMISSION))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .executes(context -> execute(command, context,
                                        List.of("pass", "reset",
                                                string(context, "player")))))));

        root.then(Commands.literal("bomb")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("bomb")))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .executes(context -> execute(command, context,
                                        List.of("bomb", "give",
                                                string(context, "player"))))
                                .then(Commands.argument("radius",
                                                IntegerArgumentType.integer(1, 12))
                                        .executes(context -> execute(command, context,
                                                List.of("bomb", "give",
                                                        string(context, "player"),
                                                        integer(context, "radius"))))))));

        root.then(Commands.literal("automine")
                .executes(context -> execute(command, context, List.of("automine")))
                .then(Commands.literal("on")
                        .executes(context -> execute(command, context,
                                List.of("automine", "on"))))
                .then(Commands.literal("off")
                        .executes(context -> execute(command, context,
                                List.of("automine", "off")))));

        root.then(Commands.literal("providers")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("providers"))));

        root.then(Commands.literal("edit")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("edit")))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .then(Commands.literal("add")
                                .then(Commands.argument("material", StringArgumentType.word())
                                        .then(Commands.argument("percentage", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "add",
                                                                string(context, "material"),
                                                                Double.toString(DoubleArgumentType.getDouble(context,
                                                                        "percentage"))))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("material", StringArgumentType.word())
                                        .executes(context -> execute(command, context,
                                                List.of("edit", string(context, "name"), "remove",
                                                        string(context, "material"))))))
                        .then(Commands.literal("commands")
                                .then(resetCommandsBranch("pre", command))
                                .then(resetCommandsBranch("post", command)))
                        .then(Commands.literal("constraints")
                                .then(constraintsBranch("break", command))
                                .then(constraintsBranch("place", command)))
                        .then(Commands.literal("set")
                                .then(Commands.literal("alias")
                                        .then(Commands.argument("alias", StringArgumentType.greedyString())
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set", "alias",
                                                                StringArgumentType.getString(context, "alias"))))))
                                .then(Commands.literal("resetdelay")
                                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1))
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set", "resetdelay",
                                                                integer(context, "minutes"))))))
                                .then(Commands.literal("resetpercent")
                                        .then(Commands.argument("percentage",
                                                        DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set",
                                                                "resetpercent",
                                                                Double.toString(DoubleArgumentType.getDouble(
                                                                        context, "percentage")))))))
                                .then(Commands.literal("resetminpercent")
                                        .then(Commands.argument("percentage",
                                                        DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set",
                                                                "resetminpercent",
                                                                Double.toString(DoubleArgumentType.getDouble(
                                                                        context, "percentage")))))))
                                .then(Commands.literal("resetmaxpercent")
                                        .then(Commands.argument("percentage",
                                                        DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set",
                                                                "resetmaxpercent",
                                                                Double.toString(DoubleArgumentType.getDouble(
                                                                        context, "percentage")))))))
                                .then(Commands.literal("resetcheck")
                                        .then(Commands.argument("ticks",
                                                        IntegerArgumentType.integer(0, 72_000))
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set",
                                                                "resetcheck", integer(context, "ticks"))))))
                                .then(Commands.literal("wavespeed")
                                        .then(Commands.argument("layers",
                                                        IntegerArgumentType.integer(0, 64))
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set",
                                                                "wavespeed",
                                                                integer(context, "layers"))))))
                                .then(Commands.literal("wavedirection")
                                        .then(Commands.argument("direction",
                                                        StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("top");
                                                    builder.suggest("bottom");
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set",
                                                                "wavedirection",
                                                                string(context, "direction"))))))
                                .then(Commands.literal("currency")
                                        .then(Commands.argument("currency", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    for (final String id : me.marcdoesntexist
                                                            .galaxymines.integration.economy
                                                            .CurrencyRegistry.ids()) {
                                                        builder.suggest(id);
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set",
                                                                "currency", string(context, "currency"))))))
                                .then(Commands.literal("sellpriority")
                                        .then(Commands.argument("priority", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("provider");
                                                    builder.suggest("material");
                                                    builder.suggest("highest");
                                                    builder.suggest("global");
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set",
                                                                "sellpriority", string(context, "priority"))))))
                                .then(Commands.literal("nearbybroadcast")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set",
                                                                "nearbybroadcast",
                                                                Boolean.toString(BoolArgumentType.getBool(context,
                                                                        "value")))))))
                                .then(Commands.literal("broadcastrange")
                                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.01D))
                                                .executes(context -> execute(command, context,
                                                        List.of("edit", string(context, "name"), "set",
                                                                "broadcastRange",
                                                                Double.toString(DoubleArgumentType.getDouble(context,
                                                                        "radius"))))))))));

        root.then(Commands.literal("resetall")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("resetall")))
                .then(Commands.literal("confirm")
                        .executes(context -> execute(command, context,
                                List.of("resetall", "confirm")))));

        root.then(Commands.literal("rename")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .then(Commands.argument("newName", StringArgumentType.word())
                                .executes(context -> execute(command, context,
                                        List.of("rename", string(context, "name"),
                                                string(context, "newName")))))));

        root.then(Commands.literal("clone")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(MINE_NAMES)
                        .then(Commands.argument("newName", StringArgumentType.word())
                                .executes(context -> execute(command, context,
                                        List.of("clone", string(context, "name"),
                                                string(context, "newName")))))));

        
        
        root.then(Commands.literal("shop")
                .requires(source -> source.getSender().hasPermission("galaxymines.sell")
                        || source.getSender().hasPermission(PERMISSION))
                .executes(context -> {
                    if (!(context.getSource().getSender() instanceof Player player)) {
                        return 0;
                    }
                    SellShopMenu.openShop(player);
                    return Command.SINGLE_SUCCESS;
                }));

        
        
        
        root.then(Commands.literal("buy")
                .requires(source -> source.getSender().hasPermission("galaxymines.buy")
                        || source.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("material", StringArgumentType.word())
                        .suggests(BUYABLE)
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304))
                                .executes(context -> {
                                    if (!(context.getSource().getSender() instanceof Player player)) {
                                        return 0;
                                    }
                                    BuyService.buy(player,
                                            context.getArgument("material", String.class),
                                            context.getArgument("amount", Integer.class));
                                    return Command.SINGLE_SUCCESS;
                                }))));

        root.then(Commands.literal("debug")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("debug")))
                .then(Commands.literal("timings")
                        .executes(context -> execute(command, context,
                                List.of("debug", "timings")))));

        root.then(Commands.literal("wand")
                .requires(source -> source.getSender().hasPermission("galaxymines.wand")
                        || source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("wand"))));

        root.then(Commands.literal("top")
                .requires(source -> source.getSender().hasPermission("galaxymines.stats")
                        || source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("top")))
                .then(Commands.argument("period", StringArgumentType.word())
                        .suggests(PERIODS)
                        .executes(context -> execute(command, context,
                                List.of("top", string(context, "period"))))));

        root.then(Commands.literal("leaderboard")
                .requires(source -> source.getSender().hasPermission("galaxymines.stats")
                        || source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("leaderboard")))
                .then(Commands.argument("period", StringArgumentType.word())
                        .suggests(PERIODS)
                        .executes(context -> execute(command, context,
                                List.of("leaderboard", string(context, "period"))))));

        root.then(Commands.literal("mystats")
                .requires(source -> source.getSender().hasPermission("galaxymines.stats")
                        || source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("mystats")))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYERS)
                        .executes(context -> execute(command, context,
                                List.of("mystats", string(context, "player"))))));

        root.then(Commands.literal("event")
                .requires(source -> source.getSender().hasPermission("galaxymines.events")
                        || source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("event", "list")))
                .then(Commands.literal("list")
                        .executes(context -> execute(command, context,
                                List.of("event", "list"))))
                .then(Commands.literal("start")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(EVENT_IDS)
                                .executes(context -> execute(command, context,
                                        List.of("event", "start", string(context, "id"))))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(EVENT_IDS)
                                .executes(context -> execute(command, context,
                                        List.of("event", "stop", string(context, "id"))))))
                .then(Commands.literal("reload")
                        .executes(context -> execute(command, context,
                                List.of("event", "reload")))));

        root.then(Commands.literal("luckyblocks")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .executes(context -> execute(command, context, List.of("luckyblocks")))
                .then(Commands.literal("reload")
                        .executes(context -> execute(command, context,
                                List.of("luckyblocks", "reload"))))
                .then(Commands.literal("validate")
                        .executes(context -> execute(command, context,
                                List.of("luckyblocks", "validate")))));

        return root.build();
    }

    private static int execute(final GMineCommand command,
                               final CommandContext<CommandSourceStack> context, final List<String> args) {
        command.handleExecute(context.getSource().getSender(), args.toArray(String[]::new));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeCreate(final CommandContext<CommandSourceStack> context,
                                     final GMineCommand command, final String stage) {
        final List<String> args = new ArrayList<>();
        args.add("create");
        args.add(string(context, "name"));
        args.add(integer(context, "resetDelay"));

        if (stage.equals("broadcast") || stage.equals("nearby") || stage.equals("radius")
                || stage.equals("message")) {
            args.add(Boolean.toString(BoolArgumentType.getBool(context, "broadcast")));
        }
        if (stage.equals("nearby") || stage.equals("radius") || stage.equals("message")) {
            args.add(Boolean.toString(BoolArgumentType.getBool(context, "nearby")));
        }
        if (stage.equals("radius") || stage.equals("message")) {
            args.add(Double.toString(DoubleArgumentType.getDouble(context, "radius")));
        }
        if (stage.equals("message")) {
            args.add(StringArgumentType.getString(context, "message"));
        }
        return execute(command, context, args);
    }

    



    private static LiteralArgumentBuilder<CommandSourceStack> constraintsBranch(
            final String section, final GMineCommand command) {
        final LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(section)
                .then(Commands.literal("list")
                        .executes(context -> execute(command, context,
                                List.of("edit", string(context, "name"), "constraints", section,
                                        "list"))))
                .then(Commands.literal("clear")
                        .executes(context -> execute(command, context,
                                List.of("edit", string(context, "name"), "constraints", section,
                                        "clear"))))
                .then(Commands.literal("allow")
                        .then(Commands.argument("material", StringArgumentType.word())
                                .executes(context -> execute(command, context,
                                        List.of("edit", string(context, "name"), "constraints",
                                                section, "allow",
                                                string(context, "material"))))))
                .then(Commands.literal("deny")
                        .then(Commands.argument("material", StringArgumentType.word())
                                .executes(context -> execute(command, context,
                                        List.of("edit", string(context, "name"), "constraints",
                                                section, "deny", string(context, "material"))))));
        if (section.equals("place")) {
            node.then(Commands.literal("on")
                    .executes(context -> execute(command, context,
                            List.of("edit", string(context, "name"), "constraints", section,
                                    "on"))));
            node.then(Commands.literal("off")
                    .executes(context -> execute(command, context,
                            List.of("edit", string(context, "name"), "constraints", section,
                                    "off"))));
        }
        return node;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> resetCommandsBranch(
            final String phase, final GMineCommand command) {
        return Commands.literal(phase)
                .then(Commands.literal("list")
                        .executes(context -> execute(command, context,
                                List.of("edit", string(context, "name"), "commands", phase,
                                        "list"))))
                .then(Commands.literal("add")
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes(context -> execute(command, context,
                                        List.of("edit", string(context, "name"), "commands", phase,
                                                "add",
                                                StringArgumentType.getString(context, "command"))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> execute(command, context,
                                        List.of("edit", string(context, "name"), "commands", phase,
                                                "remove", integer(context, "index"))))));
    }

    private static String string(final CommandContext<CommandSourceStack> context,
                                 final String name) {
        return StringArgumentType.getString(context, name);
    }

    private static String integer(final CommandContext<CommandSourceStack> context,
                                  final String name) {
        return Integer.toString(IntegerArgumentType.getInteger(context, name));
    }
}