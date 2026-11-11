package me.marcdoesntexist.galaxymines.gui.actions;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.compat.Materials;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.config.MineConfig;
import me.marcdoesntexist.galaxymines.gui.GUIManager;
import me.marcdoesntexist.galaxymines.gui.GuiChatInput;
import me.marcdoesntexist.galaxymines.gui.menus.MineTierEditorMenu;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.gui.objects.GMinesController;
import me.marcdoesntexist.galaxymines.integration.economy.CurrencyRegistry;
import me.marcdoesntexist.galaxymines.integration.economy.SellService;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;











public final class MenuActionRegistry {
    private static final Map<String, MenuAction> ACTIONS = new HashMap<>();
    private static final Map<String, String> DEFAULT_PERMISSIONS = Map.ofEntries(
            Map.entry("teleport_mine", "galaxymines.tp"),
            Map.entry("reset_mine", "galaxymines.admin"),
            Map.entry("clear_mine", "galaxymines.admin"),
            Map.entry("fill_mine", "galaxymines.admin"),
            Map.entry("clear_materials", "galaxymines.admin"),
            Map.entry("balance_materials", "galaxymines.admin"),
            Map.entry("reset_materials", "galaxymines.admin"),
            Map.entry("disable_mine", "galaxymines.admin"),
            Map.entry("delete_mine", "galaxymines.admin"),
            Map.entry("remove_material", "galaxymines.admin"),
            Map.entry("change_percentage", "galaxymines.admin"),
            Map.entry("load_mine", "galaxymines.admin"),
            Map.entry("open_reset_editor", "galaxymines.admin"),
            Map.entry("set_reset_default", "galaxymines.admin"),
            Map.entry("set_reset_min", "galaxymines.admin"),
            Map.entry("set_reset_max", "galaxymines.admin"),
            Map.entry("set_reset_interval", "galaxymines.admin"),
            Map.entry("add_reset_command", "galaxymines.admin"),
            Map.entry("remove_reset_command", "galaxymines.admin"),
            Map.entry("open_constraints_editor", "galaxymines.admin"),
            Map.entry("constraint_allow", "galaxymines.admin"),
            Map.entry("constraint_deny", "galaxymines.admin"),
            Map.entry("constraint_clear", "galaxymines.admin"),
            Map.entry("toggle_place_constraints", "galaxymines.admin"),
            Map.entry("toggle_wave_direction", "galaxymines.admin"),
            Map.entry("set_wave_layers", "galaxymines.admin"),
            Map.entry("remove_constraint", "galaxymines.admin"),
            Map.entry("open_wave_editor", "galaxymines.admin"),
            Map.entry("adjust_wave_layers", "galaxymines.admin"),
            Map.entry("open_tier_editor", "galaxymines.admin"),
            Map.entry("tier_set_level", "galaxymines.admin"),
            Map.entry("tier_add_blocks", "galaxymines.admin"),
            Map.entry("expand_mine", "galaxymines.admin"),
            Map.entry("confirm_expand_mine", "galaxymines.admin"),
            Map.entry("toggle_mine_pause", "galaxymines.admin"),
            Map.entry("copy_constraints", "galaxymines.admin"),
            Map.entry("run_command", "galaxymines.admin"),
            Map.entry("sell_shop_item", "galaxymines.sell"));
    private static final Map<UUID, PendingConfirmation> PENDING_CONFIRMATIONS =
            new ConcurrentHashMap<>();
    private static LongSupplier clock = System::currentTimeMillis;

    static {
        registerDefaults();
    }

    private MenuActionRegistry() {
    }

    public static void register(final String id, final MenuAction action) {
        if (id != null && action != null) {
            MenuActionRegistry.ACTIONS.put(id.toLowerCase(Locale.ROOT), action);
        }
    }

    public static boolean execute(final String id, final MenuActionContext context) {
        if (id == null || context == null) {
            return false;
        }
        final ResolvedAction resolved = resolve(id, context.argument());
        if (resolved == null) {
            logUnknown(id);
            return false;
        }
        if (!hasPermission(context.player(), resolved.permission())) {
            deny(context.player(), resolved.permission());
            return false;
        }
        if (resolved.confirm() && !confirm(context, id, resolved)) {
            return true;
        }
        return run(resolved, context, id);
    }

    




    public static ResolvedAction resolve(final String id, final String itemArgument) {
        if (id == null || id.isBlank()) {
            return null;
        }
        final MenuConfig.ActionDefinition definition = MenuConfig.actionDefinition(id);
        if (definition == null && MenuConfig.hasActionDefinition(id)) {
            return null;
        }
        final String type = definition == null ? id : definition.type();
        final List<MenuConfig.ActionStep> steps =
                definition == null ? List.of() : definition.steps();
        if (type == null && steps.isEmpty()) {
            return null;
        }
        if (type != null && !MenuActionRegistry.ACTIONS.containsKey(type.toLowerCase(Locale.ROOT))) {
            return null;
        }
        String argument = itemArgument;
        if (argument == null && definition != null) {
            argument = definition.argument();
        }
        return new ResolvedAction(type, argument, effectivePermission(type, definition),
                definition != null && definition.confirm(),
                definition == null ? "stop" : definition.sequenceMode(), steps);
    }

    public static boolean isRegistered(final String id) {
        return resolve(id, null) != null;
    }

    


