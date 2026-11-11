package me.marcdoesntexist.galaxymines.mines.types;


import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.compat.Potions;
import me.marcdoesntexist.galaxymines.config.GalaxyMinesConfiguration;
import me.marcdoesntexist.galaxymines.config.MineCompositionParser;
import me.marcdoesntexist.galaxymines.config.MineConfig;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.events.mine.MineResetCompleteEvent;
import me.marcdoesntexist.galaxymines.events.mine.MineResetEvent;
import me.marcdoesntexist.galaxymines.events.mine.MineUpdateEvent;
import me.marcdoesntexist.galaxymines.holograms.CompatibleHologram;
import me.marcdoesntexist.galaxymines.integration.WorldGuardIntegration;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockItems;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockRegistry;
import me.marcdoesntexist.galaxymines.messages.GalaxyMineMessageManager;
import me.marcdoesntexist.galaxymines.messages.Messages;
import me.marcdoesntexist.galaxymines.mines.*;
import me.marcdoesntexist.galaxymines.storage.StorageManager;
import me.marcdoesntexist.galaxymines.utils.AsyncFileWriter;
import me.marcdoesntexist.galaxymines.utils.BlockPositions;
import me.marcdoesntexist.galaxymines.utils.LongArrayCodec;
import me.marcdoesntexist.galaxymines.utils.LongHashSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

public abstract class BasicMine implements Mine {
    protected final FileConfiguration config;
    protected final LongHashSet brokenBlockPositions;
    protected final World world;
    private final List<PotionEffect> potionEffects;
    protected String name;
    protected String alias;
    protected File saveFile;
    protected boolean enabled;
    protected boolean broadcastOnReset;
    protected String broadcastMessage;
    protected double broadcastRadius;
    protected boolean broadcastNearby;
    protected Map<Material, Double> composition;
    protected int currentResetDelay;
    protected int totalResetDelay;
    protected boolean deleted;
    protected ItemStack icon;
    private int lastSecondBrokenBlocks;
    private boolean useEffects;
    private MineRepopulator.Session resetSession;
    private MineChunkTickets.Handle chunkTickets = MineChunkTickets.none();
    private boolean resetInProgress;
    private boolean infinite;
    private double infiniteRespawnSeconds;
    private int infiniteMaxPending;
    private MineResetPalette resetPalette;
    private boolean requireAccessPermission;
    private String requiredPermission;
    private String requiredRank;
    private boolean requireBreakPermission;
    private boolean compositionOnly;
    private MineConstraints blockConstraints = MineConstraints.NONE;
    private int waveLayersPerTick;
    private MineWaveDirection waveDirection = MineWaveDirection.TOP_DOWN;
    private MineProgression progression = MineProgression.DISABLED;
    private String sellCurrency = "money";
    private me.marcdoesntexist.galaxymines.integration.economy.SellPriority sellPriority =
            me.marcdoesntexist.galaxymines.integration.economy.SellPriority.GLOBAL;
    private Location spawnLocation;
    private double resetOnPercent;
    private ResetThreshold resetThreshold = ResetThreshold.DISABLED;
    private List<String> preResetCommands = new ArrayList<>();
    private List<String> postResetCommands = new ArrayList<>();
    private List<String> scheduledResetTimes;
    private boolean paused;
    private boolean bossBarEnabled;
    private boolean worldGuardEnabled;
    private Map<String, String> worldGuardFlags;
    private MineDrops drops;
    private MineResetStyle resetStyle;
    private int lastScheduledMinute = -1;
    private long lastResetAtMillis;
    private long lastStateSaveMillis;
    private BukkitTask countdownTask;
    private int countdownRemaining;
    private boolean countdownActive;
    private boolean stateInitialized;
    private int resetGeneration;
    private boolean lowFeedbackSent;

    public BasicMine(final MineConfig config) {
        this.name = config.getName();
        this.saveFile = GalaxyMines.getMineFile(this);
        this.enabled = true;
        this.brokenBlockPositions = new LongHashSet();
        this.deleted = false;
        this.broadcastMessage = (GalaxyMinesConfiguration.forceDefaultBroadcastMessage ?
                GalaxyMineMessageManager.globalResetMessage :
                (config.isUseCustomBroadcast() ?
                        config.getBroadcastMessage() :
                        GalaxyMineMessageManager.globalResetMessage));
        this.broadcastOnReset = config.isBroadcastOnReset();
        this.broadcastRadius = config.getBroadcastRadius();
        this.broadcastNearby = config.isNearbyBroadcast();
        this.totalResetDelay = config.getResetDelay() * 60;
        this.currentResetDelay = this.totalResetDelay;
        this.alias = config.getAlias() == null ?
                this.name :
                GalaxyMineMessageManager.getInstance().translateColors(config.getAlias());
        this.world = config.getWorld();
        this.composition = config.getComposition();
        this.config = config.getConfig();
        this.infinite = config.isInfinite();
        this.infiniteRespawnSeconds = config.getInfiniteRespawnSeconds();
        this.infiniteMaxPending = config.getInfiniteMaxPending();
        this.requireAccessPermission = config.requiresAccessPermission();
        this.requiredPermission = config.getRequiredPermission();
        this.requiredRank = config.getRequiredRank();
        this.requireBreakPermission = config.requiresBreakPermission();
        this.compositionOnly = config.isCompositionOnly();
        this.blockConstraints = config.getConstraints();
        this.waveLayersPerTick = config.getWaveLayersPerTick();
        this.waveDirection = config.getWaveDirection();
        this.progression = config.getProgression();
        this.sellCurrency = config.getSellCurrency();
        this.sellPriority = config.getSellPriority();
        this.spawnLocation = config.getSpawn();
        this.resetThreshold = config.getResetThreshold();
        this.resetOnPercent = this.resetThreshold.target();
        this.preResetCommands = new ArrayList<>(config.getPreResetCommands());
        this.postResetCommands = new ArrayList<>(config.getPostResetCommands());
        this.scheduledResetTimes = new ArrayList<>(config.getScheduledResetTimes());
        this.paused = config.isPaused();
        this.bossBarEnabled = config.isBossBarEnabled();
        this.worldGuardEnabled = config.isWorldGuardEnabled();
        this.worldGuardFlags = new LinkedHashMap<>(config.getWorldGuardFlags());
        this.drops = config.getDrops();
        this.resetStyle = config.getResetStyle();
        this.useEffects = config.getConfig().getBoolean("effects.use");
        this.potionEffects = config.getEffects();
        final ConfigurationSection iconsection = config.getConfig().getConfigurationSection("icon");
        ItemStack mineIcon;
        try {
            mineIcon = GalaxyMineMessageManager.getInstance().createIcon(iconsection, this);
        } catch (RuntimeException ex) {
            GalaxyMines.instance.printException(
                    "Invalid icon for mine " + this.name + ": using a default icon.", ex);
            mineIcon = ItemUtils.createItem(Material.STONE, this.name);
        }
        this.icon = mineIcon;
    }

