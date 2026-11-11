package me.marcdoesntexist.galaxymines.config;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.utils.PotionUtils;
import me.marcdoesntexist.galaxymines.compat.Materials;
import me.marcdoesntexist.galaxymines.integration.economy.SellPriority;
import me.marcdoesntexist.galaxymines.mines.*;
import me.marcdoesntexist.galaxymines.mines.types.CircularMine;
import me.marcdoesntexist.galaxymines.mines.types.CuboidMine;
import me.marcdoesntexist.galaxymines.mines.types.PolygonMine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MineConfig {
    private final boolean enabled;
    private final FileConfiguration config;
    private final World world;
    private final MineType type;
    private final String name;
    private final String alias;
    private final String broadcastMessage;
    private final boolean broadcastOnReset;
    private final boolean broadcastNearby;
    private final double broadcastRadius;
    private final int resetDelay;
    private final HashMap<Material, Double> composition;
    private final boolean useCustomBroadcast;
    private final ArrayList<PotionEffect> potionEffects;
    private final boolean requireAccessPermission;
    private final String requiredPermission;
    private final String requiredRank;
    private final boolean requireBreakPermission;
    private final boolean compositionOnly;
    private final MineConstraints constraints;
    private final int waveLayersPerTick;
    private final MineWaveDirection waveDirection;
    private final MineProgression progression;
    private final String sellCurrency;
    private final SellPriority sellPriority;
    private final Location spawn;
    private final double resetOnPercent;
    private final ResetThreshold resetThreshold;
    private final List<String> preResetCommands;
    private final List<String> postResetCommands;
    private final List<String> scheduledResetTimes;
    private final boolean paused;
    private final boolean bossBarEnabled;
    private final boolean worldGuardEnabled;
    private final HashMap<String, String> worldGuardFlags;
    private final MineDrops drops;
    private final MineResetStyle resetStyle;
    private final boolean infinite;
    private final double infiniteRespawnSeconds;
    private final int infiniteMaxPending;

    public MineConfig(final FileConfiguration mine) {
        this.config = mine;

        final String worldName = mine.getString("world");
        if (worldName == null || worldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing 'world' key in the configuration.");
        }
        this.world = Bukkit.getWorld(worldName);
        if (this.world == null) {
            throw new IllegalArgumentException("World '" + worldName + "' is not loaded.");
        }

        final String typeName = mine.getString("type");
        if (typeName == null || typeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing 'type' key in the configuration.");
        }
        try {
            this.type = MineType.valueOf(typeName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown mine type: " + typeName, ex);
        }

        this.name = mine.getString("name");
        if (this.name == null || this.name.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing mine name.");
        }

        final String configuredAlias = mine.getString("alias");
        this.alias = configuredAlias == null || configuredAlias.trim().isEmpty() ?
                this.name :
                configuredAlias;
        this.enabled = mine.getBoolean("enabled", true);
        final PluginSettings settings = PluginSettings.get();
        this.broadcastOnReset =
                mine.getBoolean("broadcastOnReset", settings.defaultBroadcastOnReset);
        this.broadcastNearby =
                mine.getBoolean("broadcastToNearbyOnly", settings.defaultBroadcastNearby);
        this.useCustomBroadcast = mine.getBoolean("useCustomBroadcast", false);
        this.broadcastMessage = mine.getString("customBroadcast");
        final double configuredBroadcastRadius =
                mine.getDouble("broadcastRadius", settings.defaultBroadcastRadius);
        this.broadcastRadius = Double.isFinite(configuredBroadcastRadius)
                ? configuredBroadcastRadius : settings.defaultBroadcastRadius;
        this.resetDelay = Math.max(1, mine.getInt("resetDelay", settings.defaultResetDelay));

        final HashMap<Material, Double> comp = new HashMap<>();
        if (!mine.contains("composition")) {
            mine.set("composition", new ArrayList<String>());
        } else {
            final MineCompositionParser.Result parsed =
                    MineCompositionParser.parse(mine.getStringList("composition"));
            for (final String invalid : parsed.invalidEntries()) {
                debug("Invalid composition entry for mine " + this.name + ": '"
                        + invalid + "'. Skipping it.");
            }
            for (final MineCompositionParser.Entry entry : parsed.entries()) {
                if (entry.customToken() != null) {
                    debug("Custom block composition entry '" + entry.raw() + "' in mine "
                            + this.name + " is not supported by the material composition "
                            + "and was skipped.");
                    continue;
                }
                final Material material = Materials.resolve(entry.materialName(), entry.data());
                if (material == null) {
                    debug("Invalid composition entry for mine " + this.name + ": '"
                            + entry.raw() + "' (unknown material: "
                            + entry.materialName() + "). Skipping it.");
                    continue;
                }
                comp.put(material, entry.percentage());
            }
        }
        this.composition = comp;

        this.requireAccessPermission = mine.getBoolean("requireAccessPermission",
                settings.protection.requireAccessPermission);
        this.requiredPermission = mine.getString("requiredPermission", "").trim();
        this.requiredRank = mine.getString("requiredRank", "").trim();
        this.requireBreakPermission = mine.getBoolean("requireBreakPermission",
                settings.protection.requireBreakPermission);
        this.compositionOnly = mine.getBoolean("compositionOnly",
                settings.protection.compositionOnly);
        this.constraints = new MineConstraints(
                readMaterials(mine, "constraints.break.materials"),
                readMaterials(mine, "constraints.break.deny"),
                mine.getBoolean("constraints.place.enabled", false),
                readMaterials(mine, "constraints.place.materials"),
                readMaterials(mine, "constraints.place.deny"));
        final int configuredWaveLayers = mine.getInt("wave.layersPerTick", 0);
        this.waveLayersPerTick = configuredWaveLayers <= 0 ? 0
                : Math.min(64, configuredWaveLayers);
        this.waveDirection = MineWaveDirection.parse(
                mine.getString("wave.direction", "TOP_DOWN"));
        this.sellCurrency = normalizeCurrency(mine.getString("drops.currency", "money"));
        this.sellPriority = SellPriority.parseForMine(
                mine.getString("drops.sellPriority", "GLOBAL"));
        this.progression = new MineProgression(
                mine.getBoolean("progression.enabled", false),
                mine.getInt("progression.level", 1),
                mine.getInt("progression.maxLevel", 10),
                Math.max(0L, mine.getLong("progression.minedBlocks", 0L)),
                Math.max(0L, mine.getLong("progression.blocksPerLevel", 50_000L)),
                mine.getDouble("progression.dropBonusPerLevel", 0.05D),
                mine.getDouble("progression.sellBonusPerLevel", 0.05D),
                mine.getDouble("progression.maxBonus", 1.0D));
        final double configuredResetOnPercent = mine.getDouble("resetOnPercent", 0.0D);
        this.resetOnPercent = Double.isFinite(configuredResetOnPercent)
                ? Math.max(0.0D, Math.min(100.0D, configuredResetOnPercent)) : 0.0D;
        final double configuredThreshold =
                mine.getDouble("resetPercent.default", this.resetOnPercent);
        final double configuredMin = mine.getDouble("resetPercent.min", configuredThreshold);
        final double configuredMax = mine.getDouble("resetPercent.max", configuredThreshold);
        this.resetThreshold = new ResetThreshold(configuredThreshold, configuredMin,
                configuredMax, mine.getInt("resetPercent.checkIntervalTicks", 0));
        this.preResetCommands = readCommands(mine, "commands.preReset");
        this.postResetCommands = readCommands(mine, "commands.postReset");
        this.scheduledResetTimes = new ArrayList<>(mine.getStringList("scheduledResetTimes"));
        this.paused = mine.getBoolean("paused", false);
        this.bossBarEnabled = mine.getBoolean("bossbar", true);
        this.worldGuardEnabled = mine.getBoolean("worldguard.enabled", true);
        this.worldGuardFlags = new HashMap<>();
        this.worldGuardFlags.putAll(me.marcdoesntexist.galaxymines.mines.types.BasicMine
                .defaultWorldGuardFlags());
        final org.bukkit.configuration.ConfigurationSection flagSection =
                mine.getConfigurationSection("worldguard.flags");
        if (flagSection != null) {
            for (final String key : flagSection.getKeys(false)) {
                final String value = flagSection.getString(key);
                if (value != null) {
                    this.worldGuardFlags.put(key.toLowerCase(Locale.ROOT), value);
                }
            }
        }
        this.spawn = readSpawn(mine);
        final PluginSettings.Drops dropDefaults = settings.drops;
        this.drops = new MineDrops(
                mine.getBoolean("drops.autoPickup", dropDefaults.autoPickup),
                mine.getBoolean("drops.autoSmelt", dropDefaults.autoSmelt),
                mine.getBoolean("drops.autoBlock", dropDefaults.autoBlock),
                mine.getBoolean("drops.autoSell", dropDefaults.autoSell),
                Math.max(0.0D, mine.getDouble("drops.dropMultiplier",
                        dropDefaults.dropMultiplier)),
                Math.max(0.0D, mine.getDouble("drops.sellMultiplier",
                        dropDefaults.sellMultiplier)),
                MineDrops.parsePolicy(mine.getString("drops.fullInventoryPolicy",
                        dropDefaults.fullInventoryPolicy)));
        this.resetStyle = MineResetStyle.parse(mine.getString("resetStyle", "INSTANT"));
        this.infinite = mine.getBoolean("infinite.enabled", false);
        final double configuredRespawn =
                mine.getDouble("infinite.respawnSeconds", 5.0D);
        this.infiniteRespawnSeconds = Double.isFinite(configuredRespawn)
                ? Math.max(0.05D, Math.min(3600.0D, configuredRespawn)) : 5.0D;
        this.infiniteMaxPending = Math.max(16, Math.min(1_000_000,
                mine.getInt("infinite.maxPendingRespawns", 4096)));

        this.potionEffects = new ArrayList<PotionEffect>();
        if (!this.config.contains("effects.use")) {
            this.config.set("effects.use", false);
        }
        if (!this.config.contains("effects.list")) {
            this.config.set("effects.list", new ArrayList<String>());
        }
        for (final String effectString : this.config.getStringList("effects.list")) {
            try {
                this.potionEffects.add(PotionUtils.parsePotionEffect(effectString));
            } catch (PotionUtils.PotionParseException ex) {
                GalaxyMines.instance.printException(
                        "Error reading effect '" + effectString + "' for mine "
                                + this.name + "!", ex);
            }
        }
    }

    public static Mine loadMine(final MineConfig c) {
        final MineType type = c.getType();
        return switch (type) {
            case CUBOID -> new CuboidMine(c);
            case CIRCULAR -> new CircularMine(c);
            case POLYGON -> new PolygonMine(c);
        };
    }

    private static String normalizeCurrency(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "money";
        }
        return raw.trim().toLowerCase(java.util.Locale.ROOT);
    }


    private static java.util.Set<Material> readMaterials(final FileConfiguration mine,
                                                         final String path) {
        final java.util.EnumSet<Material> materials = java.util.EnumSet.noneOf(Material.class);
        final List<String> configured;
        try {
            configured = mine.getStringList(path);
        } catch (RuntimeException ex) {
            debug("Invalid material list at '" + path + "': expected a list of block names.");
            return materials;
        }
        for (final String raw : configured) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            final Material material = Materials.resolve(raw.trim());
            if (material == null || !material.isBlock()) {
                debug("Ignoring unknown block '" + raw + "' at '" + path + "'.");
                continue;
            }
            materials.add(material);
        }
        return materials;
    }


    private static List<String> readCommands(final FileConfiguration mine, final String path) {
        final List<String> commands = new ArrayList<>();
        final List<String> configured;
        try {
            configured = mine.getStringList(path);
        } catch (RuntimeException ex) {
            debug("Invalid command list at '" + path + "': expected a list of strings.");
            return commands;
        }
        for (final String raw : configured) {
            if (raw == null) {
                continue;
            }
            String command = raw.trim();
            while (command.startsWith("/")) {
                command = command.substring(1).trim();
            }
            if (!command.isEmpty()) {
                commands.add(command);
            }
        }
        return commands;
    }

    private static Location readSpawn(final FileConfiguration mine) {
        final String worldName = mine.getString("spawn.world");
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            debug("Mine spawn world '" + worldName
                    + "' is not loaded; using the top of the mine instead.");
            return null;
        }
        final double x = mine.getDouble("spawn.x");
        final double y = mine.getDouble("spawn.y");
        final double z = mine.getDouble("spawn.z");
        final double yaw = mine.getDouble("spawn.yaw");
        final double pitch = mine.getDouble("spawn.pitch");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Double.isFinite(yaw) || !Double.isFinite(pitch)) {
            debug("Mine spawn for '" + mine.getString("name")
                    + "' contains a non-finite coordinate; using the top of the mine instead.");
            return null;
        }
        return new Location(world, x, y, z, (float) yaw, (float) pitch);
    }

    private static void debug(final String message) {
        final GalaxyMines plugin = GalaxyMines.getInstance();
        if (plugin != null) {
            plugin.debug(message, true);
        }
    }

    public boolean isUseCustomBroadcast() {
        return this.useCustomBroadcast;
    }

    public HashMap<Material, Double> getComposition() {
        return this.composition;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public String getName() {
        return this.name;
    }

    public boolean isNearbyBroadcast() {
        return this.broadcastNearby;
    }

    public boolean isBroadcastOnReset() {
        return this.broadcastOnReset;
    }

    public int getResetDelay() {
        return this.resetDelay;
    }

    public double getBroadcastRadius() {
        return this.broadcastRadius;
    }

    public String getBroadcastMessage() {
        return this.broadcastMessage;
    }

    public World getWorld() {
        return this.world;
    }

    public MineType getType() {
        return this.type;
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public String getAlias() {
        return this.alias;
    }

    public List<PotionEffect> getEffects() {
        return this.potionEffects;
    }

    public boolean requiresAccessPermission() {
        return this.requireAccessPermission;
    }

    public String getRequiredPermission() {
        return this.requiredPermission;
    }

    public String getRequiredRank() {
        return this.requiredRank;
    }

    public boolean requiresBreakPermission() {
        return this.requireBreakPermission;
    }

    public boolean isCompositionOnly() {
        return this.compositionOnly;
    }

    public MineConstraints getConstraints() {
        return this.constraints;
    }

    public int getWaveLayersPerTick() {
        return this.waveLayersPerTick;
    }

    public MineWaveDirection getWaveDirection() {
        return this.waveDirection;
    }

    public MineProgression getProgression() {
        return this.progression;
    }

    public String getSellCurrency() {
        return this.sellCurrency;
    }

    public SellPriority getSellPriority() {
        return this.sellPriority;
    }

    public Location getSpawn() {
        return this.spawn == null ? null : this.spawn.clone();
    }

    public double getResetOnPercent() {
        return this.resetOnPercent;
    }

    public ResetThreshold getResetThreshold() {
        return this.resetThreshold;
    }

    public List<String> getPreResetCommands() {
        return new ArrayList<>(this.preResetCommands);
    }

    public List<String> getPostResetCommands() {
        return new ArrayList<>(this.postResetCommands);
    }

    public List<String> getScheduledResetTimes() {
        return this.scheduledResetTimes;
    }

    public boolean isPaused() {
        return this.paused;
    }

    public boolean isBossBarEnabled() {
        return this.bossBarEnabled;
    }

    public boolean isWorldGuardEnabled() {
        return this.worldGuardEnabled;
    }

    public HashMap<String, String> getWorldGuardFlags() {
        return new HashMap<>(this.worldGuardFlags);
    }

    public MineDrops getDrops() {
        return this.drops;
    }

    public MineResetStyle getResetStyle() {
        return this.resetStyle;
    }


    public boolean isInfinite() {
        return this.infinite;
    }


    public double getInfiniteRespawnSeconds() {
        return this.infiniteRespawnSeconds;
    }


    public int getInfiniteMaxPending() {
        return this.infiniteMaxPending;
    }
}