    public static String defaultPermission(final String type) {
        return type == null ? null : DEFAULT_PERMISSIONS.get(type.toLowerCase(Locale.ROOT));
    }

    


    public static void clearConfirmations() {
        PENDING_CONFIRMATIONS.clear();
    }

    



    static void setClockForTesting(final LongSupplier supplier) {
        clock = supplier == null ? System::currentTimeMillis : supplier;
    }

    static void resetClockForTesting() {
        clock = System::currentTimeMillis;
    }

    


    public static List<String> validate() {
        final List<String> warnings = new ArrayList<>();
        for (final Map.Entry<String, MenuConfig.ActionDefinition> entry
                : MenuConfig.actionDefinitions().entrySet()) {
            final String id = entry.getKey();
            final MenuConfig.ActionDefinition definition = entry.getValue();
            if (definition.type() != null
                    && !ACTIONS.containsKey(definition.type().toLowerCase(Locale.ROOT))) {
                warnings.add("Action '" + id + "' uses unknown type '"
                        + definition.type() + "'.");
            }
            for (final MenuConfig.ActionStep step : definition.steps()) {
                final MenuConfig.ActionDefinition stepDefinition =
                        MenuConfig.actionDefinition(step.type());
                final String stepType = stepDefinition == null ? step.type()
                        : stepDefinition.type();
                if (stepType == null) {
                    warnings.add("Action '" + id + "' sequence step '" + step.type()
                            + "' is a sequence and cannot be nested.");
                } else if (!ACTIONS.containsKey(stepType.toLowerCase(Locale.ROOT))) {
                    warnings.add("Action '" + id + "' sequence step '" + step.type()
                            + "' uses unknown type '" + stepType + "'.");
                }
            }
        }
        for (final String menu : MenuConfig.menuNames()) {
            for (final String key : MenuConfig.itemKeys(menu)) {
                final String actionId = MenuConfig.action(menu, key, null);
                if (actionId != null && resolve(actionId, null) == null) {
                    warnings.add("Menu '" + menu + "' item '" + key
                            + "' uses unknown action '" + actionId + "'.");
                }
            }
        }
        return warnings;
    }

    private static boolean run(final ResolvedAction resolved, final MenuActionContext context,
                               final String id) {
        boolean executed = false;
        if (resolved.type() != null) {
            final MenuAction action =
                    MenuActionRegistry.ACTIONS.get(resolved.type().toLowerCase(Locale.ROOT));
            if (action == null) {
                logUnknown(id);
                return false;
            }
            try {
                action.execute(withArgument(context, resolved.argument()));
                executed = true;
            } catch (RuntimeException ex) {
                logActionFailure(id, resolved.type(), ex);
                if (!resolved.continueOnError()) {
                    return false;
                }
            }
        }
        for (final MenuConfig.ActionStep step : resolved.steps()) {
            if (!runStep(step, context, id)) {
                if (!resolved.continueOnError()) {
                    return executed;
                }
                continue;
            }
            executed = true;
        }
        return executed;
    }

    private static boolean runStep(final MenuConfig.ActionStep step,
                                   final MenuActionContext context, final String id) {
        final MenuConfig.ActionDefinition definition = MenuConfig.actionDefinition(step.type());
        String type = step.type();
        String argument = step.argument();
        final String permission;
        if (definition != null) {
            if (definition.type() == null) {
                logUnknown("sequence step '" + step.type() + "' in '" + id + "'");
                return false;
            }
            type = definition.type();
            if (argument == null) {
                argument = definition.argument();
            }
            permission = effectivePermission(type, definition);
        } else {
            permission = defaultPermission(type);
        }
        if (!ACTIONS.containsKey(type.toLowerCase(Locale.ROOT))) {
            logUnknown("sequence step '" + step.type() + "' in '" + id + "'");
            return false;
        }
        if (!hasPermission(context.player(), permission)) {
            deny(context.player(), permission);
            return false;
        }
        try {
            ACTIONS.get(type.toLowerCase(Locale.ROOT)).execute(withArgument(context, argument));
            return true;
        } catch (RuntimeException ex) {
            logActionFailure("sequence step '" + step.type() + "' in '" + id + "'", type, ex);
            return false;
        }
    }

    private static String effectivePermission(final String type,
                                              final MenuConfig.ActionDefinition definition) {
        if (definition != null && definition.permission() != null) {
            return definition.permission().isEmpty() ? null : definition.permission();
        }
        return defaultPermission(type);
    }

    private static boolean hasPermission(final Player player, final String permission) {
        return permission == null || permission.isBlank() || player.hasPermission(permission);
    }

    private static void deny(final Player player, final String permission) {
        player.sendMessage(Messages.get("messages.gui.noPermission",
                "You do not have permission to do that.", "%permission%", permission));
    }

    private static boolean confirm(final MenuActionContext context, final String id,
                                   final ResolvedAction resolved) {
        final UUID uuid = context.player().getUniqueId();
        final String key = confirmationKey(id, resolved, context);
        final long timeout = MenuConfig.confirmationTimeoutMillis();
        final long now = clock.getAsLong();
        final PendingConfirmation pending = PENDING_CONFIRMATIONS.get(uuid);
        if (pending != null && pending.expiresAt() >= now && pending.key().equals(key)) {
            PENDING_CONFIRMATIONS.remove(uuid);
            return true;
        }
        PENDING_CONFIRMATIONS.put(uuid, new PendingConfirmation(key, now + timeout));
        context.player().sendMessage(Messages.get("messages.gui.confirmAction",
                "Click the same button again within %seconds% seconds to confirm.",
                "%seconds%", timeout / 1000L));
        return false;
    }

