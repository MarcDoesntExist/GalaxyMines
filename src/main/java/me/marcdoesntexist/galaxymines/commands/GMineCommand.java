package me.marcdoesntexist.galaxymines.commands;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.api.commands.CustomCommand;
import me.marcdoesntexist.galaxymines.api.commands.SubCommand;
import me.marcdoesntexist.galaxymines.api.utils.MessageColor;
import me.marcdoesntexist.galaxymines.autominer.AutoMinerService;
import me.marcdoesntexist.galaxymines.autominer.AutoMinerSettings;
import me.marcdoesntexist.galaxymines.bomb.BombItem;
import me.marcdoesntexist.galaxymines.bomb.BombService;
import me.marcdoesntexist.galaxymines.compat.Materials;
import me.marcdoesntexist.galaxymines.config.GalaxyMinesConfiguration;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.daily.DailyRewardService;
import me.marcdoesntexist.galaxymines.events.MineEventManager;
import me.marcdoesntexist.galaxymines.exception.UnsupportedWorldEditRegionType;
import me.marcdoesntexist.galaxymines.gui.GUIManager;
import me.marcdoesntexist.galaxymines.gui.menus.LeaderboardMenu;
import me.marcdoesntexist.galaxymines.history.HistoryEntry;
import me.marcdoesntexist.galaxymines.history.HistoryService;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockRegistry;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockTokens;
import me.marcdoesntexist.galaxymines.integration.customblocks.ProviderDiagnostics;
import me.marcdoesntexist.galaxymines.integration.customblocks.ProviderVersions;
import me.marcdoesntexist.galaxymines.integration.economy.*;
import me.marcdoesntexist.galaxymines.luckyblocks.LuckyBlockManager;
import me.marcdoesntexist.galaxymines.luckyblocks.LuckyBlockParser;
import me.marcdoesntexist.galaxymines.messages.GalaxyMineMessageManager;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.*;
import me.marcdoesntexist.galaxymines.mines.types.CircularMine;
import me.marcdoesntexist.galaxymines.mines.types.CuboidMine;
import me.marcdoesntexist.galaxymines.mines.types.PolygonMine;
import me.marcdoesntexist.galaxymines.multiplier.BoosterParser;
import me.marcdoesntexist.galaxymines.multiplier.BoosterService;
import me.marcdoesntexist.galaxymines.multiplier.BoosterType;
import me.marcdoesntexist.galaxymines.multiplier.PlayerBooster;
import me.marcdoesntexist.galaxymines.pass.BattlePassService;
import me.marcdoesntexist.galaxymines.quest.QuestDefinition;
import me.marcdoesntexist.galaxymines.quest.QuestService;
import me.marcdoesntexist.galaxymines.stats.PlayerStats;
import me.marcdoesntexist.galaxymines.stats.PlayerStatsManager;
import me.marcdoesntexist.galaxymines.stats.StatsPeriod;
import me.marcdoesntexist.galaxymines.storage.StorageManager;
import me.marcdoesntexist.galaxymines.utils.TickMonitor;
import me.marcdoesntexist.galaxymines.utils.WorldEditHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GMineCommand extends CustomCommand {
    private static final String ADMIN_PERMISSION = "galaxymines.admin";
    private static final int LEADERBOARD_LIMIT = 10;
    private final EditCommand editCommand;
    private final Plugin owner;

    public GMineCommand() {
        this(GalaxyMines.getInstance());
    }


    GMineCommand(final Plugin owner) {
        super("galaxymines", ADMIN_PERMISSION,
                Arrays.asList("gmine", "galaxymine", "gmines", "mine", "mines"));
        this.owner = owner;
        this.addSubCommand(this.editCommand = new EditCommand(this));
        this.usageMessages = GalaxyMineMessageManager.getInstance().translateAll(Arrays
                .asList("<white>----------<red>♦ GalaxyMines created by Marcx96 ♦<white>----------",
                        "<aqua>➣ /mine <white>create (name) [resetDelay] [broadcast] [nearby] [radius] [message]",
                        "<aqua>➣ /mine <white>tp (name)",
                        "<aqua>➣ /mine <white>setspawn (name)",
                        "<aqua>➣ /mine <white>pause|resume (name)",
                        "<aqua>➣ /mine <white>stats (name)",
                        "<aqua>➣ /mine <white>currencies",
                        "<aqua>➣ /mine <white>delete (name)",
                        "<aqua>➣ /mine <white>info (name)",
                        "<aqua>➣ /mine <white>rename (name) (newName)",
                        "<aqua>➣ /mine <white>clone (name) (newName)",
                        "<aqua>➣ /mine <white>edit (name)",
                        "<aqua>➣ /mine <white>list",
                        "<aqua>➣ /mine <white>reset (name|all)",
                        "<aqua>➣ /mine <white>resetall confirm",
                        "<aqua>➣ /mine <white>wand",
                        "<aqua>➣ /mine <white>top [daily|weekly|all]",
                        "<aqua>➣ /mine <white>mystats [player]",
                        "<aqua>➣ /mine <white>leaderboard [daily|weekly|all]",
                        "<aqua>➣ /mine <white>event list|start|stop (id)|reload",
                        "<aqua>➣ /mine <white>luckyblocks reload|validate",
                        "<aqua>➣ /mine <white>booster give|list|clear",
                        "<aqua>➣ /mine <white>history [player]",
                        "<aqua>➣ /mine <white>daily [reset (player)]",
                        "<aqua>➣ /mine <white>quests [player]",
                        "<aqua>➣ /mine <white>debug",
                        "<aqua>➣ /mine <white>gui",
                        "<aqua>➣ /mine <white>reload",
                        "<white>--------------------"));
        final List<String> configuredUsage = Messages.getList("messages.command.usage");
        if (!configuredUsage.isEmpty()) {
            this.usageMessages = configuredUsage;
        }
    }

    @Override
    public boolean handleExecute(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            this.sendUsage(sender);
            return true;
        }
        final String function = args[0].toLowerCase(Locale.ROOT);
        switch (function) {
            case "tp", "teleport" -> this.handleTeleport(sender, args);
            case "create" -> this.handleCreate(sender, args);
            case "delete", "remove" -> this.handleDelete(sender, args);
            case "reset" -> this.handleReset(sender, args);
            case "resetall" -> this.handleResetAll(sender, args);
            case "info" -> this.handleInfo(sender, args);
            case "setspawn" -> this.handleSetSpawn(sender, args);
            case "pause" -> this.handlePause(sender, args, true);
            case "resume" -> this.handlePause(sender, args, false);
            case "stats" -> this.handleMineStats(sender, args);
            case "rename" -> this.handleRename(sender, args);
            case "clone" -> this.handleClone(sender, args);
            case "tier" -> this.handleTier(sender, args);
            case "expand" -> this.handleExpand(sender, args);
            case "currencies", "currency" -> this.handleCurrencies(sender);
            case "debug" -> this.handleDebug(sender, args);
            case "prices" -> this.handlePrices(sender, args);
            case "milestones" -> this.handleMilestones(sender, args);
            case "providers" -> this.handleProviders(sender);
            case "wand" -> this.handleWand(sender);
            case "top" -> this.handleLeaderboard(sender, args);
            case "leaderboard" -> this.handleLeaderboard(sender, args);
            case "mystats" -> this.handlePlayerStats(sender, args);
            case "event", "events" -> this.handleEvent(sender, args);
            case "luckyblocks", "luckyblock" -> this.handleLuckyBlocks(sender, args);
            case "booster", "boosters", "multiplier", "multipliers" -> this.handleBooster(sender, args);
            case "history" -> this.handleHistory(sender, args);
            case "daily" -> this.handleDaily(sender, args);
            case "quests", "quest" -> this.handleQuests(sender, args);
            case "pass", "battlepass" -> this.handlePass(sender, args);
            case "bomb", "bombs" -> this.handleBomb(sender, args);
            case "automine", "autominer" -> this.handleAutoMine(sender, args);
            case "reload" -> this.handleReload(sender);
            case "list" -> this.handleList(sender);
            case "gui", "menu" -> this.handleGui(sender);
            case "edit" -> this.editCommand.handleExecute(sender, args);
            default -> this.sendUsage(sender);
        }
        return true;
    }

    private void handleTeleport(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            this.send(sender, "messages.command.playerOnly",
                    "<red>This command can only be used by a player.");
            return;
        }
        if (!this.hasPermission(sender, "galaxymines.tp")) {
            this.send(sender, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        if (args.length < 2) {
            this.send(sender, "messages.command.specifyMine", "Specify the mine name.");
            return;
        }
        final Mine mine = MineManager.getMine(args[1]);
        if (mine == null) {
            this.sendMineNotFound(sender, args[1]);
            return;
        }
        if (!MineAccess.canAccess(player, mine)) {
            MineAccess.denyAccess(player, mine);
            return;
        }
        TeleportService.request(player, mine);
    }

    private void handleCreate(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            this.send(sender, "messages.command.playerOnly",
                    "<red>This command can only be used by a player.");
            return;
        }
        if (!this.hasPermission(sender, ADMIN_PERMISSION)) {
            this.send(sender, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        if (args.length < 2) {
            this.send(sender, "messages.command.specifyMine", "Specify the mine name.");
            return;
        }
        final String mineName = args[1];
        if (!MineManager.isValidName(mineName)) {
            this.send(sender, "messages.command.invalidName",
                    "Name may contain only letters, numbers, '_' and '-' (max %max% characters).",
                    "%max%", PluginSettings.get().maxMineNameLength);
            return;
        }
        if (!MineManager.isNameAvailable(mineName)) {
            this.send(sender, "messages.command.mineExists",
                    "A mine named %mine% already exists.", "%mine%", mineName);
            return;
        }
        final Region selection = this.selectionFor(player);
        if (selection == null) {
            this.send(sender, "messages.command.noSelection",
                    "Select a region with WorldEdit first.");
            return;
        }
        int delay = PluginSettings.get().defaultResetDelay;
        if (args.length > 2) {
            try {
                delay = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                this.send(sender, "messages.command.invalidNumber",
                        "%value% is not a valid number.", "%value%", args[2]);
                return;
            }
            if (delay < 1) {
                this.send(sender, "messages.command.resetDelayTooLow",
                        "Reset delay must be at least 1 minute.");
                return;
            }
        }
        final boolean broadcast = args.length <= 3
                ? PluginSettings.get().defaultBroadcastOnReset
                : Boolean.parseBoolean(args[3]);
        final boolean nearby = args.length > 4 ? Boolean.parseBoolean(args[4])
                : PluginSettings.get().defaultBroadcastNearby;
        double radius = PluginSettings.get().defaultBroadcastRadius;
        if (args.length > 5) {
            try {
                radius = Double.parseDouble(args[5]);
            } catch (NumberFormatException ex) {
                this.send(sender, "messages.command.invalidNumber",
                        "%value% is not a valid number.", "%value%", args[5]);
                return;
            }
            if (radius <= 0.0D) {
                this.send(sender, "messages.command.radiusTooLow",
                        "Radius must be greater than 0.");
                return;
            }
        }
        final String customMessage = args.length > 6
                ? String.join(" ", Arrays.copyOfRange(args, 6, args.length))
                : null;
        this.createNewMine(player, mineName, selection, delay, broadcast, nearby, radius,
                customMessage);
        final MineWandManager wandManager = GalaxyMines.getInstance().getWandManager();
        if (wandManager != null) {
            wandManager.clear(player);
        }
    }

    private void handleDelete(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            this.send(sender, "messages.command.specifyMine", "Specify the mine name.");
            return;
        }
        final Mine mine = MineManager.getMine(args[1]);
        if (mine == null) {
            this.sendMineNotFound(sender, args[1]);
            this.listMines(sender);
            return;
        }
        mine.delete();
        this.send(sender, "messages.command.mineDeleted",
                "Mine %mine% has been deleted.", "%mine%", mine.getName());
    }

    private void handleReset(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            this.send(sender, "messages.command.specifyMine", "Specify the mine name.");
            return;
        }
        if ("all".equalsIgnoreCase(args[1])) {
            this.resetAll(sender);
            return;
        }
        final Mine mine = MineManager.getMine(args[1]);
        if (mine == null) {
            this.sendMineNotFound(sender, args[1]);
            this.listMines(sender);
            return;
        }
        mine.reset();
        this.send(sender, "messages.command.mineReset",
                "Mine %mine% has been reset.", "%mine%", mine.getName());
    }

    private void handleResetAll(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 2 || !"confirm".equalsIgnoreCase(args[1])) {
            this.send(sender, "messages.command.resetAllConfirm",
                    "This resets every mine. Run /mine resetall confirm to continue.");
            return;
        }
        this.resetAll(sender);
    }

    private void resetAll(final CommandSender sender) {
        for (final Mine mine : MineManager.getMines()) {
            mine.reset();
        }
        this.send(sender, "messages.command.allMinesReset", "All mines have been reset.");
    }

    private void handleInfo(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            this.send(sender, "messages.command.specifyMine", "Specify the mine name.");
            return;
        }
        final Mine mine = MineManager.getMine(args[1]);
        if (mine == null) {
            this.sendMineNotFound(sender, args[1]);
            this.listMines(sender);
            return;
        }
        if (sender instanceof Player player) {
            MineManager.sendInfo(player, mine);
            return;
        }
        for (final String line : mine.getInfo()) {
            this.sendRaw(sender, line);
        }
    }

    private void handleSetSpawn(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            this.send(sender, "messages.command.playerOnly",
                    "<red>This command can only be used by a player.");
            return;
        }
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            this.send(sender, "messages.command.specifyMine", "Specify the mine name.");
            return;
        }
        final Mine mine = MineManager.getMine(args[1]);
        if (mine == null) {
            this.sendMineNotFound(sender, args[1]);
            return;
        }
        mine.setSpawnLocation(player.getLocation());
        this.send(sender, "messages.mine.spawnSet",
                "Spawn point for %mine% set to your current location.",
                "%mine%", mine.getName());
    }

    private void handlePause(final CommandSender sender, final String[] args,
                             final boolean pause) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            this.send(sender, "messages.command.specifyMine", "Specify the mine name.");
            return;
        }
        final Mine mine = MineManager.getMine(args[1]);
        if (mine == null) {
            this.sendMineNotFound(sender, args[1]);
            return;
        }
        mine.setPaused(pause);
        this.send(sender,
                pause ? "messages.mine.paused" : "messages.mine.resumed",
                pause ? "Mine %mine% paused." : "Mine %mine% resumed.",
                "%mine%", mine.getName());
    }

    private void handleMineStats(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            this.send(sender, "messages.command.specifyMine", "Specify the mine name.");
            return;
        }
        final Mine mine = MineManager.getMine(args[1]);
        if (mine == null) {
            this.sendMineNotFound(sender, args[1]);
            return;
        }
        final StorageManager.MineStats stats = StorageManager.getStats(mine);
        this.send(sender, "messages.mine.stats",
                "%mine%: %remaining% blocks remaining, %percent%% mined, %resets% resets.",
                "%mine%", mine.getName(),
                "%remaining%", String.valueOf(mine.getRemainingBlocks()),
                "%percent%", String.valueOf(mine.getPercentageMined()),
                "%resets%", String.valueOf(stats.resets()));
    }


    private void handleTier(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            this.send(sender, "messages.command.specifyMine", "Specify the mine name.");
            return;
        }
        final Mine mine = MineManager.getMine(args[1]);
        if (mine == null) {
            this.sendMineNotFound(sender, args[1]);
            return;
        }
        final MineProgression progression = mine.getProgression();
        if (args.length >= 3 && args[2].equalsIgnoreCase("setlevel")) {
            if (args.length < 4) {
                this.send(sender, "messages.command.tierUsage",
                        "Usage: /mine tier %mine% [setlevel (n)|addblocks (n)]",
                        "%mine%", mine.getName());
                return;
            }
            try {
                final int level = Integer.parseInt(args[3]);
                if (level < 1 || level > progression.maxLevel()) {
                    throw new NumberFormatException();
                }
                mine.setProgression(progression.withLevel(level));
                this.send(sender, "messages.command.tierSet",
                        "Tier of %mine% set to %level%.",
                        "%mine%", mine.getName(), "%level%", Integer.toString(level));
            } catch (NumberFormatException ex) {
                this.send(sender, "messages.command.invalidNumber",
                        "%value% is not a valid number.", "%value%", args[3]);
            }
            return;
        }
        if (args.length >= 3 && args[2].equalsIgnoreCase("addblocks")) {
            if (args.length < 4) {
                this.send(sender, "messages.command.tierUsage",
                        "Usage: /mine tier %mine% [setlevel (n)|addblocks (n)]",
                        "%mine%", mine.getName());
                return;
            }
            try {
                final long amount = Long.parseLong(args[3]);
                if (amount < 1L || amount > 1_000_000_000L) {
                    throw new NumberFormatException();
                }
                final MineProgression updated =
                        progression.withMined(progression.minedBlocks() + amount);
                mine.setProgression(updated);
                this.send(sender, "messages.command.tierAdded",
                        "Added %blocks% blocks to %mine%: tier %level%.",
                        "%blocks%", Long.toString(amount), "%mine%", mine.getName(),
                        "%level%", Integer.toString(updated.level()));
            } catch (NumberFormatException ex) {
                this.send(sender, "messages.command.invalidNumber",
                        "%value% is not a valid number.", "%value%", args[3]);
            }
            return;
        }
        if (args.length >= 3) {
            this.send(sender, "messages.command.tierUsage",
                    "Usage: /mine tier %mine% [setlevel (n)|addblocks (n)]",
                    "%mine%", mine.getName());
            return;
        }
        this.send(sender, "messages.mine.tierHeader", "Tier of %mine%:",
                "%mine%", mine.getName());
        if (!progression.enabled()) {
            this.send(sender, "messages.mine.tierDisabled",
                    "Progression is disabled for this mine.");
            return;
        }
        this.send(sender, "messages.mine.tierLevel",
                "Level %level%/%max%, %mined% mined blocks, next level in %remaining%.",
                "%level%", Integer.toString(progression.level()),
                "%max%", Integer.toString(progression.maxLevel()),
                "%mined%", Long.toString(progression.minedBlocks()),
                "%remaining%", Long.toString(progression.blocksToNextLevel()));
        this.send(sender, "messages.mine.tierBonus",
                "Bonus: %drop%%% drops, %sell%%% sell (%progress%% of the level).",
                "%drop%", String.format(Locale.ROOT, "%.2f", progression.dropBonus() * 100.0D),
                "%sell%", String.format(Locale.ROOT, "%.2f", progression.sellBonus() * 100.0D),
                "%progress%", String.format(Locale.ROOT, "%.1f", progression.progressPercent()));
    }


    private void handleExpand(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            this.send(sender, "messages.command.expandUsage",
                    "Usage: /mine expand (name) (blocks) [confirm]");
            return;
        }
        final Mine mine = MineManager.getMine(args[1]);
        if (mine == null) {
            this.sendMineNotFound(sender, args[1]);
            return;
        }
        final int blocks;
        try {
            blocks = Integer.parseInt(args[2]);
            if (blocks < 1 || blocks > MineExpansion.MAX_BLOCKS_PER_SIDE) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            this.send(sender, "messages.command.invalidNumber",
                    "%value% is not a valid number.", "%value%", args[2]);
            return;
        }
        switch (MineManager.checkExpansion(mine, blocks)) {
            case NOT_SUPPORTED, NO_WORLD -> {
                this.send(sender, "messages.command.expandUnsupported",
                        "Mine %mine% cannot be expanded: only cuboid mines support it.",
                        "%mine%", mine.getName());
                return;
            }
            case OVERLAP -> {
                this.send(sender, "messages.command.expandOverlap",
                        "Mine %mine% would overlap another mine.", "%mine%", mine.getName());
                return;
            }
            case TOO_LARGE -> {
                this.send(sender, "messages.command.expandTooLarge",
                        "Mine %mine% would exceed the maximum mine volume.",
                        "%mine%", mine.getName());
                return;
            }
            default -> {
            }
        }
        if (args.length < 4 || !args[3].equalsIgnoreCase("confirm")) {
            this.send(sender, "messages.command.expandConfirm",
                    "Mine %mine% would grow by %blocks% blocks per side (new volume %volume%). "
                            + "Run /mine expand %mine% %blocks% confirm.",
                    "%mine%", mine.getName(), "%blocks%", Integer.toString(blocks),
                    "%volume%", Long.toString(MineExpansion.volume(
                            MineExpansion.expand(mine.getBounds(), blocks))));
            return;
        }
        final MineManager.ExpansionResult result = MineManager.expandMine(mine, blocks);
        if (result != MineManager.ExpansionResult.OK) {
            this.send(sender, "messages.command.expandUnsupported",
                    "Mine %mine% cannot be expanded: only cuboid mines support it.",
                    "%mine%", mine.getName());
            return;
        }
        this.send(sender, "messages.command.expandDone",
                "Mine %mine% expanded by %blocks% blocks per side, new volume %volume%.",
                "%mine%", mine.getName(), "%blocks%", Integer.toString(blocks),
                "%volume%", Long.toString(MineExpansion.volume(mine.getBounds())));
    }


    private void handleCurrencies(final CommandSender sender) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        final java.util.List<CurrencyReport.Entry> entries = CurrencyReport.snapshot();
        final String tag = Messages.get("messages.command.currencyDefaultTag", " [default]");
        this.send(sender, "messages.command.currenciesHeader",
                "Registered currencies: %count% (default: %default%).",
                "%count%", Integer.toString(entries.size()),
                "%default%", CurrencyRegistry.defaultId());
        for (final CurrencyReport.Entry entry : entries) {
            this.send(sender, "messages.command.currencyEntry",
                    "- %id% (%name%): %state%%default%",
                    "%id%", entry.id(),
                    "%name%", entry.displayName(),
                    "%state%", Messages.get(entry.available()
                                    ? "messages.command.currencyAvailable"
                                    : "messages.command.currencyUnavailable",
                            entry.available() ? "available" : "unavailable"),
                    "%default%", entry.isDefault() ? tag : "");
        }
    }

    private void handleRename(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            this.send(sender, "messages.command.renameUsage",
                    "Usage: /mine rename (name) (newName)");
            return;
        }
        final Mine mine = MineManager.getMine(args[1]);
        if (mine == null) {
            this.sendMineNotFound(sender, args[1]);
            return;
        }
        final String newName = args[2];
        if (!MineManager.isValidName(newName)) {
            this.send(sender, "messages.command.invalidName",
                    "Name may contain only letters, numbers, '_' and '-' (max %max% characters).",
                    "%max%", PluginSettings.get().maxMineNameLength);
            return;
        }
        if (!MineManager.renameMine(mine, newName)) {
            this.send(sender, "messages.command.renameFailed",
                    "The mine %mine% could not be renamed to %newName%.",
                    "%mine%", mine.getName(), "%newName%", newName);
            return;
        }
        this.send(sender, "messages.command.mineRenamed",
                "Mine %old% has been renamed to %new%.",
                "%old%", args[1], "%new%", newName);
    }

    private void handleClone(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            this.send(sender, "messages.command.cloneUsage",
                    "Usage: /mine clone (name) (newName)");
            return;
        }
        final Mine source = MineManager.getMine(args[1]);
        if (source == null) {
            this.sendMineNotFound(sender, args[1]);
            return;
        }
        final String newName = args[2];
        if (!MineManager.isValidName(newName)) {
            this.send(sender, "messages.command.invalidName",
                    "Name may contain only letters, numbers, '_' and '-' (max %max% characters).",
                    "%max%", PluginSettings.get().maxMineNameLength);
            return;
        }
        mineClone(source, newName, sender);
    }

    private void mineClone(final Mine source, final String newName,
                           final CommandSender sender) {
        MineManager.cloneMine(source, newName, clone -> this.send(sender,
                        "messages.command.mineCloned",
                        "Mine %source% has been cloned to %mine%.",
                        "%source%", source.getName(), "%mine%", clone.getName()),
                reason -> this.send(sender, "messages.command.cloneFailed",
                        "The mine %mine% could not be cloned (%reason%).",
                        "%mine%", newName, "%reason%", reason));
    }

    private void handleWand(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            this.send(sender, "messages.command.playerOnly",
                    "<red>This command can only be used by a player.");
            return;
        }
        if (!this.hasPermission(sender, "galaxymines.wand")) {
            this.send(sender, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        final MineWandManager wandManager = GalaxyMines.getInstance().getWandManager();
        if (wandManager == null) {
            this.send(sender, "messages.wand.unavailable",
                    "The mine wand is not available right now.");
            return;
        }
        wandManager.give(player);
    }

    private void handleLeaderboard(final CommandSender sender, final String[] args) {
        if (!this.hasPermission(sender, "galaxymines.stats")) {
            this.send(sender, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        final StatsPeriod period = this.parsePeriod(args.length > 1 ? args[1] : null);
        if (!PlayerStatsManager.isEnabled()) {
            this.send(sender, "messages.leaderboard.disabled",
                    "<red>Statistics are disabled on this server.");
            return;
        }
        if (sender instanceof Player player) {
            new LeaderboardMenu(period).open(player);
            return;
        }
        PlayerStatsManager.leaderboard(period, LEADERBOARD_LIMIT, entries -> {
            this.send(sender, "messages.leaderboard.title", "<dark_aqua>Leaderboard");
            if (entries.isEmpty()) {
                this.send(sender, "messages.leaderboard.empty",
                        "<gray>No statistics are available yet.");
                return;
            }
            for (final String line : LeaderboardMenu.describe(period, entries)) {
                this.sendRaw(sender, line);
            }
        });
    }

    private void handlePlayerStats(final CommandSender sender, final String[] args) {
        if (!this.hasPermission(sender, "galaxymines.stats")) {
            this.send(sender, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        final Player target = this.resolveTarget(sender, args.length > 1 ? args[1] : null);
        if (target == null) {
            return;
        }
        if (!PlayerStatsManager.isEnabled()) {
            this.send(sender, "messages.stats.disabled",
                    "<red>Statistics are disabled on this server.");
            return;
        }
        final PlayerStats stats = PlayerStatsManager.profile(target);
        final LocalDate today = LocalDate.now();
        this.send(sender, "messages.stats.header", "<dark_aqua>Statistics for %player%",
                "%player%", target.getName());
        this.send(sender, "messages.stats.totalBlocks", "<gray>Blocks mined: <white>%blocks%",
                "%blocks%", String.valueOf(stats.totalBlocks()));
        this.send(sender, "messages.stats.dailyBlocks", "<gray>Blocks today: <white>%blocks%",
                "%blocks%", String.valueOf(stats.blocks(StatsPeriod.DAILY, today)));
        this.send(sender, "messages.stats.weeklyBlocks", "<gray>Blocks this week: <white>%blocks%",
                "%blocks%", String.valueOf(stats.blocks(StatsPeriod.WEEKLY, today)));
        this.send(sender, "messages.stats.soldItems", "<gray>Items sold: <white>%items%",
                "%items%", String.valueOf(stats.totalSoldItems()));
        this.send(sender, "messages.stats.earned", "<gray>Money earned: <white>%amount%",
                "%amount%", EconomyService.format(stats.totalEarned()));
    }

    private void handleEvent(final CommandSender sender, final String[] args) {
        if (!this.hasPermission(sender, "galaxymines.events")) {
            this.send(sender, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        final MineEventManager events = MineEventManager.get();
        final String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (action) {
            case "list" -> {
                final List<String> active = events.activeEvents();
                this.send(sender, "messages.events.list", "<aqua>Active events: <white>%events%",
                        "%events%", active.isEmpty()
                                ? Messages.get("messages.events.none", "none")
                                : String.join(", ", active));
            }
            case "start" -> {
                if (args.length < 3) {
                    this.send(sender, "messages.command.eventUsage",
                            "Usage: /mine event start|stop (id)");
                    return;
                }
                if (!events.start(args[2])) {
                    this.send(sender, "messages.command.eventUnknown",
                            "Unknown event %event%.", "%event%", args[2]);
                    return;
                }
                this.send(sender, "messages.command.eventStarted",
                        "Event %event% started.", "%event%", args[2]);
            }
            case "stop" -> {
                if (args.length < 3) {
                    this.send(sender, "messages.command.eventUsage",
                            "Usage: /mine event start|stop (id)");
                    return;
                }
                if (!events.stop(args[2])) {
                    this.send(sender, "messages.command.eventNotActive",
                            "The event %event% is not active.", "%event%", args[2]);
                    return;
                }
                this.send(sender, "messages.command.eventStopped",
                        "Event %event% stopped.", "%event%", args[2]);
            }
            case "reload" -> this.reloadEvents(sender);
            default -> this.send(sender, "messages.command.eventUsage",
                    "Usage: /mine event start|stop (id)|reload");
        }
    }


    private void reloadEvents(final CommandSender sender) {
        final PluginSettings settings = PluginSettings.get();
        final File file = new File(GalaxyMines.getInstance().getDataFolder(), settings.events.file);
        MineEventManager.get().load(file, settings.events.enabled);
        this.send(sender, "messages.command.eventReloaded",
                "<aqua>Loaded <white>%count%<aqua> event definition(s).",
                "%count%", String.valueOf(MineEventManager.get().knownEventCount()));
    }


    private void handleLuckyBlocks(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        final String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "reload";
        if (!action.equals("reload") && !action.equals("validate")) {
            this.send(sender, "messages.command.luckyBlocksUsage",
                    "<aqua>Usage: <white>/mine luckyblocks reload|validate");
            return;
        }
        final PluginSettings settings = PluginSettings.get();
        final File file = new File(GalaxyMines.getInstance().getDataFolder(),
                settings.luckyBlocks.file);
        final LuckyBlockParser.Result result = action.equals("validate")
                ? LuckyBlockManager.validate(file)
                : LuckyBlockManager.get().load(file, settings.luckyBlocks.enabled);
        this.send(sender, action.equals("validate")
                        ? "messages.command.luckyBlocksValidated"
                        : "messages.command.luckyBlocksReloaded",
                action.equals("validate")
                        ? "<aqua>Lucky blocks validated: <white>%tables%<aqua> table(s), <white>%mines%<aqua> mine(s), <white>%warnings%<aqua> warning(s)."
                        : "<aqua>Lucky blocks reloaded: <white>%tables%<aqua> table(s), <white>%mines%<aqua> mine(s), <white>%warnings%<aqua> warning(s).",
                "%tables%", String.valueOf(result.tables().size()),
                "%mines%", String.valueOf(result.mines().size()),
                "%warnings%", String.valueOf(result.warnings().size()));
        int shown = 0;
        for (final String warning : result.warnings()) {
            if (shown++ >= 10) {
                break;
            }
            this.send(sender, "messages.command.luckyBlocksWarning",
                    "<gray>- <white>%warning%", "%warning%", warning);
        }
    }


    private void handleBooster(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        final String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (action) {
            case "give" -> this.handleBoosterGive(sender, args);
            case "clear" -> this.handleBoosterClear(sender, args);
            case "list" -> this.handleBoosterList(sender, args);
            default -> this.send(sender, "messages.command.boosterUsage",
                    "<aqua>Usage: <white>/mine booster give <player> <sell|drop> "
                            + "<multiplier> <duration> <aqua>| <white>list [player] <aqua>| "
                            + "<white>clear <player> [sell|drop]");
        }
    }

    private void handleBoosterGive(final CommandSender sender, final String[] args) {
        if (args.length < 6) {
            this.send(sender, "messages.command.boosterUsage",
                    "<aqua>Usage: <white>/mine booster give <player> <sell|drop> "
                            + "<multiplier> <duration>");
            return;
        }
        final Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            this.send(sender, "messages.command.boosterUnknownPlayer",
                    "<red>Player <white>%player%<red> is not online.", "%player%", args[2]);
            return;
        }
        final BoosterType type = BoosterType.parse(args[3]);
        if (type == null) {
            this.send(sender, "messages.command.boosterInvalidType",
                    "<red>Unknown booster type <white>%type%<red>; use <white>sell<red> or "
                            + "<white>drop<red>.", "%type%", args[3]);
            return;
        }
        final double multiplier = BoosterParser.multiplier(args[4]);
        if (multiplier <= 0.0D) {
            this.send(sender, "messages.command.boosterInvalidMultiplier",
                    "<red>%value% is not a valid multiplier above 1.", "%value%", args[4]);
            return;
        }
        final int seconds = BoosterParser.durationSeconds(args[5]);
        if (seconds <= 0) {
            this.send(sender, "messages.command.boosterInvalidDuration",
                    "<red>%value% is not a valid duration; use seconds or 30m, 2h, 7d.",
                    "%value%", args[5]);
            return;
        }
        final PlayerBooster booster = BoosterService.get()
                .give(target.getUniqueId(), type, multiplier, seconds);
        this.send(sender, "messages.command.boosterGiven",
                "<green>Gave <white>%player%<green> a <white>x%multiplier% %type%<green> "
                        + "booster for <white>%duration%<green>.",
                "%player%", target.getName(),
                "%multiplier%", String.format(Locale.ROOT, "%.2f", booster.multiplier()),
                "%type%", type.name().toLowerCase(Locale.ROOT),
                "%duration%", BoosterParser.formatDuration(seconds));
    }

    private void handleBoosterList(final CommandSender sender, final String[] args) {
        final java.util.UUID uuid;
        final String name;
        if (args.length >= 3) {
            final Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                this.send(sender, "messages.command.boosterUnknownPlayer",
                        "<red>Player <white>%player%<red> is not online.", "%player%", args[2]);
                return;
            }
            uuid = target.getUniqueId();
            name = target.getName();
        } else if (sender instanceof Player player) {
            uuid = player.getUniqueId();
            name = player.getName();
        } else {
            this.send(sender, "messages.command.boosterUsage",
                    "<aqua>Usage: <white>/mine booster list <player>");
            return;
        }
        final List<PlayerBooster> active = BoosterService.get().active(uuid);
        if (active.isEmpty()) {
            this.send(sender, "messages.command.boosterNone",
                    "<gray>No active booster for <white>%player%<gray>.", "%player%", name);
            return;
        }
        for (final PlayerBooster booster : active) {
            this.send(sender, "messages.command.boosterLine",
                    "<gray>- <white>x%multiplier% %type% <gray>for <white>%remaining%",
                    "%multiplier%", String.format(Locale.ROOT, "%.2f", booster.multiplier()),
                    "%type%", booster.type().name().toLowerCase(Locale.ROOT),
                    "%remaining%", BoosterParser.formatDuration(
                            BoosterService.get().remainingSeconds(uuid, booster.type())));
        }
    }

    private void handleBoosterClear(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            this.send(sender, "messages.command.boosterUsage",
                    "<aqua>Usage: <white>/mine booster clear <player> [sell|drop]");
            return;
        }
        final Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            this.send(sender, "messages.command.boosterUnknownPlayer",
                    "<red>Player <white>%player%<red> is not online.", "%player%", args[2]);
            return;
        }
        final BoosterType type = args.length >= 4 ? BoosterType.parse(args[3]) : null;
        if (args.length >= 4 && type == null) {
            this.send(sender, "messages.command.boosterInvalidType",
                    "<red>Unknown booster type <white>%type%<red>; use <white>sell<red> or "
                            + "<white>drop<red>.", "%type%", args[3]);
            return;
        }
        final int removed = type == null
                ? BoosterService.get().clearAll(target.getUniqueId())
                : (BoosterService.get().clear(target.getUniqueId(), type) ? 1 : 0);
        if (removed == 0) {
            this.send(sender, "messages.command.boosterNone",
                    "<gray>No active booster for <white>%player%<gray>.",
                    "%player%", target.getName());
            return;
        }
        this.send(sender, "messages.command.boosterCleared",
                "<green>Removed <white>%amount%<green> booster(s) from <white>%player%<green>.",
                "%amount%", Integer.toString(removed), "%player%", target.getName());
    }


    private void handleHistory(final CommandSender sender, final String[] args) {
        if (!this.hasPermission(sender, "galaxymines.stats")) {
            this.send(sender, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        if (args.length > 1 && !this.requireAdmin(sender)) {
            return;
        }
        final Player target = this.resolveTarget(sender, args.length > 1 ? args[1] : null);
        if (target == null) {
            return;
        }
        final List<HistoryEntry> entries = HistoryService.get().entries(target.getUniqueId());
        if (entries.isEmpty()) {
            this.send(sender, "messages.history.empty",
                    "<gray>No sales recorded for <white>%player%<gray>.",
                    "%player%", target.getName());
            return;
        }
        this.send(sender, "messages.history.header",
                "<aqua>Last sales of <white>%player%<aqua> (<white>%count%<aqua>):",
                "%player%", target.getName(),
                "%count%", Integer.toString(entries.size()));
        final java.time.format.DateTimeFormatter format =
                java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm");
        int shown = 0;
        for (final HistoryEntry entry : entries) {
            if (shown++ >= 10) {
                break;
            }
            this.send(sender, "messages.history.line",
                    "<gray>%time% <dark_gray>| <white>%amount% <gray>(%items% items, %mine%)",
                    "%time%", format.format(java.time.Instant.ofEpochMilli(entry.time())
                            .atZone(java.time.ZoneId.systemDefault())),
                    "%amount%", EconomyService.format(entry.amount()),
                    "%items%", Integer.toString(entry.items()),
                    "%mine%", entry.mine().isBlank() ? "-" : entry.mine());
        }
    }


    private void handleDaily(final CommandSender sender, final String[] args) {
        if (args.length > 1 && "reset".equalsIgnoreCase(args[1])) {
            if (!this.requireAdmin(sender)) {
                return;
            }
            if (args.length < 3) {
                this.send(sender, "messages.daily.usage",
                        "<aqua>Usage: <white>/mine daily <aqua>| "
                                + "<white>/mine daily reset <player>");
                return;
            }
            final Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                this.send(sender, "messages.command.unknownPlayer",
                        "<red>Player <white>%player%<red> is not online.",
                        "%player%", args[2]);
                return;
            }
            DailyRewardService.get().reset(target.getUniqueId());
            this.send(sender, "messages.daily.reset",
                    "<green>Daily reward state of <white>%player%<green> was reset.",
                    "%player%", target.getName());
            return;
        }
        if (!(sender instanceof Player player)) {
            this.send(sender, "messages.command.playerOnly",
                    "<red>This command can only be used by a player.");
            return;
        }
        if (!this.hasPermission(sender, "galaxymines.daily")) {
            this.send(sender, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        final DailyRewardService service = DailyRewardService.get();
        if (!service.isEnabled()) {
            this.send(sender, "messages.daily.disabled",
                    "<red>Daily rewards are disabled on this server.");
            return;
        }
        final DailyRewardService.Result result =
                service.claim(player.getUniqueId(), player.getName());
        if (!result.claimed()) {
            this.send(sender, "messages.daily.cooldown",
                    "<red>You already claimed today's reward. Next in <white>%time%<red>.",
                    "%time%", result.remainingText());
            return;
        }
        final DailyRewardService.Reward reward = result.reward();
        if (reward.money() > 0.0D) {
            if (EconomyService.deposit(player, reward.money())) {
                this.send(sender, "messages.daily.money",
                        "<green>Daily reward: <white>%money%<green>.",
                        "%money%", EconomyService.format(reward.money()));
            } else {
                this.send(sender, "messages.daily.noEconomy",
                        "<red>The money part could not be paid: no economy provider.");
            }
        }
        for (final String raw : reward.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    raw.replace("%player%", player.getName())
                            .replace("%uuid%", player.getUniqueId().toString()));
        }
        this.send(sender, "messages.daily.claimed",
                "<green>Day <white>%streak%<green> reward claimed!",
                "%streak%", Integer.toString(result.streak()));
    }


    private void handleQuests(final CommandSender sender, final String[] args) {
        if (args.length > 1 && "reset".equalsIgnoreCase(args[1])) {
            if (!this.requireAdmin(sender)) {
                return;
            }
            if (args.length < 3) {
                this.send(sender, "messages.quests.usage",
                        "<aqua>Usage: <white>/mine quests [player] <aqua>| "
                                + "<white>/mine quests reset <player>");
                return;
            }
            final Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                this.send(sender, "messages.command.unknownPlayer",
                        "<red>Player <white>%player%<red> is not online.",
                        "%player%", args[2]);
                return;
            }
            QuestService.get().reset(target.getUniqueId());
            this.send(sender, "messages.quests.reset",
                    "<green>Quest progress of <white>%player%<green> was reset.",
                    "%player%", target.getName());
            return;
        }
        if (!this.hasPermission(sender, "galaxymines.stats")) {
            this.send(sender, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        final Player target = this.resolveTarget(sender, args.length > 1 ? args[1] : null);
        if (target == null) {
            return;
        }
        final QuestService service = QuestService.get();
        if (!service.isEnabled()) {
            this.send(sender, "messages.quests.disabled",
                    "<red>Quests are disabled on this server.");
            return;
        }
        this.send(sender, "messages.quests.header",
                "<aqua>Quests of <white>%player%<aqua>:", "%player%", target.getName());
        for (final QuestService.Status status : service.status(target.getUniqueId())) {
            final QuestDefinition quest = status.quest();
            if (status.claimed()) {
                this.send(sender, "messages.quests.done",
                        "<green>✔ <gray>%name% <green>(claimed: %reward%)",
                        "%name%", quest.displayName(),
                        "%reward%", EconomyService.format(quest.rewardMoney()));
                continue;
            }
            this.send(sender, "messages.quests.line",
                    "<gray>• <white>%name% <gray>%progress%<dark_gray>/<gray>%target% "
                            + "<dark_gray>| <green>%reward%",
                    "%name%", quest.displayName(),
                    "%progress%", Long.toString(status.progress()),
                    "%target%", Long.toString(quest.target()),
                    "%reward%", EconomyService.format(quest.rewardMoney()));
        }
    }


    private void handlePass(final CommandSender sender, final String[] args) {
        final BattlePassService service = BattlePassService.get();
        if (!service.isEnabled()) {
            this.send(sender, "messages.battlepass.disabled",
                    "<red>The battle pass is disabled on this server.");
            return;
        }
        if (args.length > 1 && "claim".equalsIgnoreCase(args[1])) {
            if (!(sender instanceof Player player)) {
                this.send(sender, "messages.command.playerOnly",
                        "<red>This command can only be used by a player.");
                return;
            }
            final int before = service.status(player.getUniqueId()).stream()
                    .mapToInt(status -> status.claimed() ? 1 : 0).sum();
            service.claim(player);
            final int after = service.status(player.getUniqueId()).stream()
                    .mapToInt(status -> status.claimed() ? 1 : 0).sum();
            if (after == before) {
                this.send(sender, "messages.battlepass.nothingToClaim",
                        "<red>No battle pass tier is ready to claim.");
            }
            return;
        }
        if (args.length > 1 && "addxp".equalsIgnoreCase(args[1])) {
            if (!this.requireAdmin(sender)) {
                return;
            }
            if (args.length < 4) {
                this.send(sender, "messages.battlepass.usage",
                        "<aqua>Usage: <white>/mine pass [claim] <aqua>| "
                                + "<white>/mine pass addxp <player> <amount> <aqua>| "
                                + "<white>/mine pass reset <player>");
                return;
            }
            final Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                this.send(sender, "messages.command.unknownPlayer",
                        "<red>Player <white>%player%<red> is not online.",
                        "%player%", args[2]);
                return;
            }
            final long amount;
            try {
                amount = Long.parseLong(args[3]);
            } catch (NumberFormatException exception) {
                this.send(sender, "messages.battlepass.usage",
                        "<aqua>Usage: <white>/mine pass addxp <player> <amount>");
                return;
            }
            final long xp = service.addXp(target.getUniqueId(), Math.max(1L, amount));
            this.send(sender, "messages.battlepass.addxp",
                    "<green>Gave <white>%amount%<green> battle pass XP to <white>%player% "
                            + "<green>(total <white>%xp%<green>).",
                    "%amount%", Long.toString(amount),
                    "%player%", target.getName(),
                    "%xp%", Long.toString(xp));
            return;
        }
        if (args.length > 1 && "reset".equalsIgnoreCase(args[1])) {
            if (!this.requireAdmin(sender)) {
                return;
            }
            if (args.length < 3) {
                this.send(sender, "messages.battlepass.usage",
                        "<aqua>Usage: <white>/mine pass reset <player>");
                return;
            }
            final Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                this.send(sender, "messages.command.unknownPlayer",
                        "<red>Player <white>%player%<red> is not online.",
                        "%player%", args[2]);
                return;
            }
            service.reset(target.getUniqueId());
            this.send(sender, "messages.battlepass.reset",
                    "<green>The battle pass progress of <white>%player%<green> was reset.",
                    "%player%", target.getName());
            return;
        }
        final Player player = sender instanceof Player online ? online : null;
        if (player == null) {
            this.send(sender, "messages.command.playerOnly",
                    "<red>This command can only be used by a player.");
            return;
        }
        this.send(sender, "messages.battlepass.header",
                "<aqua>Battle pass <white>%season%<aqua>: <white>%xp%<aqua> XP, "
                        + "level <white>%level%",
                "%season%", service.season(),
                "%xp%", Long.toString(service.xp(player.getUniqueId())),
                "%level%", Integer.toString(service.level(player.getUniqueId())));
        for (final BattlePassService.TierStatus status : service.status(player.getUniqueId())) {
            final String state = status.claimed() ? "<green>claimed"
                    : status.unlocked() ? "<yellow>ready" : "<dark_gray>locked";
            this.send(sender, "messages.battlepass.line",
                    "<gray>• <white>%level% <gray>[%state%<gray>] <dark_gray>| "
                            + "<green>%money% <gray>+ %commands%",
                    "%level%", Integer.toString(status.tier().level()),
                    "%state%", state,
                    "%money%", EconomyService.format(status.tier().money()),
                    "%commands%", Integer.toString(status.tier().commands().size()));
        }
    }


    private void handleBomb(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 4 || !"give".equalsIgnoreCase(args[1])) {
            this.send(sender, "messages.bomb.usage",
                    "<aqua>Usage: <white>/mine bomb give <player> [radius]");
            return;
        }
        final Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            this.send(sender, "messages.command.unknownPlayer",
                    "<red>Player <white>%player%<red> is not online.",
                    "%player%", args[2]);
            return;
        }
        int radius = 4;
        if (args.length > 3) {
            try {
                radius = Integer.parseInt(args[3]);
            } catch (NumberFormatException exception) {
                this.send(sender, "messages.bomb.usage",
                        "<aqua>Usage: <white>/mine bomb give <player> [radius]");
                return;
            }
        }
        radius = BombService.get().clampRadius(radius);
        final ItemStack bomb = BombItem.create(radius);
        for (final ItemStack leftover : target.getInventory().addItem(bomb).values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover);
        }
        this.send(sender, "messages.bomb.given",
                "<green>Gave a radius <white>%radius%<green> bomb to <white>%player%<green>.",
                "%radius%", Integer.toString(radius), "%player%", target.getName());
    }


    private void handleAutoMine(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            this.send(sender, "messages.command.playerOnly",
                    "<red>This command can only be used by a player.");
            return;
        }
        final AutoMinerService service = AutoMinerService.get();
        if (!service.settings().enabled()) {
            this.send(sender, "messages.autominer.disabled",
                    "<red>The auto-miner is disabled on this server.");
            return;
        }
        if (!this.hasPermission(sender, "galaxymines.autominer")) {
            this.send(sender, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        final boolean enable = args.length > 1
                ? !"off".equalsIgnoreCase(args[1]) : !service.isActive(player.getUniqueId());
        final boolean active = service.setActive(player, enable);
        final AutoMinerSettings settings = service.settings();
        if (active) {
            this.send(sender, "messages.autominer.enabled",
                    "<green>Auto-miner on: <white>%blocks%<green> blocks every "
                            + "<white>%seconds%<green>s around you.",
                    "%blocks%", Integer.toString(settings.maxBlocksPerCycle()),
                    "%seconds%", Integer.toString(settings.intervalSeconds()));
        } else {
            this.send(sender, "messages.autominer.disabledPlayer",
                    "<yellow>Auto-miner off.");
        }
    }

    private void handleReload(final CommandSender sender) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        GalaxyMines.getInstance().reload(sender instanceof Player player ? player : null);
    }

    private void handleList(final CommandSender sender) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        this.listMines(sender);
    }

    private void handleGui(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            this.send(sender, "messages.command.playerOnly",
                    "<red>This command can only be used by a player.");
            return;
        }
        if (!this.requireAdmin(sender)) {
            return;
        }
        GUIManager.mainMenu.open(player);
    }


    private void handlePriceGui(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            this.send(sender, "messages.command.playersOnly",
                    "<red>Only players can use this command.");
            return;
        }
        if (!player.hasPermission("galaxymines.prices.edit")
                && !player.hasPermission("galaxymines.admin")) {
            this.send(sender, "messages.command.noPermission",
                    "<red>You do not have permission to edit prices.");
            return;
        }
        java.io.File file = PriceRegistry.source();
        if (file == null) {
            file = new java.io.File(
                    me.marcdoesntexist.galaxymines.GalaxyMines.getInstance().getDataFolder(),
                    "prices.yml");
        }
        me.marcdoesntexist.galaxymines.gui.menus.PriceEditorMenu.open(player, file);
    }


    private void handleMilestones(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        final String name;
        if (args.length >= 2) {
            name = args[1];
        } else if (sender instanceof Player player) {
            name = player.getName();
        } else {
            this.send(sender, "milestonesView.usage",
                    "<red>Usage: /mine milestones <player>");
            return;
        }
        final Player online = org.bukkit.Bukkit.getPlayerExact(name);
        final org.bukkit.OfflinePlayer target =
                online != null ? online : org.bukkit.Bukkit.getOfflinePlayer(name);
        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            this.send(sender, "milestonesView.unknown",
                    "<red>No player named <white>%player%<red> has played here.",
                    "%player%", name);
            return;
        }
        final me.marcdoesntexist.galaxymines.milestone.MilestoneService.Status status =
                me.marcdoesntexist.galaxymines.milestone.MilestoneService.get()
                        .status(target.getUniqueId());
        final String display = target.getName() == null ? name : target.getName();
        this.send(sender, "milestonesView.header",
                "<aqua>Milestones for <white>%player%<aqua>: <white>%total%<aqua> blocks, "
                        + "<white>%count%<aqua> threshold(s), <white>%state%<aqua>.",
                "%player%", display,
                "%total%", Long.toString(status.total()),
                "%count%", Integer.toString(status.milestones().size()),
                "%state%", status.enabled()
                        ? Messages.get("milestonesView.enabled", "enabled")
                        : Messages.get("milestonesView.disabled", "disabled"));
        if (status.allReached()) {
            this.send(sender, "milestonesView.done",
                    "<aqua>Next: <white>none<aqua>, every threshold has been reached.");
        } else {
            this.send(sender, "milestonesView.next",
                    "<aqua>Next: <white>%blocks%<aqua> blocks (<white>%remaining%<aqua> to go).",
                    "%blocks%", Long.toString(status.nextThreshold()),
                    "%remaining%", Long.toString(status.remaining()));
        }
        if (status.milestones().isEmpty()) {
            this.send(sender, "milestonesView.none", "<gray>No thresholds configured.");
            return;
        }
        for (final me.marcdoesntexist.galaxymines.milestone.Milestone milestone
                : status.milestones()) {
            this.send(sender, "milestonesView.threshold",
                    "<gray>[%state%] <white>%blocks%<gray> blocks",
                    "%state%", status.isPaid(milestone.blocks()) ? "x" : " ",
                    "%blocks%", Long.toString(milestone.blocks()));
        }
    }


    private void handlePrices(final CommandSender sender, final String[] args) {
        if (args.length >= 2 && "gui".equalsIgnoreCase(args[1])) {
            this.handlePriceGui(sender);
            return;
        }
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length >= 2) {
            final String action = args[1].toLowerCase(Locale.ROOT);
            if ("set".equals(action) || "remove".equals(action)) {
                this.handlePriceEdit(sender, action, args);
                return;
            }
        }
        int materials = 0;
        for (final String key : PriceRegistry.snapshot().keySet()) {
            if (!key.contains(":")) {
                ++materials;
            }
        }
        final List<String> unresolved = PriceRegistry.unresolvedCustomKeys();
        this.send(sender, "messages.command.pricesHeader",
                "<aqua>Prices: <white>%materials%<aqua> material key(s), <white>%custom%<aqua> "
                        + "provider key(s), <white>%unresolved%<aqua> unresolved.",
                "%materials%", String.valueOf(materials),
                "%custom%", String.valueOf(PriceRegistry.customKeyCount()),
                "%unresolved%", String.valueOf(unresolved.size()));
        if (PriceRegistry.source() != null) {
            this.send(sender, "messages.command.pricesSource", "<gray>File: <white>%file%",
                    "%file%", PriceRegistry.source().getName());
        }
        for (final String key : unresolved) {
            this.send(sender, "messages.command.pricesUnresolved",
                    "<red>- %key%<gray> (provider missing or unknown item id)",
                    "%key%", key);
        }
    }


    private void handlePriceEdit(final CommandSender sender, final String action,
                                 final String[] args) {
        if (args.length < 3) {
            this.send(sender, "messages.command.priceUsage",
                    "<red>Usage: /mine prices set <key> <price> | /mine prices remove <key>");
            return;
        }
        final String key = args[2];
        final java.io.File file = new java.io.File(GalaxyMines.getInstance().getDataFolder(),
                PluginSettings.get().economy.pricesFile);
        try {
            if ("remove".equals(action)) {
                if (!PriceFileEditor.remove(file, key)) {
                    this.send(sender, "messages.command.priceUnknown",
                            "<red>%key% is not configured in prices.yml.", "%key%", key);
                    return;
                }
                PriceRegistry.load(file);
                this.send(sender, "messages.command.priceRemoved",
                        "<green>Removed <white>%key%<green> from prices.yml.",
                        "%key%", PriceFileEditor.normalize(key));
                return;
            }
            if (args.length < 4) {
                this.send(sender, "messages.command.priceUsage",
                        "<red>Usage: /mine prices set <key> <price> | /mine prices remove <key>");
                return;
            }
            final double value;
            try {
                value = Double.parseDouble(args[3]);
            } catch (NumberFormatException exception) {
                this.send(sender, "messages.command.priceInvalid",
                        "<red>%value% is not a valid price.", "%value%", args[3]);
                return;
            }
            if (!Double.isFinite(value) || value <= 0.0D) {
                this.send(sender, "messages.command.priceInvalid",
                        "<red>The price must be a positive number.");
                return;
            }
            if (!PriceFileEditor.set(file, key, value)) {
                this.send(sender, "messages.command.priceInvalid",
                        "<red>%value% is not a valid price.", "%value%", args[3]);
                return;
            }
            PriceRegistry.load(file);
            this.send(sender, "messages.command.priceSet",
                    "<green>Set <white>%key%<green> to <white>%value%<green> in prices.yml.",
                    "%key%", PriceFileEditor.normalize(key),
                    "%value%", Double.toString(value));
        } catch (java.io.IOException exception) {
            this.send(sender, "messages.command.priceWriteFailed",
                    "<red>Could not write prices.yml: %error%",
                    "%error%", exception.getMessage() == null
                            ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }


    private void handleProviders(final CommandSender sender) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        final List<ProviderVersions.Status> statuses = ProviderVersions.status(this.owner);
        int loaded = 0;
        for (final ProviderVersions.Status status : statuses) {
            if (CustomBlockRegistry.isAvailable(status.namespace())) {
                ++loaded;
            }
        }
        this.send(sender, "messages.command.providersHeader",
                "<aqua>Custom providers: <white>%loaded%<aqua>/%total% adapter(s) active",
                "%loaded%", String.valueOf(loaded),
                "%total%", String.valueOf(statuses.size()));
        for (final ProviderVersions.Status status : statuses) {
            if (!status.available()) {
                this.send(sender, "messages.command.providersMissing",
                        "<gray>- <white>%name%<gray>: not installed, tested minimum "
                                + "<white>%minimum%",
                        "%name%", status.pluginName(),
                        "%minimum%", status.minimum());
                continue;
            }
            final String adapter = CustomBlockRegistry.isAvailable(status.namespace())
                    ? "loaded" : "unavailable";
            this.send(sender, "messages.command.providersEntry",
                    "<gray>- <white>%name% <white>%version%<gray>: %state%, adapter "
                            + "<white>%adapter%<gray>, tested minimum <white>%minimum%",
                    "%name%", status.pluginName(),
                    "%version%", status.installedVersion(),
                    "%state%", !status.enabled() ? "disabled"
                            : status.outdated() ? "older than tested minimum" : "supported",
                    "%adapter%", adapter,
                    "%minimum%", status.minimum());
        }
        final List<String> priceIds = PriceRegistry.unresolvedCustomKeys();
        final List<String> eventIds = ProviderDiagnostics.unresolvedEvents(
                MineEventManager.get().definitions(),
                id -> CustomBlockRegistry.item(id) != null);
        final List<String> luckyIds = ProviderDiagnostics.unresolvedLuckyBlocks(
                LuckyBlockManager.get().tables(),
                id -> CustomBlockRegistry.item(id) != null);
        final int unresolvedCount = priceIds.size() + eventIds.size() + luckyIds.size();
        this.send(sender, "messages.command.providersUnresolvedHeader",
                "<aqua>Unresolved provider ids: <white>%count%",
                "%count%", String.valueOf(unresolvedCount));
        for (final String key : priceIds) {
            this.send(sender, "messages.command.providersUnresolvedEntry",
                    "<red>- <white>%entry%", "%entry%", "prices.yml: " + key);
        }
        for (final String entry : eventIds) {
            this.send(sender, "messages.command.providersUnresolvedEntry",
                    "<red>- <white>%entry%", "%entry%", "events.yml: " + entry);
        }
        for (final String entry : luckyIds) {
            this.send(sender, "messages.command.providersUnresolvedEntry",
                    "<red>- <white>%entry%", "%entry%", "luckyblocks.yml: " + entry);
        }
    }


    private void handleDebugTimings(final CommandSender sender) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        final PluginSettings.Timings settings = PluginSettings.get().timings;
        this.send(sender, "messages.command.debugTimingsHeader", "<aqua>Reset timings");
        if (!settings.enabled) {
            this.send(sender, "messages.command.debugTimingsDisabled",
                    "<yellow>Timing collection is disabled (settings.timings.enabled: false).");
        }
        this.send(sender, "messages.command.debugTimingsLastTick",
                "<gray>Reset tick: <white>%blocks%<gray> blocks in <white>%millis%<gray> ms, "
                        + "<white>%sessions%<gray> active reset(s), <white>%infinite%<gray> "
                        + "infinite respawn(s) pending",
                "%blocks%", String.valueOf(MineRepopulator.lastTickBlocks()),
                "%millis%", String.format(Locale.ROOT, "%.2f", MineRepopulator.lastTickMillis()),
                "%sessions%", String.valueOf(MineRepopulator.activeSessions()),
                "%infinite%", String.valueOf(InfiniteRespawnService.pendingTotal()));
        this.send(sender, "messages.command.debugTimingsServer",
                "<gray>Server MSPT: <white>%avg%<gray> avg, <white>%p95%<gray> p95 over "
                        + "<white>%samples%<gray> tick(s)",
                "%avg%", String.format(Locale.ROOT, "%.2f", TickMonitor.averageMillis()),
                "%p95%", String.format(Locale.ROOT, "%.2f", TickMonitor.p95Millis()),
                "%samples%", String.valueOf(TickMonitor.samples()));
        int shown = 0;
        for (final Mine mine : MineManager.getMines()) {
            final ResetMetrics metrics = ResetMetrics.find(mine.getName());
            if (metrics == null || metrics.resets() == 0L) {
                continue;
            }
            final ResetMetrics.Snapshot snapshot = metrics.snapshot();
            this.send(sender, "messages.command.debugTimingsMine",
                    "<gray>- <white>%mine%<gray>: <white>%resets%<gray> reset(s), last "
                            + "<white>%lastblocks%<gray> blocks in <white>%seconds%<gray>s "
                            + "(<white>%avg%<gray> ms avg, <white>%p95%<gray> ms p95, "
                            + "<white>%max%<gray> ms max, <white>%rate%<gray> blocks/s)",
                    "%mine%", mine.getName(),
                    "%resets%", String.valueOf(snapshot.resets()),
                    "%lastblocks%", String.valueOf(snapshot.lastBlocks()),
                    "%seconds%", String.format(Locale.ROOT, "%.2f",
                            snapshot.lastDurationSeconds()),
                    "%avg%", String.format(Locale.ROOT, "%.2f", snapshot.sliceAverageMillis()),
                    "%p95%", String.format(Locale.ROOT, "%.2f", snapshot.sliceP95Millis()),
                    "%max%", String.format(Locale.ROOT, "%.2f", snapshot.sliceMaxMillis()),
                    "%rate%", String.format(Locale.ROOT, "%.0f", snapshot.blocksPerSecond()));
            ++shown;
        }
        if (shown == 0) {
            this.send(sender, "messages.command.debugTimingsEmpty",
                    "<gray>No completed reset has been measured yet.");
        }
    }

    private void handleDebug(final CommandSender sender, final String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("timings")) {
            this.handleDebugTimings(sender);
            return;
        }
        this.send(sender, "messages.command.debugHeader", "<aqua>GalaxyMines diagnostics");
        this.send(sender, "messages.command.debugVersion", "<gray>Version: <white>%version%",
                "%version%", GalaxyMines.getInstance().getDescription().getVersion());
        final me.marcdoesntexist.galaxymines.multiplier.MultiplierSources sources =
                me.marcdoesntexist.galaxymines.multiplier.MultiplierSources.get();
        this.send(sender, "messages.command.debugMultipliers",
                "<gray>Multipliers: <white>%state%<gray>, sources <white>%sources%<gray>, "
                        + "caps <white>%drop%<gray>/<white>%sell%<gray>, you <white>%current%",
                "%state%", sources.isEnabled() ? "enabled" : "disabled",
                "%sources%", sources.summary(),
                "%drop%", String.format(Locale.ROOT, "%.2f", sources.dropCap()),
                "%sell%", String.format(Locale.ROOT, "%.2f", sources.sellCap()),
                "%current%", sender instanceof org.bukkit.entity.Player player
                        ? sources.describe(player)
                        : "n/a");
        final List<Mine> mines = MineManager.getMines();
        int activeResets = 0;
        int paused = 0;
        int instant = 0;
        int wave = 0;
        for (final Mine mine : mines) {
            if (mine.isResetInProgress()) {
                ++activeResets;
            }
            if (mine.isPaused()) {
                ++paused;
            }
            if (mine.getResetStyle() == MineResetStyle.WAVE) {
                ++wave;
            } else {
                ++instant;
            }
        }
        final PluginSettings.Reset reset = PluginSettings.get().reset;
        this.send(sender, "messages.command.debugMines", "<gray>Loaded mines: <white>%mines%",
                "%mines%", String.valueOf(mines.size()));
        this.send(sender, "messages.command.debugIndex",
                "<gray>Index: <white>%worlds%<gray> worlds, <white>%chunks%<gray> chunks, "
                        + "<white>%entries%<gray> entries",
                "%worlds%", String.valueOf(MineIndex.worldCount()),
                "%chunks%", String.valueOf(MineIndex.chunkCount()),
                "%entries%", String.valueOf(MineIndex.entryCount()));
        this.send(sender, "messages.command.debugResets",
                "<gray>Resets: <white>%active%<gray> active, <white>%paused%<gray> paused, "
                        + "<white>%instant%<gray> instant, <white>%wave%<gray> wave",
                "%active%", String.valueOf(activeResets), "%paused%", String.valueOf(paused),
                "%instant%", String.valueOf(instant), "%wave%", String.valueOf(wave));
        this.send(sender, "messages.command.debugResetBudget",
                "<gray>Reset budget: <white>%blocks%<gray> blocks/tick, <white>%millis%<gray> "
                        + "ms/tick shared by all resets; last tick <white>%lastblocks%<gray> blocks in "
                        + "<white>%lastmillis%<gray> ms",
                "%blocks%", String.valueOf(reset.blocksPerTick),
                "%millis%", String.format(Locale.ROOT, "%.1f",
                        reset.globalNanosPerTick / 1_000_000.0D),
                "%lastblocks%", String.valueOf(MineRepopulator.lastTickBlocks()),
                "%lastmillis%", String.format(Locale.ROOT, "%.2f",
                        MineRepopulator.lastTickMillis()));
        final MineWandManager wandManager = GalaxyMines.getInstance().getWandManager();
        this.send(sender, "messages.command.debugCaches",
                "<gray>Caches: <white>%profiles%<gray> profiles (<white>%loading%<gray> "
                        + "loading), <white>%boards%<gray> leaderboards, <white>%smelt%<gray> "
                        + "smelt / <white>%block%<gray> block recipes",
                "%profiles%", String.valueOf(PlayerStatsManager.cachedProfileCount()),
                "%loading%", String.valueOf(PlayerStatsManager.loadingProfileCount()),
                "%boards%", String.valueOf(PlayerStatsManager.cachedLeaderboardCount()),
                "%smelt%", String.valueOf(MineDropProcessor.smeltRecipeCount()),
                "%block%", String.valueOf(MineDropProcessor.blockRecipeCount()));
        this.send(sender, "messages.command.debugWand",
                "<gray>Wand selections: <white>%selections%<gray>, pending teleports: "
                        + "<white>%teleports%",
                "%selections%", String.valueOf(wandManager == null ? 0
                        : wandManager.selectionCount()),
                "%teleports%", String.valueOf(TeleportService.pendingCount()));
        this.send(sender, "messages.command.debugSuspended",
                "<gray>Suspended worlds: <white>%worlds%<gray>, mines: <white>%mines%",
                "%worlds%", String.valueOf(MineManager.suspendedWorldCount()),
                "%mines%", String.valueOf(MineManager.suspendedMineCount()));
        this.send(sender, "messages.command.debugEconomy",
                "<gray>Economy enabled: <white>%enabled%<gray>, provider available: <white>%available%",
                "%enabled%", String.valueOf(PluginSettings.get().economy.enabled),
                "%available%", String.valueOf(EconomyService.isAvailable()));
        this.send(sender, "messages.command.debugStatistics",
                "<gray>Statistics enabled: <white>%enabled%",
                "%enabled%", String.valueOf(PlayerStatsManager.isEnabled()));
        this.send(sender, "messages.command.debugEvents",
                "<gray>Active events: <white>%events%",
                "%events%", MineEventManager.get().activeEvents().isEmpty()
                        ? Messages.get("messages.events.none", "none")
                        : String.join(", ", MineEventManager.get().activeEvents()));
        this.send(sender, "messages.command.debugEventDefinitions",
                "<gray>Event definitions: <white>%events%",
                "%events%", String.valueOf(MineEventManager.get().knownEventCount()));
        final me.marcdoesntexist.galaxymines.holograms.OrphanCleanupTask cleanup =
                me.marcdoesntexist.galaxymines.holograms.OrphanCleanupTask.get();
        this.send(sender, "messages.command.debugHolograms",
                "<gray>Holograms: <white>%enabled%<gray>, orphan cleanup <white>%when%<gray> ago, "
                        + "removed <white>%removed%",
                "%enabled%",
                String.valueOf(me.marcdoesntexist.galaxymines.config.GalaxyMinesConfiguration.useHolograms),
                "%when%", cleanup.lastRunRelative(),
                "%removed%", cleanup.lastRemoved() < 0 ? "-" : String.valueOf(cleanup.lastRemoved()));
        if (sender instanceof Player player) {
            final Mine mine = MineManager.getMineAt(player.getLocation());
            this.send(sender, "messages.command.debugCurrentMine",
                    "<gray>Current mine: <white>%mine%",
                    "%mine%", mine == null
                            ? Messages.get("messages.command.debugOutside", "outside a mine")
                            : mine.getName());
        }
    }

    private Region selectionFor(final Player player) {
        final Region worldEditSelection = WorldEditHook.getSelection(player);
        if (worldEditSelection != null) {
            return worldEditSelection;
        }
        final MineWandManager wandManager = GalaxyMines.getInstance().getWandManager();
        return wandManager == null ? null : wandManager.selection(player);
    }

    private Player resolveTarget(final CommandSender sender, final String name) {
        if (name == null || name.isBlank()) {
            if (sender instanceof Player player) {
                return player;
            }
            this.send(sender, "messages.command.specifyPlayer", "Specify a player name.");
            return null;
        }
        final Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            this.send(sender, "messages.command.playerNotFound",
                    "Could not find player %player%.", "%player%", name);
        }
        return target;
    }

    private StatsPeriod parsePeriod(final String raw) {
        if (raw == null || raw.isBlank()) {
            return StatsPeriod.ALL_TIME;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "daily", "day" -> StatsPeriod.DAILY;
            case "weekly", "week" -> StatsPeriod.WEEKLY;
            default -> StatsPeriod.ALL_TIME;
        };
    }

    private boolean requireAdmin(final CommandSender sender) {
        if (this.hasPermission(sender, ADMIN_PERMISSION)) {
            return true;
        }
        this.send(sender, "messages.command.noPermission",
                "You do not have permission to use this command.");
        return false;
    }

    private boolean hasPermission(final CommandSender sender, final String permission) {
        return sender.hasPermission(permission) || sender.hasPermission(ADMIN_PERMISSION);
    }

    private void createNewMine(final Player p, final String name, final Region selection,
                               final int resetDelay, final boolean broadcastOnReset,
                               final boolean nearbyBroadcast, final double broadcastRadius,
                               final String customMessage) {
        if (!this.checkPerm(p)) {
            this.send(p, "messages.command.noPermission",
                    "You do not have permission to use this command.");
            return;
        }
        Mine m = null;
        this.send(p, "messages.command.regionInfo",
                "WorldEdit region: %region%", "%region%",
                selection.getClass().getSimpleName());
        if (selection instanceof CylinderRegion cylinder) {
            final Vector3 cylinderCenter = cylinder.getCenter();
            final Location center = new Location(p.getWorld(), cylinderCenter.x(),
                    cylinder.getMinimumY(), cylinderCenter.z());
            final int height = cylinder.getMaximumY() - cylinder.getMinimumY();
            final double radius =
                    Math.max(cylinder.getRadius().x(), cylinder.getRadius().z());
            m = new CircularMine(name, center, radius, height);
        } else if (selection instanceof CuboidRegion cuboidSelection) {
            final BlockVector3 minimum = cuboidSelection.getMinimumPoint();
            final BlockVector3 maximum = cuboidSelection.getMaximumPoint();
            m = new CuboidMine(name,
                    new Location(p.getWorld(), minimum.x(), minimum.y(), minimum.z()),
                    new Location(p.getWorld(), maximum.x(), maximum.y(), maximum.z()));
        } else if (selection instanceof Polygonal2DRegion polygon) {
            m = new PolygonMine(polygon, name, p.getWorld());
        }
        if (m == null) {
            this.send(p, "messages.command.unsupportedRegion",
                    "Region %region% is not supported.", "%region%",
                    selection.getClass().getSimpleName());
            GalaxyMines.instance.printException(
                    "Unsupported WorldEdit region: " + selection.getClass().getSimpleName(),
                    new UnsupportedWorldEditRegionType(selection));
            return;
        }
        for (final Block block : m) {
            if (MineManager.isPartOfMine(block)) {
                this.send(p, "messages.command.mineOverlap",
                        "A mine already exists at this location.");
                return;
            }
        }
        m.setResetDelay(resetDelay);
        m.setBroadcastOnReset(broadcastOnReset);
        m.setNearbyBroadcast(nearbyBroadcast);
        m.setBroadcastRange(broadcastRadius);
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            m.setBroadcastMessage(customMessage);
        }
        MineManager.registerMine(m);
        GalaxyMines.chatManager.sendMessage(p, GalaxyMineMessageManager.getInstance()
                .translateAll(GalaxyMineMessageManager.mineCreateMessage, m));
        m.save();
        if (GalaxyMinesConfiguration.useHolograms && m instanceof HologramCompatible compatible) {
            compatible.setupHolograms();
        }
        m.reset();
    }

    private void listMines(final CommandSender sender) {
        if (MineManager.getMines().isEmpty()) {
            this.send(sender, "messages.command.noMines", "There are no loaded mines.");
            return;
        }
        final StringBuilder names = new StringBuilder();
        for (final Mine mine : MineManager.getMines()) {
            if (names.length() > 0) {
                names.append(MessageColor.SECONDARY).append(", ").append(MessageColor.PRIMARY);
            }
            names.append(mine.getName());
        }
        this.send(sender, "messages.command.existingMines",
                "Existing mines: %mines%", "%mines%", names.toString());
    }

    private void sendMineNotFound(final CommandSender sender, final String name) {
        this.send(sender, "messages.command.mineNotFound",
                "Could not find mine %mine%.", "%mine%", name);
    }

    private void send(final CommandSender sender, final String key, final String fallback,
                      final Object... replacements) {
        this.sendRaw(sender, Messages.get(key, fallback, replacements));
    }

    private void sendRaw(final CommandSender sender, final String message) {
        sender.sendMessage(Text.parse(message));
    }

    @Override
    public void sendInvalidArguments(final Player p, final String msg) {
        this.sendRaw(p, msg);
    }

    @Override
    public void sendUsage(final Player p) {
        this.sendUsage((CommandSender) p);
    }

    public void sendUsage(final CommandSender sender) {
        for (final String message : this.usageMessages) {
            sender.sendMessage(Text.parse(message));
        }
    }

    public class EditCommand extends SubCommand {
        public EditCommand(final GMineCommand minecmd) {
            super("edit", minecmd, Collections.emptyList());
            this.usageMessages = GalaxyMineMessageManager.getInstance().translateAll(Arrays
                    .asList("<white>----------<red>♦ GalaxyMines created by Marcx96 ♦<white>----------",
                            "<aqua>➣ /mine <white>edit (name) add (material|nexo:id|itemsadder:id|oraxen:id) (number from 0 to 100)",
                            "<aqua>➣ /mine <white>edit (name) remove (material)",
                            "<aqua>➣ /mine <white>edit (name) set (alias|resetdelay|nearbybroadcast|broadcastRange) (value)",
                            "<white>--------------------"));
            final List<String> configuredEditUsage =
                    Messages.getList("messages.command.editUsage");
            if (!configuredEditUsage.isEmpty()) {
                this.usageMessages = configuredEditUsage;
            }
        }

        @Override
        public void handleExecute(final CommandSender sender, final String[] args) {
            if (!(sender instanceof Player p)) {
                GMineCommand.this.send(sender, "messages.command.consoleOnly",
                        "<red>You cannot use this command from the console");
                return;
            }
            if (!this.checkPerm(p)) {
                GMineCommand.this.send(sender, "messages.command.noPermission",
                        "You do not have permission to use this command.");
                return;
            }
            if (args.length <= 1) {
                GUIManager.mainMenu.open(p);
                return;
            }
            final String mineName = args[1];
            final Mine mine = MineManager.getMine(mineName);
            if (mine == null) {
                GMineCommand.this.sendMineNotFound(sender, mineName);
                return;
            }
            if (args.length <= 2) {
                this.sendUsage(p);
                return;
            }
            final String toDo = args[2];
            if (toDo.equalsIgnoreCase("add")) {
                this.addMaterial(p, mine, args);
            } else if (toDo.equalsIgnoreCase("remove") || toDo.equalsIgnoreCase("delete")) {
                this.removeMaterial(p, mine, args);
            } else if (toDo.equalsIgnoreCase("set")) {
                this.setParameter(p, mine, args);
            } else if (toDo.equalsIgnoreCase("commands")) {
                this.editResetCommands(p, mine, args);
            } else if (toDo.equalsIgnoreCase("constraints")) {
                this.editConstraints(p, mine, args);
            } else {
                this.sendUsage(p);
            }
        }

        private void addMaterial(final Player p, final Mine mine, final String[] args) {
            if (args.length <= 3) {
                GMineCommand.this.send(p, "messages.command.specifyMaterial",
                        "Specify a material.");
                return;
            }
            if (args.length <= 4) {
                GMineCommand.this.send(p, "messages.command.specifyNumber",
                        "Specify a number.");
                return;
            }
            final Material material = this.parseCompositionItem(p, args[3]);
            if (material == null) {
                return;
            }
            final String percentage = args[4].replace("%", "");
            try {
                mine.setMaterialPercentage(material, Double.parseDouble(percentage));
                mine.reset();
                GMineCommand.this.send(p, "messages.command.success",
                        "Change applied successfully.");
            } catch (NumberFormatException ex) {
                GMineCommand.this.send(p, "messages.command.invalidNumber",
                        "%value% is not a valid number.", "%value%", percentage);
            }
        }

        private Material parseCompositionItem(final Player p, final String raw) {
            if (CustomBlockTokens.normalize(raw) != null) {
                GMineCommand.this.send(p, "messages.command.invalidBlock",
                        "%value% is not a valid block. Custom block compositions are no longer "
                                + "supported; use a Bukkit material instead.", "%value%", raw);
                return null;
            }
            final String materialName = raw.toUpperCase(Locale.ROOT);
            final String[] split = materialName.split(":", 2);
            try {
                int materialData = 0;
                if (split.length > 1) {
                    try {
                        materialData = Integer.parseInt(split[1].trim());
                    } catch (NumberFormatException ex) {
                        GMineCommand.this.send(p, "messages.command.invalidNumber",
                                "%value% is not a valid number.", "%value%", split[1]);
                        return null;
                    }
                }
                final Material material = Materials.resolve(split[0], materialData);
                if (material == null || !material.isBlock()) {
                    GMineCommand.this.send(p, "messages.command.invalidBlock",
                            "%value% is not a valid block.", "%value%", materialName);
                    return null;
                }
                return material;
            } catch (RuntimeException ex) {
                GMineCommand.this.send(p, "messages.command.invalidMaterial",
                        "%value% is not a valid material.", "%value%", materialName);
                return null;
            }
        }

        private void removeMaterial(final Player p, final Mine mine, final String[] args) {
            if (args.length <= 3) {
                GMineCommand.this.send(p, "messages.command.specifyMaterial",
                        "Specify a material.");
                return;
            }
            final String raw = args[3];
            final Material material = this.parseCompositionItem(p, raw);
            if (material == null) {
                return;
            }
            if (!mine.containsMaterial(material)) {
                GMineCommand.this.send(p, "messages.command.mineDoesNotContain",
                        "Mine %mine% does not contain %material%.",
                        "%mine%", mine.getName(), "%material%", raw);
                return;
            }
            mine.removeMaterial(material);
            mine.reset();
            GMineCommand.this.send(p, "messages.command.success",
                    "Change applied successfully.");
        }

        private void setParameter(final Player p, final Mine mine, final String[] args) {
            if (args.length <= 3) {
                GMineCommand.this.send(p, "messages.command.specifyParameter",
                        "Specify a parameter.");
                return;
            }
            final String parameter = args[3];
            if (parameter.equalsIgnoreCase("alias")) {
                if (args.length <= 4) {
                    GMineCommand.this.send(p, "messages.command.specifyAlias",
                            "Specify the alias to set for the mine.");
                    return;
                }
                final String alias = String.join(" ",
                        Arrays.copyOfRange(args, 4, args.length)).trim();
                if (alias.isEmpty()) {
                    GMineCommand.this.send(p, "messages.command.specifyAlias",
                            "Specify the alias to set for the mine.");
                    return;
                }
                mine.setAlias(alias);
                GMineCommand.this.send(p, "messages.command.success",
                        "Change applied successfully.");
            } else if (parameter.equalsIgnoreCase("resetdelay")) {
                if (args.length <= 4) {
                    GMineCommand.this.send(p, "messages.command.specifyDelay",
                            "Specify the delay in minutes.");
                    return;
                }
                try {
                    final int minutes = Integer.parseInt(args[4]);
                    if (minutes < 1) {
                        throw new NumberFormatException();
                    }
                    mine.setResetDelay(minutes);
                    GMineCommand.this.send(p, "messages.command.success",
                            "Change applied successfully.");
                } catch (NumberFormatException ex) {
                    GMineCommand.this.send(p, "messages.command.invalidNumber",
                            "%value% is not a valid number.", "%value%", args[4]);
                }
            } else if (parameter.equalsIgnoreCase("nearbybroadcast")) {
                if (args.length <= 4) {
                    GMineCommand.this.send(p, "messages.command.specifyBoolean",
                            "Specify true or false.");
                    return;
                }
                mine.setNearbyBroadcast(Boolean.parseBoolean(args[4]));
                GMineCommand.this.send(p, "messages.command.success",
                        "Change applied successfully.");
            } else if (parameter.equalsIgnoreCase("broadcastrange")) {
                if (args.length <= 4) {
                    GMineCommand.this.send(p, "messages.command.specifyRadius",
                            "Specify a radius.");
                    return;
                }
                try {
                    mine.setBroadcastRange(Double.parseDouble(args[4]));
                    GMineCommand.this.send(p, "messages.command.success",
                            "Change applied successfully.");
                } catch (NumberFormatException ex) {
                    GMineCommand.this.send(p, "messages.command.invalidNumber",
                            "%value% is not a valid number.", "%value%", args[4]);
                }
            } else if (parameter.equalsIgnoreCase("resetpercent")) {
                if (args.length <= 4) {
                    GMineCommand.this.send(p, "messages.command.specifyPercentage",
                            "Specify a percentage.");
                    return;
                }
                try {
                    final double value = Double.parseDouble(args[4]);
                    if (!Double.isFinite(value) || value < 0.0D || value > 100.0D) {
                        throw new NumberFormatException();
                    }
                    mine.setResetOnPercent(value);
                    GMineCommand.this.send(p, "messages.command.resetPercentSet",
                            "Reset threshold of %mine% set to %value%%.",
                            "%mine%", mine.getName(), "%value%", format(value));
                } catch (NumberFormatException ex) {
                    GMineCommand.this.send(p, "messages.command.invalidNumber",
                            "%value% is not a valid number.", "%value%", args[4]);
                }
            } else if (parameter.equalsIgnoreCase("resetminpercent")
                    || parameter.equalsIgnoreCase("resetmaxpercent")) {
                if (args.length <= 4) {
                    GMineCommand.this.send(p, "messages.command.specifyPercentage",
                            "Specify a percentage.");
                    return;
                }
                try {
                    final double value = Double.parseDouble(args[4]);
                    if (!Double.isFinite(value) || value < 0.0D || value > 100.0D) {
                        throw new NumberFormatException();
                    }
                    if (parameter.equalsIgnoreCase("resetminpercent")) {
                        mine.setResetPercentRange(value, mine.getResetPercentMax());
                    } else {
                        mine.setResetPercentRange(mine.getResetPercentMin(), value);
                    }
                    GMineCommand.this.send(p, "messages.command.resetRangeSet",
                            "Reset threshold range of %mine% set to %min%%-%max%%.",
                            "%mine%", mine.getName(),
                            "%min%", format(mine.getResetPercentMin()),
                            "%max%", format(mine.getResetPercentMax()));
                } catch (NumberFormatException ex) {
                    GMineCommand.this.send(p, "messages.command.invalidNumber",
                            "%value% is not a valid number.", "%value%", args[4]);
                }
            } else if (parameter.equalsIgnoreCase("resetcheck")) {
                if (args.length <= 4) {
                    GMineCommand.this.send(p, "messages.command.specifyNumber",
                            "Specify a number.");
                    return;
                }
                try {
                    final int ticks = Integer.parseInt(args[4]);
                    if (ticks < 0 || ticks > 72_000) {
                        throw new NumberFormatException();
                    }
                    mine.setResetPercentCheckIntervalTicks(ticks);
                    GMineCommand.this.send(p, "messages.command.resetCheckSet",
                            "Reset percentage check interval of %mine% set to %ticks% tick(s).",
                            "%mine%", mine.getName(), "%ticks%", Integer.toString(ticks));
                } catch (NumberFormatException ex) {
                    GMineCommand.this.send(p, "messages.command.invalidNumber",
                            "%value% is not a valid number.", "%value%", args[4]);
                }
            } else if (parameter.equalsIgnoreCase("wavespeed")) {
                if (args.length <= 4) {
                    GMineCommand.this.send(p, "messages.command.specifyNumber",
                            "Specify a number.");
                    return;
                }
                try {
                    final int layers = Integer.parseInt(args[4]);
                    if (layers < 0 || layers > 64) {
                        throw new NumberFormatException();
                    }
                    mine.setWaveLayersPerTick(layers);
                    GMineCommand.this.send(p, "messages.command.waveSpeedSet",
                            "Layer speed of %mine% set to %layers% layer(s) per tick (0 = global).",
                            "%mine%", mine.getName(), "%layers%", Integer.toString(layers));
                } catch (NumberFormatException ex) {
                    GMineCommand.this.send(p, "messages.command.invalidNumber",
                            "%value% is not a valid number.", "%value%", args[4]);
                }
            } else if (parameter.equalsIgnoreCase("wavedirection")) {
                if (args.length <= 4) {
                    GMineCommand.this.send(p, "messages.command.waveDirectionInvalid",
                            "Invalid direction: use top or bottom.");
                    return;
                }
                final String raw = args[4].toLowerCase(java.util.Locale.ROOT)
                        .replace("_", "").replace("-", "");
                final MineWaveDirection direction;
                if (raw.equals("top") || raw.equals("topdown")) {
                    direction = MineWaveDirection.TOP_DOWN;
                } else if (raw.equals("bottom") || raw.equals("bottomup")) {
                    direction = MineWaveDirection.BOTTOM_UP;
                } else {
                    GMineCommand.this.send(p, "messages.command.waveDirectionInvalid",
                            "Invalid direction: use top or bottom.");
                    return;
                }
                mine.setWaveDirection(direction);
                GMineCommand.this.send(p, "messages.command.waveDirectionSet",
                        "Wave direction of %mine% set to %direction%.",
                        "%mine%", mine.getName(), "%direction%", direction.name());
            } else if (parameter.equalsIgnoreCase("currency")) {
                final java.util.List<String> ids =
                        me.marcdoesntexist.galaxymines.integration.economy.CurrencyRegistry
                                .ids();
                if (args.length <= 4) {
                    GMineCommand.this.send(p, "messages.command.specifyCurrency",
                            "Specify a currency: %currencies%.",
                            "%currencies%", String.join(", ", ids));
                    return;
                }
                final String currencyId = args[4].trim()
                        .toLowerCase(java.util.Locale.ROOT);
                final me.marcdoesntexist.galaxymines.integration.economy.Currency currency =
                        me.marcdoesntexist.galaxymines.integration.economy.CurrencyRegistry
                                .get(currencyId);
                if (currency == null) {
                    GMineCommand.this.send(p, "messages.command.currencyUnknown",
                            "%value% is not a registered currency. Available: %currencies%.",
                            "%value%", args[4], "%currencies%", String.join(", ", ids));
                    return;
                }
                mine.setSellCurrency(currencyId);
                GMineCommand.this.send(p, "messages.command.currencySet",
                        "Sell currency of %mine% set to %currency%.",
                        "%mine%", mine.getName(),
                        "%currency%", currency.displayName());
            } else if (parameter.equalsIgnoreCase("sellpriority")) {
                if (args.length <= 4) {
                    GMineCommand.this.send(p, "messages.command.sellPriorityInvalid",
                            "Invalid priority: use provider, material, highest or global.");
                    return;
                }
                final me.marcdoesntexist.galaxymines.integration.economy.SellPriority priority =
                        me.marcdoesntexist.galaxymines.integration.economy.SellPriority
                                .parseOrNull(args[4]);
                if (priority == null) {
                    GMineCommand.this.send(p, "messages.command.sellPriorityInvalid",
                            "Invalid priority: use provider, material, highest or global.");
                    return;
                }
                mine.setSellPriority(priority);
                GMineCommand.this.send(p, "messages.command.sellPrioritySet",
                        "Sell priority of %mine% set to %priority%.",
                        "%mine%", mine.getName(), "%priority%", priority.name());
            } else {
                GMineCommand.this.send(p, "messages.command.unknownParameter",
                        "Unknown parameter.");
            }
        }


        private void editResetCommands(final Player p, final Mine mine, final String[] args) {
            if (args.length <= 3) {
                GMineCommand.this.send(p, "messages.command.resetCommandPhase",
                        "Specify pre or post.");
                return;
            }
            final boolean pre;
            if (args[3].equalsIgnoreCase("pre")) {
                pre = true;
            } else if (args[3].equalsIgnoreCase("post")) {
                pre = false;
            } else {
                GMineCommand.this.send(p, "messages.command.resetCommandPhase",
                        "Specify pre or post.");
                return;
            }
            if (args.length <= 4) {
                GMineCommand.this.send(p, "messages.command.resetCommandAction",
                        "Specify add, remove or list.");
                return;
            }
            final String phase = pre ? "pre" : "post";
            final java.util.List<String> commands = pre
                    ? new java.util.ArrayList<>(mine.getPreResetCommands())
                    : new java.util.ArrayList<>(mine.getPostResetCommands());
            final String action = args[4];
            if (action.equalsIgnoreCase("list")) {
                GMineCommand.this.send(p, "messages.command.resetCommandsHeader",
                        "Reset commands of %mine% (%phase%):",
                        "%mine%", mine.getName(), "%phase%", phase);
                if (commands.isEmpty()) {
                    GMineCommand.this.send(p, "messages.command.resetCommandsEmpty",
                            "No %phase% reset commands are configured.", "%phase%", phase);
                    return;
                }
                for (int index = 0; index < commands.size(); index++) {
                    GMineCommand.this.send(p, "messages.command.resetCommandEntry",
                            "- %command% (#%index%)",
                            "%command%", commands.get(index),
                            "%index%", Integer.toString(index + 1));
                }
                return;
            }
            if (action.equalsIgnoreCase("add")) {
                final String raw = args.length <= 5 ? ""
                        : String.join(" ", Arrays.copyOfRange(args, 5, args.length)).trim();
                if (raw.isEmpty()) {
                    GMineCommand.this.send(p, "messages.command.resetCommandAction",
                            "Specify add, remove or list.");
                    return;
                }
                String command = raw;
                while (command.startsWith("/")) {
                    command = command.substring(1).trim();
                }
                commands.add(command);
                this.saveResetCommands(mine, pre, commands);
                GMineCommand.this.send(p, "messages.command.resetCommandAdded",
                        "Added the %phase% reset command: %command%",
                        "%phase%", phase, "%command%", command);
                return;
            }
            if (action.equalsIgnoreCase("remove")) {
                try {
                    final int index = args.length <= 5 ? -1 : Integer.parseInt(args[5]);
                    if (index < 1 || index > commands.size()) {
                        throw new NumberFormatException();
                    }
                    commands.remove(index - 1);
                    this.saveResetCommands(mine, pre, commands);
                    GMineCommand.this.send(p, "messages.command.resetCommandRemoved",
                            "Removed the %phase% reset command #%index%.",
                            "%phase%", phase, "%index%", Integer.toString(index));
                } catch (NumberFormatException ex) {
                    GMineCommand.this.send(p, "messages.command.resetCommandIndex",
                            "%value% is not a valid command index.",
                            "%value%", args.length <= 5 ? "" : args[5]);
                }
                return;
            }
            GMineCommand.this.send(p, "messages.command.resetCommandAction",
                    "Specify add, remove or list.");
        }

        private void saveResetCommands(final Mine mine, final boolean pre,
                                       final java.util.List<String> commands) {
            if (pre) {
                mine.setPreResetCommands(commands);
            } else {
                mine.setPostResetCommands(commands);
            }
        }


        private void editConstraints(final Player p, final Mine mine, final String[] args) {
            if (args.length <= 4) {
                this.sendConstraintUsage(p, mine);
                return;
            }
            final boolean breakSection;
            if (args[3].equalsIgnoreCase("break")) {
                breakSection = true;
            } else if (args[3].equalsIgnoreCase("place")) {
                breakSection = false;
            } else {
                this.sendConstraintUsage(p, mine);
                return;
            }
            final String section = breakSection ? "break" : "place";
            final String action = args[4];
            final MineConstraints current = mine.getBlockConstraints();
            final java.util.Set<Material> allowed = new java.util.LinkedHashSet<>(
                    breakSection ? current.breakAllowed() : current.placeAllowed());
            final java.util.Set<Material> denied = new java.util.LinkedHashSet<>(
                    breakSection ? current.breakDenied() : current.placeDenied());
            boolean placeEnabled = current.isPlaceEnabled();
            if (action.equalsIgnoreCase("list")) {
                GMineCommand.this.send(p, "messages.command.constraintList",
                        "%section% constraints of %mine%: allow [%allow%], deny [%deny%], "
                                + "place checks %state%",
                        "%section%", section, "%mine%", mine.getName(),
                        "%allow%", MineConstraints.format(
                                breakSection ? allowed : current.placeAllowed()),
                        "%deny%", MineConstraints.format(
                                breakSection ? denied : current.placeDenied()),
                        "%state%", placeEnabled ? "on" : "off");
                return;
            }
            if (!breakSection && (action.equalsIgnoreCase("on")
                    || action.equalsIgnoreCase("off"))) {
                placeEnabled = action.equalsIgnoreCase("on");
                mine.setBlockConstraints(new MineConstraints(current.breakAllowed(),
                        current.breakDenied(), placeEnabled, current.placeAllowed(),
                        current.placeDenied()));
                GMineCommand.this.send(p, "messages.command.constraintPlaceState",
                        "Placement constraints for %mine% are now %state%.",
                        "%mine%", mine.getName(), "%state%", placeEnabled ? "on" : "off");
                return;
            }
            if (action.equalsIgnoreCase("clear")) {
                allowed.clear();
                denied.clear();
                this.saveConstraints(mine, breakSection, allowed, denied, placeEnabled);
                GMineCommand.this.send(p, "messages.command.constraintCleared",
                        "Cleared the %section% constraints of %mine%.",
                        "%section%", section, "%mine%", mine.getName());
                return;
            }
            if (!action.equalsIgnoreCase("allow") && !action.equalsIgnoreCase("deny")) {
                this.sendConstraintUsage(p, mine);
                return;
            }
            if (args.length <= 5) {
                GMineCommand.this.send(p, "messages.command.specifyMaterial",
                        "Specify a material.");
                return;
            }
            final Material material = this.parseCompositionItem(p, args[5]);
            if (material == null) {
                return;
            }
            if (action.equalsIgnoreCase("allow")) {
                allowed.add(material);
                denied.remove(material);
            } else {
                denied.add(material);
                allowed.remove(material);
            }
            this.saveConstraints(mine, breakSection, allowed, denied, placeEnabled);
            GMineCommand.this.send(p, "messages.command.constraintChanged",
                    "%action% %material% in the %section% constraints of %mine%.",
                    "%action%", action.toLowerCase(java.util.Locale.ROOT),
                    "%material%", material.name().toLowerCase(java.util.Locale.ROOT),
                    "%section%", section, "%mine%", mine.getName());
        }

        private void saveConstraints(final Mine mine, final boolean breakSection,
                                     final java.util.Set<Material> allowed,
                                     final java.util.Set<Material> denied,
                                     final boolean placeEnabled) {
            final MineConstraints current = mine.getBlockConstraints();
            if (breakSection) {
                mine.setBlockConstraints(new MineConstraints(allowed, denied,
                        current.isPlaceEnabled(), current.placeAllowed(),
                        current.placeDenied()));
            } else {
                mine.setBlockConstraints(new MineConstraints(current.breakAllowed(),
                        current.breakDenied(), placeEnabled, allowed, denied));
            }
        }

        private void sendConstraintUsage(final Player p, final Mine mine) {
            GMineCommand.this.send(p, "messages.command.constraintUsage",
                    "Usage: /mine edit %mine% constraints (break|place) "
                            + "(allow|deny|clear|list) [material]", "%mine%", mine.getName());
        }

        private String format(final double value) {
            if (value == Math.rint(value)) {
                return Long.toString((long) value);
            }
            return String.format(java.util.Locale.ROOT, "%.2f", value);
        }

        @Override
        public void sendUsage(final Player p) {
            for (final String message : this.usageMessages) {
                p.sendMessage(Text.parse(message));
            }
        }
    }
}