    public BasicMine(final String name, final World world, final ItemStack icon) {
        this.name = name;
        this.saveFile = GalaxyMines.getMineFile(this);
        this.enabled = true;
        this.brokenBlockPositions = new LongHashSet();
        this.deleted = false;
        this.alias = name;
        this.world = world;
        (this.composition = new HashMap<>()).put(Material.STONE, 100.0);
        this.broadcastOnReset = true;
        this.broadcastRadius = 50.0;
        this.broadcastMessage = GalaxyMineMessageManager.globalResetMessage;
        this.broadcastNearby = false;
        this.totalResetDelay = 300;
        this.currentResetDelay = this.totalResetDelay;
        this.icon = icon;
        this.config = GalaxyMines.getYAMLMineFile(this);
        this.potionEffects = new ArrayList<PotionEffect>();
        this.requiredPermission = "";
        this.requiredRank = "";
        this.resetThreshold = ResetThreshold.DISABLED;
        this.scheduledResetTimes = new ArrayList<>();
        this.bossBarEnabled = true;
        this.worldGuardEnabled = true;
        this.worldGuardFlags = defaultWorldGuardFlags();
        this.drops = MineDrops.VANILLA;
        this.resetStyle = MineResetStyle.INSTANT;
    }

    private static double sanitizePercentage(final double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    private static List<String> materialNames(final Collection<Material> materials) {
        final List<String> names = new ArrayList<>();
        if (materials != null) {
            for (final Material material : materials) {
                if (material != null) {
                    names.add(material.name());
                }
            }
        }
        return names;
    }

    private static long blockKey(final Block block) {
        return BlockPositions.pack(block.getX(), block.getY(), block.getZ());
    }

    public static Map<String, String> defaultWorldGuardFlags() {
        final Map<String, String> flags = new LinkedHashMap<>();
        flags.put("build", "allow");
        flags.put("pvp", "deny");
        flags.put("mob-spawning", "deny");
        return flags;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ItemStack getIcon() {
        return this.icon;
    }

    public void setIcon(final ItemStack icon) {
        this.icon = icon;
    }

    public int getResetMinutesDelay() {
        return this.totalResetDelay / 60;
    }

    public void setResetMinutesDelay(final int resetMinutesDelay) {
        this.totalResetDelay = resetMinutesDelay * 60;
        this.save();
    }

    @Override
    public boolean broadcastOnReset() {
        return this.broadcastOnReset;
    }

    @Override
    public boolean broadcastToNearbyOnly() {
        return this.broadcastNearby;
    }

    @Override
    public double broadcastRadius() {
        return this.broadcastRadius;
    }

    @Override
    public boolean isBroadcastOnReset() {
        return this.broadcastOnReset;
    }

    @Override
    public void setBroadcastOnReset(final boolean broadcastOnReset) {
        this.broadcastOnReset = broadcastOnReset;
        this.save();
    }

    @Override
    public void setResetDelay(final int resetDelay) {
        this.totalResetDelay = Math.max(1, resetDelay) * 60;
        this.currentResetDelay = this.totalResetDelay;
        this.save();
    }

    @Override
    public void setNearbyBroadcast(final boolean nearbyBroadcast) {
        this.broadcastNearby = nearbyBroadcast;
        this.save();
    }

    @Override
    public void setBroadcastRange(final double broadcastRadius) {
        this.broadcastRadius = broadcastRadius;
        this.save();
    }

    @Override
    public boolean requiresAccessPermission() {
        return this.requireAccessPermission;
    }

    @Override
    public String getRequiredPermission() {
        return this.requiredPermission;
    }

    @Override
    public String getRequiredRank() {
        return this.requiredRank;
    }

    @Override
    public boolean requiresBreakPermission() {
        return this.requireBreakPermission;
    }

    @Override
    public boolean isCompositionOnly() {
        return this.compositionOnly;
    }

    @Override
    public MineConstraints getBlockConstraints() {
        return this.blockConstraints;
    }

    @Override
    public void setBlockConstraints(final MineConstraints constraints) {
        this.blockConstraints = constraints == null ? MineConstraints.NONE : constraints;
        this.save();
    }

    @Override
    public int getWaveLayersPerTick() {
        return this.waveLayersPerTick;
    }

    @Override
    public void setWaveLayersPerTick(final int layers) {
        this.waveLayersPerTick = Math.max(0, Math.min(64, layers));
        this.save();
    }

    @Override
    public MineWaveDirection getWaveDirection() {
        return this.waveDirection;
    }

    @Override
    public void setWaveDirection(final MineWaveDirection direction) {
        this.waveDirection = direction == null ? MineWaveDirection.TOP_DOWN : direction;
        this.save();
    }

    @Override
    public MineProgression getProgression() {
        return this.progression;
    }

    @Override
    public void setProgression(final MineProgression progression) {
        this.progression = progression == null ? MineProgression.DISABLED : progression;
        this.save();
    }

    @Override
    public String getSellCurrency() {
        return this.sellCurrency;
    }

    @Override
    public void setSellCurrency(final String currencyId) {
        this.sellCurrency = currencyId == null || currencyId.isBlank() ? "money"
                : currencyId.trim().toLowerCase(java.util.Locale.ROOT);
        this.save();
    }

    @Override
    public me.marcdoesntexist.galaxymines.integration.economy.SellPriority getSellPriority() {
        return this.sellPriority;
    }

    @Override
    public void setSellPriority(
            final me.marcdoesntexist.galaxymines.integration.economy.SellPriority priority) {
        this.sellPriority = priority == null
                ? me.marcdoesntexist.galaxymines.integration.economy.SellPriority.GLOBAL
                : priority;
        this.save();
    }

    @Override
    public Location getSpawnLocation() {
        if (this.spawnLocation != null && this.world.equals(this.spawnLocation.getWorld())) {
            return this.spawnLocation.clone();
        }
        return this.getTopCenterLocation();
    }

    @Override
    public void setSpawnLocation(final Location location) {
        this.spawnLocation = location == null ? null : location.clone();
        this.save();
    }

    @Override
    public Location getSafeTeleportLocation() {
        final Location base = this.getSpawnLocation();
        if (base == null || base.getWorld() == null) {
            return this.getTopCenterLocation();
        }
        if (this.contains(base)) {
            base.setY(this.getUpperY() + 2);
        }
        final org.bukkit.World bukkitWorld = base.getWorld();
        final int minY = bukkitWorld.getMinHeight() + 1;
        final int maxY = Math.min(bukkitWorld.getMaxHeight() - 2, base.getBlockY() + 32);
        for (int y = Math.max(minY, base.getBlockY()); y <= maxY; ++y) {
            final Block feet = bukkitWorld.getBlockAt(base.getBlockX(), y, base.getBlockZ());
            final Block head = feet.getRelative(0, 1, 0);
            if (feet.isPassable() && head.isPassable() && !feet.isLiquid() && !head.isLiquid()) {
                base.setY(y);
                return base;
            }
        }
        base.setY(this.getUpperY() + 2);
        return base;
    }

    @Override
    public double getResetOnPercent() {
        return this.resetThreshold.target();
    }

    @Override
    public void setResetOnPercent(final double percentage) {
        final double value = sanitizePercentage(percentage);
        this.resetThreshold = new ResetThreshold(value, value, value,
                this.resetThreshold.checkIntervalTicks());
        this.resetOnPercent = value;
        this.save();
    }

    @Override
    public void setResetPercentDefault(final double percentage) {
        final double value = sanitizePercentage(percentage);
        this.resetThreshold = new ResetThreshold(value, this.resetThreshold.minPercent(),
                this.resetThreshold.maxPercent(), this.resetThreshold.checkIntervalTicks());
        this.resetOnPercent = this.resetThreshold.target();
        this.save();
    }

    @Override
    public double getResetPercentMin() {
        return this.resetThreshold.minPercent();
    }

    @Override
    public double getResetPercentMax() {
        return this.resetThreshold.maxPercent();
    }

    @Override
    public int getResetPercentCheckIntervalTicks() {
        return this.resetThreshold.checkIntervalTicks();
    }

    @Override
    public void setResetPercentCheckIntervalTicks(final int ticks) {
        this.resetThreshold = new ResetThreshold(this.resetThreshold.defaultPercent(),
                this.resetThreshold.minPercent(), this.resetThreshold.maxPercent(),
                Math.max(0, Math.min(72_000, ticks)));
        this.resetOnPercent = this.resetThreshold.target();
        this.save();
    }

    @Override
    public double getResetThresholdTarget() {
        return this.resetThreshold.target();
    }

    @Override
    public List<String> getPreResetCommands() {
        return new ArrayList<>(this.preResetCommands);
    }

    @Override
    public void setPreResetCommands(final List<String> commands) {
        this.preResetCommands = commands == null ? new ArrayList<>()
                : new ArrayList<>(commands);
        this.save();
    }

    @Override
    public List<String> getPostResetCommands() {
        return new ArrayList<>(this.postResetCommands);
    }

    @Override
    public void setPostResetCommands(final List<String> commands) {
        this.postResetCommands = commands == null ? new ArrayList<>()
                : new ArrayList<>(commands);
        this.save();
    }

    @Override
    public void setResetPercentRange(final double min, final double max) {
        final double low = Math.min(sanitizePercentage(min), sanitizePercentage(max));
        final double high = Math.max(sanitizePercentage(min), sanitizePercentage(max));
        this.resetThreshold = new ResetThreshold(0.0D, low, high,
                this.resetThreshold.checkIntervalTicks());
        this.resetOnPercent = this.resetThreshold.target();
        this.save();
    }

    @Override
    public List<String> getScheduledResetTimes() {
        return new ArrayList<>(this.scheduledResetTimes);
    }

    @Override
    public void setScheduledResetTimes(final List<String> times) {
        this.scheduledResetTimes = times == null ? new ArrayList<>() : new ArrayList<>(times);
        this.lastScheduledMinute = -1;
        this.save();
    }

    @Override
    public boolean isPaused() {
        return this.paused;
    }

    @Override
    public void setPaused(final boolean paused) {
        this.paused = paused;
        if (paused) {
            this.cancelCountdown();
        }
        this.save();
    }

    @Override
    public boolean isResetInProgress() {
        return this.resetInProgress;
    }

    @Override
    public boolean isInfinite() {
        return this.infinite;
    }

    @Override
    public double getInfiniteRespawnSeconds() {
        return this.infiniteRespawnSeconds;
    }

    @Override
    public int getInfiniteMaxPending() {
        return this.infiniteMaxPending;
    }

    @Override
    public boolean isBossBarEnabled() {
        return this.bossBarEnabled;
    }

    @Override
    public boolean isWorldGuardEnabled() {
        return this.worldGuardEnabled;
    }

    @Override
    public void setWorldGuardEnabled(final boolean enabled) {
        this.worldGuardEnabled = enabled;
        this.save();
    }

    @Override
    public Map<String, String> getWorldGuardFlags() {
        return new LinkedHashMap<>(this.worldGuardFlags);
    }

    @Override
    public void setWorldGuardFlag(final String flag, final String value) {
        if (flag == null || flag.isBlank()) {
            return;
        }
        if (value == null || value.isBlank()) {
            this.worldGuardFlags.remove(flag);
        } else {
            this.worldGuardFlags.put(flag, value);
        }
        this.save();
    }

    @Override
    public MineDrops getDrops() {
        return this.drops;
    }

    @Override
    public void setDrops(final MineDrops drops) {
        this.drops = drops == null ? MineDrops.VANILLA : drops;
        this.save();
    }

    @Override
    public MineResetStyle getResetStyle() {
        return this.resetStyle;
    }

    @Override
    public void setResetStyle(final MineResetStyle style) {
        this.resetStyle = style == null ? MineResetStyle.INSTANT : style;
        this.save();
    }

    @Override
    public void renameTo(final String newName) {
        if (newName == null || newName.isBlank() || newName.equals(this.name)) {
            return;
        }
        final File oldFile = GalaxyMines.getMineFile(this);
        this.name = newName;
        this.saveFile = GalaxyMines.getMineFile(this);
        this.save();
        if (!oldFile.equals(this.saveFile)) {
            AsyncFileWriter.delete(oldFile);
        }
    }

    public double getBroadcastRadius() {
        return this.broadcastRadius;
    }

    public String getBroadcastMessage() {
        return this.broadcastMessage;
    }

    public void setBroadcastMessage(final String broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
        this.save();
    }

    @Override
    public String getAlias() {
        return this.alias;
    }

    @Override
    public void setAlias(final String alias) {
        this.alias = alias;
        this.save();
    }

    @Override
    public void secondCountdown() {
        if (this.deleted || this.paused) {
            return;
        }
        if (this.useEffects) {
            for (final PotionEffect e : this.getEffects()) {
                for (final Player p : this.getPlayersInside()) {
                    p.addPotionEffect(e, true);
                }
            }
        }
        if (this.countdownActive || this.resetInProgress) {
            return;
        }
        this.tickScheduledReset();
        if (this.infinite) {
            this.lastSecondBrokenBlocks = 0;
            this.persistStateIfDue();
            return;
        }
        --this.currentResetDelay;
        if (this.isBelowResetPercentage()) {
            if (!this.lowFeedbackSent) {
                this.lowFeedbackSent = true;
                final PluginSettings.Feedback feedback = PluginSettings.get().feedback;
                MineFeedback.playAll(this.resetAudience(), feedback.lowSound,
                        feedback.lowParticle);
            }
            this.startResetCountdown();
        } else if (this.currentResetDelay <= 0) {
            this.startResetCountdown();
        }
        this.lastSecondBrokenBlocks = 0;
        this.persistStateIfDue();
    }

    private void startResetCountdown() {
        if (this.deleted || this.paused || this.countdownActive || this.resetInProgress) {
            return;
        }
        if (this.isResetCooldownActive()) {
            return;
        }
        final PluginSettings.Reset settings = PluginSettings.get().reset;
        if (!settings.countdownEnabled || settings.countdownSeconds.length == 0) {
            this.reset();
            return;
        }
        this.countdownRemaining = settings.countdownSeconds[0];
        this.countdownActive = true;
        this.countdownTask = Bukkit.getScheduler().runTaskTimer(GalaxyMines.getInstance(),
                this::tickResetCountdown, 0L, 20L);
    }

    private void tickResetCountdown() {
        if (this.deleted || this.paused) {
            this.cancelCountdown();
            return;
        }
        if (this.countdownRemaining <= 0) {
            this.cancelCountdown();
            this.reset();
            return;
        }
        if (this.isCountdownAnnouncement(this.countdownRemaining)) {
            this.announceResetCountdown(this.countdownRemaining);
        }
        --this.countdownRemaining;
    }

    private void announceResetCountdown(final int seconds) {
        final PluginSettings.Reset settings = PluginSettings.get().reset;
        final Component chat = Text.parse(Messages.get("messages.reset.countdown",
                "The mine %alias% resets in %seconds% seconds.", "%alias%", this.alias,
                "%seconds%", String.valueOf(seconds)));
        final Component title = Text.parse(Messages.get("messages.reset.countdownTitle",
                "%alias%", "%alias%", this.alias));
        final Component actionBar = Text.parse(Messages.get("messages.reset.countdownActionBar",
                "Reset in %seconds%s", "%seconds%", String.valueOf(seconds)));
        for (final Player player : this.resetAudience()) {
            player.sendMessage(chat);
            if (settings.countdownTitle) {
                player.showTitle(Title.title(title, Component.empty(), Title.Times.times(
                        Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(200))));
            }
            if (settings.countdownActionBar) {
                player.sendActionBar(actionBar);
            }
            if (settings.countdownSound != null && !settings.countdownSound.isBlank()) {
                try {
                    player.playSound(player.getLocation(), settings.countdownSound, 0.7F, 1.0F);
                } catch (IllegalArgumentException ex) {
                    GalaxyMines.getInstance().debug("Unknown countdown sound: "
                            + settings.countdownSound, true);
                }
            }
        }
    }

    private boolean isCountdownAnnouncement(final int seconds) {
        for (final int configured : PluginSettings.get().reset.countdownSeconds) {
            if (configured == seconds) {
                return true;
            }
        }
        return false;
    }

    private List<Player> resetAudience() {
        final List<Player> players = new ArrayList<>(this.getPlayersInside());
        if (!this.broadcastNearby) {
            return players;
        }
        for (final Player player : this.world.getPlayers()) {
            if (players.contains(player)) {
                continue;
            }
            if (player.getLocation().distanceSquared(this.getCenter())
                    <= this.broadcastRadius * this.broadcastRadius) {
                players.add(player);
            }
        }
        return players;
    }

    private void tickScheduledReset() {
        if (this.scheduledResetTimes.isEmpty()) {
            return;
        }
        final LocalTime now = LocalTime.now();
        final int minute = now.getHour() * 60 + now.getMinute();
        if (minute == this.lastScheduledMinute) {
            return;
        }
        this.lastScheduledMinute = minute;
        for (final String configured : this.scheduledResetTimes) {
            if (configured == null) {
                continue;
            }
            try {
                final String[] parts = configured.trim().split(":", 2);
                if (parts.length != 2) {
                    continue;
                }
                if (Integer.parseInt(parts[0]) == now.getHour()
                        && Integer.parseInt(parts[1]) == now.getMinute()) {
                    this.startResetCountdown();
                    return;
                }
            } catch (NumberFormatException ex) {
                GalaxyMines.getInstance().debug("Invalid scheduled reset time '" + configured
                        + "' for mine " + this.name, true);
            }
        }
    }

    private boolean isBelowResetPercentage() {
        return this.getVolume() > 0 && this.resetThreshold.enabled()
                && this.resetThreshold.reached(this.getPercentageRemaining(), System.nanoTime());
    }

    private boolean isResetCooldownActive() {
        final int cooldown = PluginSettings.get().reset.minCooldownSeconds;
        return cooldown > 0 && this.lastResetAtMillis > 0
                && System.currentTimeMillis() - this.lastResetAtMillis < cooldown * 1000L;
    }

    private void persistStateIfDue() {
        final int interval = PluginSettings.get().reset.stateSaveSeconds;
        if (interval <= 0) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - this.lastStateSaveMillis >= interval * 1000L) {
            this.lastStateSaveMillis = now;
            this.save();
        }
    }

    @Override
    public int averageBreakSpeed() {
        return this.lastSecondBrokenBlocks;
    }

    @Override
    public List<String> getInfo() {
        final String[] basic =
                {"%header%", "$3Name: $2" + this.getName(), "$3Alias: $2" + this.getAlias(),
                        "$3Type: $2" + this.getType(),
                        "$3World: $2" + this.getCenter().getWorld().getName(),
                        "$3Location: $2" + this.getCenter().getBlockX() + ", " + this.getCenter()
                                .getBlockY() + ", " + this.getCenter().getBlockZ(),
                        "$3Broadcast on reset: $2" + this.broadcastOnReset(),
                        "$3Nearby broadcast: $2" + this.broadcastToNearbyOnly(),
                        "$3Broadcast radius: $2" + this.broadcastRadius(), "$3Composition: $2"};
        final ArrayList<String> comp = new ArrayList<String>(Arrays.asList(basic));
        for (final Material m : this.getMaterials()) {
            comp.add("$3" + m.name() + " $2= $1" + this.composition.get(m));
        }
        return comp;
    }

    @Override
    public boolean wasAlreadyBroken(final Block b) {
        return !this.brokenBlockPositions.contains(blockKey(b));
    }

    @Override
    public float getPercentageMined() {
        if (this.brokenBlockPositions.isEmpty()) {
            return 0.0f;
        }
        final double percentage = this.brokenBlockPositions.size() * 100.0f / this.getVolume();
        return (float) Math.round(percentage);
    }

    @Override
    public double getPercentage(final Material material) {
        if (material == null || this.composition == null) {
            return 0.0;
        }
        final Double percentage = this.composition.get(material);
        return percentage == null ? 0.0 : percentage;
    }

    @Override
    public float getPercentageRemaining() {
        if (this.getRemainingBlocks() <= 0) {
            return 0.0f;
        }
        final double percentage = this.getRemainingBlocks() * 100.0 / this.getVolume();
        return (float) Math.round(percentage);
    }

    @Override
    public int getMinedBlocks() {
        return this.getVolume() - this.getRemainingBlocks();
    }

    @Override
    public int getRemainingBlocks() {
        return Math.max(0, this.getVolume() - this.brokenBlockPositions.size());
    }

    @Override
    public List<Player> getPlayersInside() {
        final ArrayList<Player> players = new ArrayList<Player>();
        for (final Player p : this.world.getPlayers()) {
            if (this.contains(p)) {
                players.add(p);
            }
        }
        return players;
    }

    @Override
    public void setBlockAsBroken(final Block block) {
        if (this.resetInProgress || !this.contains(block)) {
            return;
        }
        if (this.infinite) {
            InfiniteRespawnService.schedule(this, block.getX(), block.getY(), block.getZ());
            this.stateInitialized = true;
            ++this.lastSecondBrokenBlocks;
            this.fireMineUpdate();
            return;
        }
        if (!this.brokenBlockPositions.add(blockKey(block))) {
            return;
        }
        this.stateInitialized = true;
        ++this.lastSecondBrokenBlocks;
        if (this.isBelowResetPercentage()) {
            this.startResetCountdown();
        }
        this.fireMineUpdate();
    }

    private void fireMineUpdate() {
        if (MineUpdateEvent.getHandlerList().getRegisteredListeners().length != 0) {
            new MineUpdateEvent(this).call();
        }
        if (this instanceof HologramCompatible) {
            final HologramCompatible hc = (HologramCompatible) this;
            hc.softHologramUpdate();
        }
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean containsMaterial(final Material material) {
        return material != null && this.composition != null
                && this.composition.containsKey(material);
    }

    @Override
    public void removeMaterial(final Material material) {
        if (material == null || this.composition.remove(material) == null) {
            GalaxyMines.instance.debug(
                    "Tried to remove material " + material + " from mine " + this.name
                            + ", but it isn't a part of the composition.", 2);
        }
        this.invalidateResetPalette();
        this.save();
    }

    @Override
    public double getFreePercentage() {
        return CompositionMath.free(this.getTotalPercentage());
    }

    @Override
    public double getExceedingTotal() {
        return CompositionMath.excess(this.getTotalPercentage());
    }

    @Override
    public void setMaterialPercentage(final Material material, double percentage) {
        if (material == null) {
            return;
        }
        if (percentage < 0.0) {
            percentage = 0.0;
        }
        GalaxyMines.instance.debug(
                "The percentage of " + material.name() + " of mine "
                        + this.name + " was changed to " + percentage + ".", 2);
        this.composition.put(material, percentage);
        this.invalidateResetPalette();
        this.save();
    }

    @Override
    public boolean isDeleted() {
        return this.deleted;
    }

    @Override
    public Map<Material, Double> getComposition() {
        return this.composition;
    }

    @Override
    public void setComposition(final Map<Material, Double> composition) {
        this.composition = composition == null ? new HashMap<>() : composition;
        this.invalidateResetPalette();
    }

    public MineResetPalette getResetPalette() {
        if (this.resetPalette == null) {
            this.resetPalette = new MineResetPalette(this);
        }
        return this.resetPalette;
    }

    private void invalidateResetPalette() {
        this.resetPalette = null;
    }

    private void cancelReset() {
        ++this.resetGeneration;
        if (this.resetSession != null) {
            MineRepopulator.cancel(this.resetSession);
            this.resetSession = null;
        }
        this.releaseChunkTickets();
        this.cancelCountdown();
        this.resetInProgress = false;
    }


    private void acquireChunkTickets() {
        this.releaseChunkTickets();
        final PluginSettings.Reset settings = PluginSettings.get().reset;
        if (!settings.preloadChunks) {
            return;
        }
        this.chunkTickets = MineChunkTickets.acquire(GalaxyMines.getInstance(), this.world,
                this.getBounds(), settings.maxPreloadChunks);
    }

    private void releaseChunkTickets() {
        this.chunkTickets.release();
        this.chunkTickets = MineChunkTickets.none();
    }

    private void cancelCountdown() {
        if (this.countdownTask != null) {
            this.countdownTask.cancel();
            this.countdownTask = null;
        }
        this.countdownActive = false;
        this.countdownRemaining = 0;
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public List<Material> getMaterials() {
        return new ArrayList<>(this.composition.keySet());
    }

    @Override
    public String toString() {
        final JsonObject obj = new JsonObject();
        obj.addProperty("name", this.name);
        obj.addProperty("location", this.getCenter().toString());
        return obj.toString();
    }

    @Override
    public int getTimeToNextReset() {
        return this.countdownActive ? this.countdownRemaining : Math.max(0, this.currentResetDelay);
    }

    @Override
    public boolean restoreState() {
        if (!this.config.contains("state")) {
            return false;
        }
        this.stateInitialized = true;
        final int savedDelay = this.config.getInt("state.resetDelay", this.totalResetDelay);
        this.currentResetDelay = Math.max(0, Math.min(this.totalResetDelay, savedDelay));
        final String encoded = this.config.getString("state.broken", "");
        if (encoded != null && !encoded.isBlank()) {
            try {
                LongArrayCodec.decode(encoded, this.brokenBlockPositions);
            } catch (IllegalArgumentException ex) {
                GalaxyMines.getInstance().debug("Could not restore broken blocks for mine "
                        + this.name + ": " + ex.getMessage(), true);
            }
        }
        return true;
    }

    @Override
    public boolean isCompletelyBroken() {
        return this.getRemainingBlocks() <= 0;
    }

    @Override
    public void clear() {
        MineBulkOperations.cancel(this);
        final BlockData air = Material.AIR.createBlockData();
        for (final Block b : this) {
            b.setBlockData(air, false);
        }
    }

    @Override
    public void fill(final ItemStack item) {
        MineBulkOperations.cancel(this);
        final String token = CustomBlockItems.tokenOf(item);
        final BlockData fallback = item.getType().isBlock() && !item.getType().isAir()
                ? item.getType().createBlockData()
                : Material.NOTE_BLOCK.createBlockData();
        for (final Block b : this) {
            if (token != null && CustomBlockRegistry.place(token, b.getLocation())) {
                continue;
            }
            b.setBlockData(fallback, false);
        }
    }

    @Override
    public void disable() {
        MineBulkOperations.cancel(this);
        InfiniteRespawnService.clear(this);
        this.cancelReset();
        this.setEnabled(false);
        if (this instanceof HologramCompatible) {
            final HologramCompatible hc = (HologramCompatible) this;
            for (final CompatibleHologram hologram : hc.getHolograms()) {
                hologram.delete();
            }
            hc.getHolograms().clear();
        }
        MineManager.unregisterMine(this);
        this.save();
    }

    @Override
    public void delete() {
        MineBulkOperations.cancel(this);
        InfiniteRespawnService.clear(this);
        this.cancelReset();
        this.clear();
        this.setEnabled(false);
        WorldGuardIntegration.onMineDeleted(this);
        GalaxyMines.getMineFile(this).delete();
        MineManager.unregisterMine(this);
        this.deleted = true;
        if (this instanceof HologramCompatible) {
            final HologramCompatible hc = (HologramCompatible) this;
            for (final CompatibleHologram hologram : hc.getHolograms()) {
                hologram.delete();
            }
        }
    }

    @Override
    public Location getTopCenterLocation() {
        return new Location(this.world, this.getCenter().getX(), this.getUpperY() + 1,
                this.getCenter().getZ());
    }

    @Override
    public double getTotalPercentage() {
        return this.composition == null ? 0.0 : CompositionMath.total(this.composition.values());
    }

    @Override
    public int getTotalMaterials() {
        if (this.composition == null) {
            return 0;
        }
        return this.composition.keySet().size();
    }

    @Override
    public boolean contains(final Block block) {
        return this.world.equals(block.getWorld())
                && this.contains(block.getX(), block.getY(), block.getZ());
    }

    @Override
    public boolean contains(final Location l) {
        return this.world.equals(l.getWorld()) && this.contains(l.getX(), l.getY(), l.getZ());
    }

    @Override
    public boolean contains(final Player p) {
        return this.contains(p.getLocation());
    }

    @Override
    public boolean useEffects() {
        return this.useEffects;
    }

    @Override
    public List<PotionEffect> getEffects() {
        return this.potionEffects;
    }

    @Override
    public void addPotionEffect(final PotionEffect effect) {
        final PotionEffectType type = effect.getType();
        if (this.getEffect(type) != null) {
            this.removePotionEffect(type);
        }
        this.potionEffects.add(effect);
    }

    @Override
    public void removePotionEffect(final PotionEffectType effectType) {
        this.potionEffects.removeIf(e -> e.getType().equals(effectType));
    }

    @Override
    public void clearEffects() {
        this.potionEffects.clear();
    }

    @Override
    public FileConfiguration exportConfiguration() {
        return this.getBasicSavedConfig();
    }

    public FileConfiguration getBasicSavedConfig() {
        final FileConfiguration c = GalaxyMines.getYAMLMineFile(this);
        c.set("enabled", this.enabled);
        c.set("name", this.name);
        c.set("alias", this.alias);
        c.set("world", this.world.getName());
        c.set("type", this.getType().name());
        c.set("resetDelay", this.getResetMinutesDelay());
        c.set("broadcastOnReset", this.broadcastOnReset);
        c.set("broadcastToNearbyOnly", this.broadcastNearby);
        c.set("broadcastRadius", this.broadcastRadius);
        c.set("requireAccessPermission", this.requireAccessPermission);
        c.set("requiredPermission", this.requiredPermission);
        c.set("requiredRank", this.requiredRank);
        c.set("requireBreakPermission", this.requireBreakPermission);
        c.set("compositionOnly", this.compositionOnly);
        c.set("constraints.break.materials", materialNames(this.blockConstraints.breakAllowed()));
        c.set("constraints.break.deny", materialNames(this.blockConstraints.breakDenied()));
        c.set("constraints.place.enabled", this.blockConstraints.isPlaceEnabled());
        c.set("constraints.place.materials", materialNames(this.blockConstraints.placeAllowed()));
        c.set("constraints.place.deny", materialNames(this.blockConstraints.placeDenied()));
        c.set("wave.layersPerTick", this.waveLayersPerTick);
        c.set("wave.direction", this.waveDirection.name());
        c.set("progression.enabled", this.progression.enabled());
        c.set("progression.level", this.progression.level());
        c.set("progression.minedBlocks", this.progression.minedBlocks());
        c.set("progression.maxLevel", this.progression.maxLevel());
        c.set("progression.blocksPerLevel", this.progression.blocksPerLevel());
        c.set("progression.dropBonusPerLevel", this.progression.dropBonusPerLevel());
        c.set("progression.sellBonusPerLevel", this.progression.sellBonusPerLevel());
        c.set("progression.maxBonus", this.progression.maxBonus());
        c.set("resetOnPercent", this.getResetOnPercent());
        c.set("resetPercent.default", this.resetThreshold.defaultPercent());
        c.set("resetPercent.min", this.resetThreshold.minPercent());
        c.set("resetPercent.max", this.resetThreshold.maxPercent());
        c.set("resetPercent.checkIntervalTicks", this.resetThreshold.checkIntervalTicks());
        c.set("commands.preReset", this.preResetCommands);
        c.set("commands.postReset", this.postResetCommands);
        c.set("scheduledResetTimes", this.scheduledResetTimes);
        c.set("paused", this.paused);
        c.set("bossbar", this.bossBarEnabled);
        c.set("worldguard.enabled", this.worldGuardEnabled);
        c.set("worldguard.flags", this.worldGuardFlags);
        c.set("resetStyle", this.resetStyle.name());
        c.set("infinite.enabled", this.infinite);
        c.set("infinite.respawnSeconds", this.infiniteRespawnSeconds);
        c.set("infinite.maxPendingRespawns", this.infiniteMaxPending);
        c.set("drops.autoPickup", this.drops.autoPickup());
        c.set("drops.autoSmelt", this.drops.autoSmelt());
        c.set("drops.autoBlock", this.drops.autoBlock());
        c.set("drops.autoSell", this.drops.autoSell());
        c.set("drops.dropMultiplier", this.drops.dropMultiplier());
        c.set("drops.sellMultiplier", this.drops.sellMultiplier());
        c.set("drops.fullInventoryPolicy", this.drops.fullInventoryPolicy().name());
        c.set("drops.currency", this.sellCurrency);
        c.set("drops.sellPriority", this.sellPriority.name());
        c.set("spawn", null);
        if (this.spawnLocation != null && this.spawnLocation.getWorld() != null) {
            c.set("spawn.world", this.spawnLocation.getWorld().getName());
            c.set("spawn.x", this.spawnLocation.getX());
            c.set("spawn.y", this.spawnLocation.getY());
            c.set("spawn.z", this.spawnLocation.getZ());
            c.set("spawn.yaw", this.spawnLocation.getYaw());
            c.set("spawn.pitch", this.spawnLocation.getPitch());
        }
        if (this.stateInitialized) {
            c.set("state.resetDelay", this.currentResetDelay);
            c.set("state.broken", LongArrayCodec.encode(this.brokenBlockPositions));
        } else {
            c.set("state", null);
        }
        if (!c.contains("icon")) {
            c.createSection("icon");
            c.set("icon.type", this.icon.getType().name());
            final ItemMeta iconMeta = this.icon.getItemMeta();
            if (iconMeta != null) {
                c.set("icon.name", iconMeta.getDisplayName());
            }
            c.set("icon.lore", ItemUtils.getLore(this.icon));
        }
        final ArrayList<String> effects = new ArrayList<String>();
        for (final PotionEffect e : this.potionEffects) {
            effects.add(
                    Potions.name(e.getType()) + ":" + e.getDuration() / 20 + ":" + e.getAmplifier());
        }
        c.createSection("effects");
        c.set("effects.use", this.useEffects);
        c.set("effects.list", effects);
        final ArrayList<String> comp = new ArrayList<String>();
        for (final Material m : this.composition.keySet()) {
            comp.add(MineCompositionParser.format(m.name(), this.composition.get(m)));
        }
        c.set("composition", comp);
        if (this instanceof HologramCompatible) {
            final HologramCompatible hc = (HologramCompatible) this;
            c.set("hologramsText", hc.getHologramsLines());
            c.set("useCustomHologramText", hc.useCustomHologramText());
        }
        return c;
    }

    @Override
    public void reset() {
        if (this.isDeleted() || this.resetInProgress || this.paused) {
            return;
        }
        if (this.isResetCooldownActive()) {
            return;
        }
        MineBulkOperations.cancel(this);
        InfiniteRespawnService.clear(this);
        if (this.countdownActive) {
            this.cancelCountdown();
        }
        final MineResetEvent event = (MineResetEvent) new MineResetEvent(this).call();
        if (event.isCancelled()) {
            GalaxyMines.getInstance()
                    .debug("Reset event for mine " + this.name + " was cancelled", 2);
            this.currentResetDelay = this.totalResetDelay;
            return;
        }
        GalaxyMines.getInstance().debug("Resetting mine " + this.name, 2);
        ResetCommandRunner.run("pre", this, this.preResetCommands);
        if (PluginSettings.get().reset.safeTeleport) {
            final Location safe = this.getSafeTeleportLocation();
            for (final Player p : this.getPlayersInside()) {
                p.teleportAsync(safe.clone());
            }
        }
        this.currentResetDelay = this.totalResetDelay;
        this.lastResetAtMillis = System.currentTimeMillis();
        this.resetInProgress = true;
        final int generation = ++this.resetGeneration;
        if (PluginSettings.get().reset.preloadChunks) {
            this.preloadChunks().whenComplete((ignored, error) -> Bukkit.getScheduler()
                    .runTask(GalaxyMines.getInstance(), () -> {
                        if (error != null) {
                            GalaxyMines.instance.printException(
                                    "Could not preload chunks for mine " + this.name, error);
                        }
                        if (generation != this.resetGeneration || this.deleted) {
                            return;
                        }
                        this.startRepopulation();
                    }));
        } else {
            this.startRepopulation();
        }
    }

    private void startRepopulation() {
        if (this.deleted || !this.resetInProgress) {
            return;
        }
        try {
            this.acquireChunkTickets();
            ResetMetrics.forMine(this.name).begin();
            this.resetSession = MineRepopulator.start(this, this::finishReset);
        } catch (RuntimeException ex) {
            this.abortReset();
            GalaxyMines.instance.printException("Could not start reset for " + this.name, ex);
        }
    }


    public void abortReset() {
        ++this.resetGeneration;
        if (this.resetSession != null) {
            MineRepopulator.cancel(this.resetSession);
            this.resetSession = null;
        }
        this.releaseChunkTickets();
        this.resetInProgress = false;
        if (!this.deleted) {
            this.currentResetDelay = this.totalResetDelay;
        }
        final ResetMetrics metrics = ResetMetrics.find(this.name);
        if (metrics != null) {
            metrics.abandon();
        }
    }

    private java.util.concurrent.CompletableFuture<Void> preloadChunks() {
        final MineBounds bounds = this.getBounds();
        final long chunkCount = (long) (bounds.maxChunkX() - bounds.minChunkX() + 1)
                * (bounds.maxChunkZ() - bounds.minChunkZ() + 1);
        final int limit = PluginSettings.get().reset.maxPreloadChunks;
        if (chunkCount > limit) {
            GalaxyMines.getInstance().debug("Skipping chunk preload for mine " + this.name
                    + ": " + chunkCount + " chunks exceed the limit of " + limit, true);
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        final java.util.List<java.util.concurrent.CompletableFuture<?>> futures =
                new java.util.ArrayList<>();
        for (int chunkX = bounds.minChunkX(); chunkX <= bounds.maxChunkX(); ++chunkX) {
            for (int chunkZ = bounds.minChunkZ(); chunkZ <= bounds.maxChunkZ(); ++chunkZ) {
                futures.add(this.world.getChunkAtAsync(chunkX, chunkZ));
            }
        }
        return java.util.concurrent.CompletableFuture.allOf(
                futures.toArray(java.util.concurrent.CompletableFuture[]::new));
    }

    private void finishReset() {
        this.resetSession = null;
        this.releaseChunkTickets();
        this.resetInProgress = false;
        if (this.deleted) {
            return;
        }
        this.resetThreshold.rollTarget(java.util.concurrent.ThreadLocalRandom.current());
        this.resetOnPercent = this.resetThreshold.target();
        if (this.broadcastOnReset) {
            for (final Player p : Bukkit.getOnlinePlayers()) {
                if (this.broadcastNearby) {
                    if (p.getLocation().getWorld() != this.getCenter().getWorld()
                            || p.getLocation().distance(this.getCenter()) > this.broadcastRadius) {
                        continue;
                    }
                }
                p.sendMessage(GalaxyMineMessageManager.getInstance()
                        .translateComponent(this.broadcastMessage, this));
            }
        }
        try {
            StorageManager.addReset(this);
        } catch (IOException | JsonParseException ex3) {
            GalaxyMines.instance.printException(
                    "There was an error trying to update the total broken blocks in " + this.name
                            + "'s storage file!", ex3);
        }
        final int brokenThisReset = this.brokenBlockPositions.size();
        if (this.progression.enabled() && brokenThisReset > 0) {
            final int previousLevel = this.progression.level();
            this.progression = this.progression.withMined(
                    this.progression.minedBlocks() + brokenThisReset);
            final int levelsGained = Math.max(0, this.progression.level() - previousLevel);
            if (levelsGained > 0 && !this.deleted) {
                final String message = me.marcdoesntexist.galaxymines.messages.Messages.get(
                        "messages.mine.tierUp",
                        "The mine %mine% reached tier %level%!",
                        "%mine%", this.name, "%level%", Integer.toString(this.progression.level()));
                for (final Player p : this.getPlayersInside()) {
                    p.sendMessage(me.marcdoesntexist.galaxymines.api.chat.Text.parse(message));
                }
                GalaxyMines.getInstance().debug("Mine " + this.name + " reached tier "
                        + this.progression.level() + " (was " + previousLevel + ")", true);
            }
        }
        this.brokenBlockPositions.clear();
        this.stateInitialized = true;
        this.lowFeedbackSent = false;
        this.lastResetAtMillis = System.currentTimeMillis();
        this.save();
        final PluginSettings.Feedback feedback = PluginSettings.get().feedback;
        MineFeedback.playAll(this.resetAudience(), feedback.resetSound, feedback.resetParticle);
        new MineResetCompleteEvent(this).call();
        if (this instanceof HologramCompatible) {
            ((HologramCompatible) this).updateHolograms();
        }
        ResetCommandRunner.run("post", this, this.postResetCommands);
    }

    @Override
    public void clearMaterials() {
        this.composition.clear();
        this.invalidateResetPalette();
    }

    @Override
    public void addMaterial(final Material material) {
        if (material != null && !this.containsMaterial(material)) {
            this.setMaterialPercentage(material, 1.0);
        }
    }

    @Override
    public PotionEffect getEffect(final PotionEffectType type) {
        for (final PotionEffect effect : this.getEffects()) {
            if (effect.getType().equals(type)) {
                return effect;
            }
        }
        return null;
    }
}