    private static String confirmationKey(final String id, final ResolvedAction resolved,
                                          final MenuActionContext context) {
        final String mine = context.mine() == null ? "" : context.mine().getName();
        return id.toLowerCase(Locale.ROOT) + "|" + resolved.type() + "|"
                + resolved.argument() + "|" + mine;
    }

    private static MenuActionContext withArgument(final MenuActionContext context,
                                                  final String argument) {
        return Objects.equals(argument, context.argument()) ? context
                : new MenuActionContext(context.player(), context.itemInHand(), context.mine(),
                context.menu(), context.payload(), argument);
    }

    private static void registerDefaults() {
        register("noop", context -> {
        });
        register("refresh", MenuActionRegistry::refresh);
        register("close", context -> context.player().closeInventory());
        register("back", context -> {
            if (context.payload() instanceof BasicInventoryMenu menu) {
                menu.open(context.player());
            }
        });
        register("open_mine", context -> {
            if (context.mine() != null && GUIManager.mainMenu != null) {
                GUIManager.mainMenu.openMineMenu(context.mine(), context.player());
            }
        });
        register("open_reset_editor", context -> {
            if (context.mine() == null || GUIManager.mainMenu == null) {
                return;
            }
            GUIManager.mainMenu.getMineInventory(context.mine())
                    .openResetEditor(context.player());
        });
        register("set_reset_default", context -> requestPercentage(context,
                "messages.gui.resetEditor.promptDefault",
                "<yellow>Type the default reset threshold (0-100) or 'cancel'.",
                (mine, value) -> mine.setResetPercentDefault(value),
                "messages.gui.resetEditor.defaultSet",
                "<green>Default reset threshold set to <aqua>%value%%<green>."));
        register("set_reset_min", context -> requestPercentage(context,
                "messages.gui.resetEditor.promptMin",
                "<yellow>Type the minimum reset threshold (0-100) or 'cancel'.",
                (mine, value) -> mine.setResetPercentRange(value, mine.getResetPercentMax()),
                "messages.gui.resetEditor.minSet",
                "<green>Minimum reset threshold set to <aqua>%value%%<green>."));
        register("set_reset_max", context -> requestPercentage(context,
                "messages.gui.resetEditor.promptMax",
                "<yellow>Type the maximum reset threshold (0-100) or 'cancel'.",
                (mine, value) -> mine.setResetPercentRange(mine.getResetPercentMin(), value),
                "messages.gui.resetEditor.maxSet",
                "<green>Maximum reset threshold set to <aqua>%value%%<green>."));
        register("set_reset_interval", MenuActionRegistry::requestInterval);
        register("add_reset_command", context -> {
            final Mine mine = context.mine();
            if (mine == null) {
                return;
            }
            final boolean pre = !"post".equalsIgnoreCase(context.argument());
            final Player player = context.player();
            GuiChatInput.request(player,
                    pre ? "messages.gui.resetEditor.promptPreCommand"
                            : "messages.gui.resetEditor.promptPostCommand",
                    pre ? "<yellow>Type the console command to run before the reset or 'cancel'."
                            : "<yellow>Type the console command to run after the reset or 'cancel'.",
                    raw -> {
                        final String command = sanitizeCommand(raw);
                        if (command == null) {
                            player.sendMessage(Messages.get(
                                    "messages.gui.resetEditor.commandInvalid",
                                    "<red>That command cannot be used."));
                            reopen(context, player);
                            return;
                        }
                        final List<String> commands = new ArrayList<>(
                                pre ? mine.getPreResetCommands() : mine.getPostResetCommands());
                        commands.add(command);
                        if (pre) {
                            mine.setPreResetCommands(commands);
                        } else {
                            mine.setPostResetCommands(commands);
                        }
                        player.sendMessage(Messages.get("messages.gui.resetEditor.commandAdded",
                                "<green>Added the <aqua>%phase%<green> reset command "
                                        + "#<aqua>%index%<green>: <aqua>%command%",
                                "%phase%", pre ? "pre" : "post",
                                "%index%", Integer.toString(commands.size()),
                                "%command%", command));
                        reopen(context, player);
                    });
        });
        register("remove_reset_command", context -> {
            final Mine mine = context.mine();
            if (mine == null
                    || !(context.payload() instanceof MenuActionContext.CommandRemoval removal)) {
                return;
            }
            final List<String> commands = new ArrayList<>(removal.pre()
                    ? mine.getPreResetCommands() : mine.getPostResetCommands());
            if (removal.index() < 0 || removal.index() >= commands.size()) {
                return;
            }
            final String removed = commands.remove(removal.index());
            if (removal.pre()) {
                mine.setPreResetCommands(commands);
            } else {
                mine.setPostResetCommands(commands);
            }
            context.player().sendMessage(Messages.get(
                    "messages.gui.resetEditor.commandRemoved",
                    "<green>Removed the <aqua>%phase%<green> reset command "
                            + "#<aqua>%index%<green>: <aqua>%command%",
                    "%phase%", removal.pre() ? "pre" : "post",
                    "%index%", Integer.toString(removal.index() + 1),
                    "%command%", removed));
            removal.returnMenu().open(context.player());
        });
        register("open_constraints_editor", context -> {
            if (context.mine() == null || GUIManager.mainMenu == null) {
                return;
            }
            GUIManager.mainMenu.getMineInventory(context.mine())
                    .openConstraintsEditor(context.player());
        });
        register("constraint_allow", context -> requestConstraintMaterial(context, true));
        register("constraint_deny", context -> requestConstraintMaterial(context, false));
        register("constraint_clear", context -> {
            final Mine mine = context.mine();
            if (mine == null) {
                return;
            }
            final boolean breakSection = !"place".equalsIgnoreCase(context.argument());
            applyConstraints(mine, breakSection, new LinkedHashSet<>(),
                    new LinkedHashSet<>());
            context.player().sendMessage(Messages.get(
                    "messages.gui.constraintsEditor.sectionCleared",
                    "<green>Cleared the <aqua>%section%<green> constraints.",
                    "%section%", breakSection ? "break" : "place"));
            reopen(context, context.player());
        });
        register("toggle_place_constraints", context -> {
            final Mine mine = context.mine();
            if (mine == null) {
                return;
            }
            final MineConstraints current = mine.getBlockConstraints();
            final boolean enabled = !current.isPlaceEnabled();
            mine.setBlockConstraints(new MineConstraints(current.breakAllowed(),
                    current.breakDenied(), enabled, current.placeAllowed(),
                    current.placeDenied()));
            context.player().sendMessage(Messages.get(
                    "messages.gui.constraintsEditor.placeChanged",
                    "<green>Placement checks are now <aqua>%state%<green>.",
                    "%state%", enabled ? "on" : "off"));
            reopen(context, context.player());
        });
        register("toggle_wave_direction", context -> {
            final Mine mine = context.mine();
            if (mine == null) {
                return;
            }
            final MineWaveDirection direction =
                    mine.getWaveDirection() == MineWaveDirection.BOTTOM_UP
                            ? MineWaveDirection.TOP_DOWN : MineWaveDirection.BOTTOM_UP;
            mine.setWaveDirection(direction);
            context.player().sendMessage(Messages.get(
                    "messages.gui.constraintsEditor.waveDirectionChanged",
                    "<green>Wave direction set to <aqua>%direction%<green>.",
                    "%direction%", direction.name()));
            reopen(context, context.player());
        });
        register("set_wave_layers", MenuActionRegistry::requestWaveLayers);
        register("adjust_wave_layers", context -> {
            final Mine mine = context.mine();
            if (mine == null) {
                return;
            }
            final int delta = "-1".equals(context.argument()) ? -1 : 1;
            final int layers = Math.max(0, Math.min(64,
                    mine.getWaveLayersPerTick() + delta));
            mine.setWaveLayersPerTick(layers);
            context.player().sendMessage(Messages.get(
                    "messages.gui.waveEditor.layersAdjusted",
                    "<green>Wave speed set to <aqua>%value%<green> layer(s) per tick "
                            + "(0 = global).",
                    "%value%", Integer.toString(layers)));
            reopen(context, context.player());
        });
        register("open_wave_editor", context -> {
            if (context.mine() == null || GUIManager.mainMenu == null) {
                return;
            }
            GUIManager.mainMenu.getMineInventory(context.mine())
                    .openWaveEditor(context.player());
        });
        register("open_tier_editor", context -> {
            if (context.mine() == null || GUIManager.mainMenu == null) {
                return;
            }
            GUIManager.mainMenu.getMineInventory(context.mine())
                    .openTierEditor(context.player());
        });
        register("tier_set_level", context -> {
            final Mine mine = context.mine();
            if (mine == null) {
                return;
            }
            final Player player = context.player();
            final MineProgression progression = mine.getProgression();
            GuiChatInput.request(player, "messages.gui.tierEditor.promptLevel",
                    "<yellow>Type the new tier level (1-%max%) or 'cancel'.", raw -> {
                        final Integer level = parseInteger(raw);
                        if (level == null || level < 1 || level > progression.maxLevel()) {
                            player.sendMessage(Messages.get(
                                    "messages.gui.tierEditor.invalidLevel",
                                    "<red>%value% is not a level between 1 and %max%.",
                                    "%value%", raw,
                                    "%max%", Integer.toString(progression.maxLevel())));
                        } else {
                            mine.setProgression(progression.withLevel(level));
                            player.sendMessage(Messages.get(
                                    "messages.gui.tierEditor.levelSet",
                                    "<green>Tier of %mine% set to <aqua>%level%<green>.",
                                    "%mine%", mine.getName(),
                                    "%level%", Integer.toString(level)));
                        }
                        reopen(context, player);
                    });
        });
        register("tier_add_blocks", context -> {
            final Mine mine = context.mine();
            if (mine == null) {
                return;
            }
            final Player player = context.player();
            final MineProgression progression = mine.getProgression();
            GuiChatInput.request(player, "messages.gui.tierEditor.promptBlocks",
                    "<yellow>Type how many mined blocks to add or 'cancel'.", raw -> {
                        final long amount;
                        try {
                            amount = Long.parseLong(raw.trim());
                        } catch (NumberFormatException ex) {
                            player.sendMessage(Messages.get(
                                    "messages.gui.tierEditor.invalidBlocks",
                                    "<red>%value% is not a block count between 1 and "
                                            + "1000000000.", "%value%", raw));
                            reopen(context, player);
                            return;
                        }
                        if (amount < 1L || amount > 1_000_000_000L) {
                            player.sendMessage(Messages.get(
                                    "messages.gui.tierEditor.invalidBlocks",
                                    "<red>%value% is not a block count between 1 and "
                                            + "1000000000.", "%value%", raw));
                            reopen(context, player);
                            return;
                        }
                        final MineProgression updated =
                                progression.withMined(progression.minedBlocks() + amount);
                        mine.setProgression(updated);
                        player.sendMessage(Messages.get(
                                "messages.gui.tierEditor.blocksAdded",
                                "<green>Added <aqua>%blocks%<green> blocks to <aqua>%mine%"
                                        + "<green>: tier <aqua>%level%<green>.",
                                "%blocks%", Long.toString(amount),
                                "%mine%", mine.getName(),
                                "%level%", Integer.toString(updated.level())));
                        reopen(context, player);
                    });
        });
        register("expand_mine", context -> {
            final Mine mine = context.mine();
            if (mine == null || !(context.menu() instanceof MineTierEditorMenu menu)) {
                return;
            }
            final Player player = context.player();
            GuiChatInput.request(player, "messages.gui.tierEditor.promptExpand",
                    "<yellow>Type how many blocks per side to grow by or 'cancel'.", raw -> {
                        final Integer blocks = parseInteger(raw);
                        if (blocks == null || blocks < 1
                                || blocks > MineExpansion.MAX_BLOCKS_PER_SIDE) {
                            player.sendMessage(Messages.get(
                                    "messages.gui.tierEditor.invalidBlocks",
                                    "<red>%value% is not a block count between 1 and "
                                            + "1000000000.", "%value%", raw));
                            reopen(context, player);
                            return;
                        }
                        switch (MineManager.checkExpansion(mine, blocks)) {
                            case NOT_SUPPORTED, NO_WORLD -> {
                                player.sendMessage(Messages.get(
                                        "messages.command.expandUnsupported",
                                        "Mine %mine% cannot be expanded: only cuboid mines "
                                                + "support it.", "%mine%", mine.getName()));
                                reopen(context, player);
                                return;
                            }
                            case OVERLAP -> {
                                player.sendMessage(Messages.get(
                                        "messages.command.expandOverlap",
                                        "Mine %mine% would overlap another mine.",
                                        "%mine%", mine.getName()));
                                reopen(context, player);
                                return;
                            }
                            case TOO_LARGE -> {
                                player.sendMessage(Messages.get(
                                        "messages.command.expandTooLarge",
                                        "Mine %mine% would exceed the maximum mine volume.",
                                        "%mine%", mine.getName()));
                                reopen(context, player);
                                return;
                            }
                            default -> {
                            }
                        }
                        menu.setPendingExpansion(blocks);
                        player.sendMessage(Messages.get(
                                "messages.gui.tierEditor.expandPrepared",
                                "<green>Mine <aqua>%mine%<green> would grow by "
                                        + "<aqua>%blocks%<green> blocks per side. Click the "
                                        + "confirm button.",
                                "%mine%", mine.getName(),
                                "%blocks%", Integer.toString(blocks)));
                        reopen(context, player);
                    });
        });
        register("confirm_expand_mine", context -> {
            final Mine mine = context.mine();
            if (mine == null || !(context.menu() instanceof MineTierEditorMenu menu)) {
                return;
            }
            final Player player = context.player();
            final int blocks = menu.takePendingExpansion();
            if (blocks <= 0) {
                player.sendMessage(Messages.get("messages.gui.tierEditor.confirmMissing",
                        "<red>Type an expansion amount first."));
                reopen(context, player);
                return;
            }
            final MineManager.ExpansionResult result = MineManager.expandMine(mine, blocks);
            player.sendMessage(result == MineManager.ExpansionResult.OK
                    ? Messages.get("messages.command.expandDone",
                    "Mine %mine% expanded by %blocks% blocks per side, new volume %volume%.",
                    "%mine%", mine.getName(), "%blocks%", Integer.toString(blocks),
                    "%volume%", Long.toString(MineExpansion.volume(mine.getBounds())))
                    : Messages.get("messages.command.expandUnsupported",
                    "Mine %mine% cannot be expanded: only cuboid mines support it.",
                    "%mine%", mine.getName()));
            reopen(context, player);
        });
        register("toggle_mine_pause", context -> {
            final Mine mine = context.mine();
            if (mine == null) {
                return;
            }
            final boolean paused = !mine.isPaused();
            mine.setPaused(paused);
            context.player().sendMessage(Messages.get(
                    paused ? "messages.mine.paused" : "messages.mine.resumed",
                    paused ? "The mine %mine% is paused." : "The mine %mine% is active again.",
                    "%mine%", mine.getName()));
            reopen(context, context.player());
        });
        register("copy_constraints", context -> {
            final Mine mine = context.mine();
            if (mine == null) {
                return;
            }
            final Player player = context.player();
            GuiChatInput.request(player, "messages.gui.constraintsEditor.promptCopyMine",
                    "<yellow>Type the name of the mine to copy the rules from or 'cancel'.",
                    raw -> {
                        final Mine source = MineManager.getMine(raw.trim());
                        if (source == null) {
                            player.sendMessage(Messages.get(
                                    "messages.gui.constraintsEditor.copyUnknown",
                                    "<red>Could not find mine %mine%.", "%mine%", raw));
                        } else if (source == mine) {
                            player.sendMessage(Messages.get(
                                    "messages.gui.constraintsEditor.copySame",
                                    "<red>That is the current mine; choose another one."));
                        } else {
                            final MineConstraints original = source.getBlockConstraints();
                            mine.setBlockConstraints(new MineConstraints(
                                    original.breakAllowed(), original.breakDenied(),
                                    original.isPlaceEnabled(), original.placeAllowed(),
                                    original.placeDenied()));
                            player.sendMessage(Messages.get(
                                    "messages.gui.constraintsEditor.copied",
                                    "<green>Copied the rules of <aqua>%source%<green> into "
                                            + "this mine.",
                                    "%source%", source.getName()));
                        }
                        reopen(context, player);
                    });
        });
        register("remove_constraint", context -> {
            final Mine mine = context.mine();
            if (mine == null
                    || !(context.payload() instanceof MenuActionContext.ConstraintRemoval removal)) {
                return;
            }
            final MineConstraints current = mine.getBlockConstraints();
            final java.util.Set<Material> allowed = new LinkedHashSet<>(
                    removal.breakSection() ? current.breakAllowed() : current.placeAllowed());
            final java.util.Set<Material> denied = new LinkedHashSet<>(
                    removal.breakSection() ? current.breakDenied() : current.placeDenied());
            final boolean removed = removal.allow()
                    ? allowed.remove(removal.material()) : denied.remove(removal.material());
            if (!removed) {
                return;
            }
            applyConstraints(mine, removal.breakSection(), allowed, denied);
            context.player().sendMessage(Messages.get(
                    "messages.gui.constraintsEditor.materialRemoved",
                    "<green>Removed <aqua>%material%<green> from the <aqua>%section% "
                            + "%list%<green> list.",
                    "%material%", removal.material().name(),
                    "%section%", removal.breakSection() ? "break" : "place",
                    "%list%", removal.allow() ? "allow" : "deny"));
            removal.returnMenu().open(context.player());
        });
        register("teleport_mine", context -> {
            if (context.mine() == null) {
                return;
            }
            if (!MineAccess.canAccess(context.player(), context.mine())) {
                MineAccess.denyAccess(context.player(), context.mine());
                return;
            }
            context.player().teleportAsync(context.mine().getSafeTeleportLocation());
        });
        register("reset_mine", context -> {
            if (context.mine() != null) {
                context.mine().reset();
                refresh(context);
            }
        });
        register("clear_mine", context -> {
            final Mine mine = context.mine();
            if (mine == null) {
                return;
            }
            final Player player = context.player();
            if (!MineBulkOperations.startClear(mine, () -> {
                if (player.isOnline()) {
                    player.sendMessage(Messages.get("messages.gui.mineCleared",
                            "The mine has been cleared.", "%mine%", mine.getName()));
                }
                refresh(context);
            })) {
                player.sendMessage(Messages.get("messages.gui.mineBusy",
                        "Another clear or fill operation is already running for this mine.",
                        "%mine%", mine.getName()));
                return;
            }
            player.sendMessage(Messages.get("messages.gui.mineClearing",
                    "Clearing the mine...", "%mine%", mine.getName()));
        });
        register("fill_mine", context -> {
            final Mine mine = context.mine();
            final org.bukkit.inventory.ItemStack item = context.itemInHand();
            if (mine == null || item == null || item.getType().isAir()) {
                return;
            }
            final Player player = context.player();
            final String material = item.getType().name();
            if (!MineBulkOperations.startFill(mine, item, () -> {
                if (player.isOnline()) {
                    player.sendMessage(Messages.get("messages.gui.mineFilled",
                            "The mine has been filled.", "%mine%", mine.getName(),
                            "%material%", material));
                }
                refresh(context);
            })) {
                player.sendMessage(Messages.get("messages.gui.mineBusy",
                        "Another clear or fill operation is already running for this mine.",
                        "%mine%", mine.getName()));
                return;
            }
            player.sendMessage(Messages.get("messages.gui.mineFilling",
                    "Filling the mine...", "%mine%", mine.getName(),
                    "%material%", material));
        });
        register("clear_materials", context -> {
            if (context.mine() != null) {
                context.mine().clearMaterials();
                refresh(context);
            }
        });
        register("balance_materials", context -> {
            if (context.mine() == null) {
                return;
            }
            final List<Material> materials = context.mine().getMaterials();
            final double percentage = materials.isEmpty() ? 0.0 : 100.0 / materials.size();
            for (final Material material : materials) {
                context.mine().setMaterialPercentage(material, percentage);
            }
            context.player().sendMessage(Messages.get("messages.gui.materialsBalanced",
                    "Materials balanced to %percentage%% each.",
                    "%percentage%", percentage));
            refresh(context);
        });
        register("reset_materials", context -> {
            if (context.mine() == null) {
                return;
            }
            for (final Material material : context.mine().getMaterials()) {
                context.mine().setMaterialPercentage(material, 5.0D);
            }
            context.player().sendMessage(Messages.get("messages.gui.materialsReset",
                    "Materials reset to 5% each."));
            refresh(context);
        });
        register("disable_mine", context -> {
            if (context.mine() == null) {
                return;
            }
            context.player().sendMessage(Messages.get("messages.gui.mineDisabled",
                    "Mine %mine% has been disabled.", "%mine%", context.mine().getName()));
            context.mine().disable();
            context.player().closeInventory();
        });
        register("delete_mine", context -> {
            if (context.mine() == null) {
                return;
            }
            context.player().sendMessage(Messages.get("messages.gui.mineDeleted",
                    "Mine %mine% has been deleted.", "%mine%", context.mine().getName()));
            context.mine().delete();
            context.player().closeInventory();
        });
        register("toggle_composition", context -> {
            if (context.payload() instanceof GMinesController controller) {
                controller.toggleDisplay();
            }
        });
        register("remove_material", context -> {
            if (context.mine() != null
                    && context.payload() instanceof MenuActionContext.Removal removal) {
                context.mine().removeMaterial(removal.material());
                removal.returnMenu().open(context.player());
            }
        });
        register("change_percentage", context -> {
            if (context.mine() == null
                    || !(context.payload() instanceof MenuActionContext.PercentageChange change)) {
                return;
            }
            context.mine().setMaterialPercentage(change.material(),
                    context.mine().getPercentage(change.material()) + change.delta());
            refresh(context);
        });
        register("load_mine", context -> {
            if (!(context.payload() instanceof MineConfig mineConfig)) {
                return;
            }
            if (!MineManager.isNameAvailable(mineConfig.getName())) {
                context.player().sendMessage(Messages.get("messages.gui.mineAlreadyLoaded",
                        "A mine with this name is already loaded."));
                return;
            }
            final Mine loaded = MineManager.loadMine(mineConfig);
            loaded.setEnabled(true);
            loaded.save();
            context.player().sendMessage(Messages.get("messages.gui.loadedMine",
                    "Mine %mine% has been loaded.", "%mine%", loaded.getName()));
            refresh(context);
        });
        
        
        register("sell_shop_item", context -> {
            final Player player = context.player();
            if (!SellService.isEnabled()) {
                player.sendMessage(Text.parse(Messages.get("messages.sell.shopDisabled",
                        "<red>The sell shop is disabled.")));
                return;
            }
            final boolean all = context.argument() == null
                    || "all".equalsIgnoreCase(context.argument().trim());
            final Mine mine = MineManager.getMineAt(player.getLocation());
            final SellService.Sale sale = all
                    ? SellService.sellInventory(player, mine)
                    : SellService.sellHand(player, mine);
            if (sale.isEmpty()) {
                player.sendMessage(Text.parse(Messages.get("messages.sell.nothing",
                        "<red>You have nothing that can be sold.")));
            } else {
                player.sendMessage(Text.parse(Messages.get("messages.sell.success",
                        "<green>Sold %items% items for %amount%.",
                        "%items%", Integer.toString(sale.itemCount()),
                        "%amount%", CurrencyRegistry.format(sale.currency(), sale.amount()))));
            }
            if (context.menu() != null) {
                
                context.menu().open(player);
            }
        });
        register("run_command", context -> {
            if (context.argument() == null || context.argument().isBlank()) {
                return;
            }
            String command = context.argument().replace("%player%", context.player().getName());
            if (context.mine() != null) {
                command = command.replace("%mine%", context.mine().getName());
            }
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            context.player().performCommand(command);
        });
    }

    



    private static void requestPercentage(final MenuActionContext context, final String promptKey,
                                          final String promptFallback,
                                          final java.util.function.BiConsumer<Mine, Double> apply,
                                          final String successKey, final String successFallback) {
        final Mine mine = context.mine();
        if (mine == null) {
            return;
        }
        final Player player = context.player();
        GuiChatInput.request(player, promptKey, promptFallback, raw -> {
            final Double value = parseDouble(raw);
            if (value == null || value < 0.0D || value > 100.0D) {
                player.sendMessage(Messages.get("messages.gui.resetEditor.invalidPercentage",
                        "<red>%value% is not a percentage between 0 and 100.",
                        "%value%", raw));
                reopen(context, player);
                return;
            }
            apply.accept(mine, value);
            player.sendMessage(Messages.get(successKey, successFallback,
                    "%value%", formatNumber(value)));
            reopen(context, player);
        });
    }

    private static void requestInterval(final MenuActionContext context) {
        final Mine mine = context.mine();
        if (mine == null) {
            return;
        }
        final Player player = context.player();
        GuiChatInput.request(player, "messages.gui.resetEditor.promptInterval",
                "<yellow>Type the minimum ticks between threshold checks (0-72000) "
                        + "or 'cancel'.", raw -> {
                    final Integer ticks = parseInteger(raw);
                    if (ticks == null || ticks < 0 || ticks > 72_000) {
                        player.sendMessage(Messages.get(
                                "messages.gui.resetEditor.invalidInterval",
                                "<red>%value% is not a tick count between 0 and 72000.",
                                "%value%", raw));
                        reopen(context, player);
                        return;
                    }
                    mine.setResetPercentCheckIntervalTicks(ticks);
                    player.sendMessage(Messages.get("messages.gui.resetEditor.intervalSet",
                            "<green>Reset check interval set to <aqua>%value%<green> tick(s).",
                            "%value%", Integer.toString(ticks)));
                    reopen(context, player);
                });
    }

    




    private static void requestConstraintMaterial(final MenuActionContext context,
                                                  final boolean allow) {
        final Mine mine = context.mine();
        if (mine == null) {
            return;
        }
        final boolean breakSection = !"place".equalsIgnoreCase(context.argument());
        final Player player = context.player();
        GuiChatInput.request(player, "messages.gui.constraintsEditor.promptMaterial",
                "<yellow>Type a block name (for example stone) or 'cancel'. "
                        + "You have %seconds% seconds.", raw -> {
                    final Material material = Materials.resolve(raw);
                    if (material == null || !material.isBlock()) {
                        player.sendMessage(Messages.get(
                                "messages.gui.constraintsEditor.invalidMaterial",
                                "<red>%value% is not a known block.", "%value%", raw));
                        reopen(context, player);
                        return;
                    }
                    final MineConstraints current = mine.getBlockConstraints();
                    final java.util.Set<Material> allowed = new LinkedHashSet<>(
                            breakSection ? current.breakAllowed() : current.placeAllowed());
                    final java.util.Set<Material> denied = new LinkedHashSet<>(
                            breakSection ? current.breakDenied() : current.placeDenied());
                    if (allow) {
                        allowed.add(material);
                        denied.remove(material);
                    } else {
                        denied.add(material);
                        allowed.remove(material);
                    }
                    applyConstraints(mine, breakSection, allowed, denied);
                    player.sendMessage(Messages.get(
                            "messages.gui.constraintsEditor.materialAdded",
                            "<green>%action% <aqua>%material%<green> in the <aqua>%section% "
                                    + "constraints.",
                            "%action%", allow ? "Allowed" : "Denied",
                            "%material%", material.name(),
                            "%section%", breakSection ? "break" : "place"));
                    reopen(context, player);
                });
    }

    private static void requestWaveLayers(final MenuActionContext context) {
        final Mine mine = context.mine();
        if (mine == null) {
            return;
        }
        final Player player = context.player();
        GuiChatInput.request(player, "messages.gui.constraintsEditor.promptWaveLayers",
                "<yellow>Type the layers per tick (0-64, 0 = global) or 'cancel'. "
                        + "You have %seconds% seconds.", raw -> {
                    final Integer layers = parseInteger(raw);
                    if (layers == null || layers < 0 || layers > 64) {
                        player.sendMessage(Messages.get(
                                "messages.gui.constraintsEditor.invalidWaveLayers",
                                "<red>%value% is not a layer count between 0 and 64.",
                                "%value%", raw));
                        reopen(context, player);
                        return;
                    }
                    mine.setWaveLayersPerTick(layers);
                    player.sendMessage(Messages.get(
                            "messages.gui.constraintsEditor.waveLayersSet",
                            "<green>Wave speed set to <aqua>%value%<green> layer(s) per tick "
                                    + "(0 = global).",
                            "%value%", Integer.toString(layers)));
                    reopen(context, player);
                });
    }

    private static void applyConstraints(final Mine mine, final boolean breakSection,
                                         final java.util.Set<Material> allowed,
                                         final java.util.Set<Material> denied) {
        final MineConstraints current = mine.getBlockConstraints();
        if (breakSection) {
            mine.setBlockConstraints(new MineConstraints(allowed, denied,
                    current.isPlaceEnabled(), current.placeAllowed(), current.placeDenied()));
        } else {
            mine.setBlockConstraints(new MineConstraints(current.breakAllowed(),
                    current.breakDenied(), current.isPlaceEnabled(), allowed, denied));
        }
    }

    private static void reopen(final MenuActionContext context, final Player player) {
        if (context.menu() != null && player.isOnline()) {
            context.menu().open(player);
        }
    }

    private static Double parseDouble(final String raw) {
        try {
            final double value = Double.parseDouble(raw.replace(',', '.'));
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer parseInteger(final String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String formatNumber(final double value) {
        String formatted = String.format(Locale.ROOT, "%.2f", value);
        while (formatted.contains(".") && (formatted.endsWith("0") || formatted.endsWith("."))) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    private static String sanitizeCommand(final String raw) {
        String command = raw == null ? "" : raw.trim();
        while (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (command.isEmpty() || command.length() > 256
                || command.indexOf('\n') >= 0 || command.indexOf('\r') >= 0) {
            return null;
        }
        return command;
    }

    private static void refresh(final MenuActionContext context) {
        if (context.menu() != null) {
            context.menu().update();
        }
    }

    private static void logUnknown(final String id) {
        if (GalaxyMines.instance == null) {
            return;
        }
        final MenuConfig.ActionDefinition definition = MenuConfig.actionDefinition(id);
        final String type = definition == null || definition.type() == null
                ? id : definition.type();
        GalaxyMines.instance.debug("Unknown menu action in Menu.yml: " + id
                + (type.equals(id) ? "" : " (" + type + ")"), true);
    }

    private static void logActionFailure(final String id, final String type,
                                         final RuntimeException ex) {
        if (GalaxyMines.instance == null) {
            return;
        }
        GalaxyMines.instance.debug("Menu action '" + id + "' (" + type
                + ") failed: " + ex.getMessage(), true);
    }

    




    public record ResolvedAction(String type, String argument, String permission,
                                 boolean confirm, String sequenceMode,
                                 List<MenuConfig.ActionStep> steps) {
        public ResolvedAction {
            sequenceMode = "continue".equalsIgnoreCase(sequenceMode) ? "continue" : "stop";
            steps = steps == null ? List.of() : List.copyOf(steps);
        }

        public boolean continueOnError() {
            return "continue".equals(this.sequenceMode);
        }
    }

    private record PendingConfirmation(String key, long expiresAt) {
    }
}
