package me.marcdoesntexist.galaxymines.mines;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

public interface Mine extends Iterable<Block> {
    void delete();

    void reset();

    void save();




    org.bukkit.configuration.file.FileConfiguration exportConfiguration();

    String getName();

    List<Material> getMaterials();

    boolean isDeleted();

    double getTotalPercentage();

    void fill(final ItemStack p0);

    void clear();

    boolean isExceedingMaterials();

    double getExceedingTotal();

    void setResetDelay(final int p0);

    boolean isBroadcastOnReset();

    void setBroadcastOnReset(final boolean p0);

    void setBroadcastMessage(final String p0);

    MineType getType();

    boolean contains(final double p0, final double p1, final double p2);

    boolean contains(final Block p0);

    boolean contains(final Location p0);

    boolean contains(final Player p0);

    boolean broadcastOnReset();

    boolean broadcastToNearbyOnly();

    double broadcastRadius();

    List<String> getInfo();

    Map<Material, Double> getComposition();

    void setComposition(final Map<Material, Double> p0);

    double getPercentage(final Material p0);

    void secondCountdown();

    void setBlockAsBroken(final Block p0);

    boolean restoreState();

    boolean isCompletelyBroken();

    Location getCenter();

    String getAlias();

    void setAlias(final String p0);

    boolean containsMaterial(final Material p0);



    default boolean containsBlock(final Block block) {
        return block != null && this.containsMaterial(block.getType());
    }





    default String customBlockToken(final Block block) {
        return null;
    }

    void setMaterialPercentage(final Material p0, final double p1);

    void addMaterial(final Material p0);

    void removeMaterial(final Material p0);

    World getWorld();

    MineBounds getBounds();

    int getLowerY();

    int getUpperY();

    int getTimeToNextReset();

    boolean isEnabled();

    void setEnabled(final boolean p0);

    ItemStack getIcon();

    int getVolume();

    int getRemainingBlocks();

    float getPercentageRemaining();




    default boolean isInfinite() {
        return false;
    }



    default double getInfiniteRespawnSeconds() {
        return 0.0D;
    }



    default int getInfiniteMaxPending() {
        return 0;
    }

    int getMinedBlocks();

    float getPercentageMined();

    boolean wasAlreadyBroken(final Block p0);

    Location getTopCenterLocation();

    int averageBreakSpeed();

    int getTotalMaterials();


    void setBroadcastRange(final double p0);

    void setNearbyBroadcast(final boolean p0);

    boolean requiresAccessPermission();

    String getRequiredPermission();

    String getRequiredRank();

    boolean requiresBreakPermission();

    boolean isCompositionOnly();




    default MineConstraints getBlockConstraints() {
        return MineConstraints.NONE;
    }

    void setBlockConstraints(MineConstraints constraints);




    default int getWaveLayersPerTick() {
        return 0;
    }

    void setWaveLayersPerTick(int layers);

    default MineWaveDirection getWaveDirection() {
        return MineWaveDirection.TOP_DOWN;
    }

    void setWaveDirection(MineWaveDirection direction);




    default MineProgression getProgression() {
        return MineProgression.DISABLED;
    }

    void setProgression(MineProgression progression);



    default String getSellCurrency() {
        return "money";
    }

    void setSellCurrency(String currencyId);



    default me.marcdoesntexist.galaxymines.integration.economy.SellPriority getSellPriority() {
        return me.marcdoesntexist.galaxymines.integration.economy.SellPriority.GLOBAL;
    }

    void setSellPriority(me.marcdoesntexist.galaxymines.integration.economy.SellPriority priority);



    default double getProgressionDropBonus() {
        return this.getProgression().dropBonus();
    }



    default double getProgressionSellBonus() {
        return this.getProgression().sellBonus();
    }



    default boolean supportsExpansion() {
        return false;
    }




    default boolean expandHorizontally(final int blocks) {
        return false;
    }

    Location getSpawnLocation();

    void setSpawnLocation(final Location location);

    Location getSafeTeleportLocation();

    double getResetOnPercent();

    void setResetOnPercent(final double percentage);




    default double getResetPercentMin() {
        return getResetOnPercent();
    }



    default double getResetPercentMax() {
        return getResetOnPercent();
    }




    default int getResetPercentCheckIntervalTicks() {
        return 0;
    }

    void setResetPercentCheckIntervalTicks(int ticks);



    default double getResetThresholdTarget() {
        return getResetOnPercent();
    }



    default List<String> getPreResetCommands() {
        return List.of();
    }

    void setPreResetCommands(List<String> commands);



    default List<String> getPostResetCommands() {
        return List.of();
    }

    void setPostResetCommands(List<String> commands);




    default void setResetPercentDefault(final double percentage) {
        setResetOnPercent(percentage);
    }

    void setResetPercentRange(double min, double max);

    List<String> getScheduledResetTimes();

    void setScheduledResetTimes(final List<String> times);

    boolean isPaused();

    void setPaused(final boolean paused);




    default boolean isResetInProgress() {
        return false;
    }

    boolean isBossBarEnabled();

    boolean isWorldGuardEnabled();

    void setWorldGuardEnabled(boolean enabled);

    MineDrops getDrops();

    void setDrops(MineDrops drops);

    MineResetStyle getResetStyle();

    void setResetStyle(MineResetStyle style);




    void renameTo(String newName);

    Map<String, String> getWorldGuardFlags();

    void setWorldGuardFlag(String flag, String value);

    List<Player> getPlayersInside();

    void addPotionEffect(final PotionEffect p0);

    void removePotionEffect(final PotionEffectType p0);

    void clearEffects();

    boolean useEffects();

    List<PotionEffect> getEffects();

    void clearMaterials();

    void disable();

    double getFreePercentage();

    PotionEffect getEffect(final PotionEffectType p0);
}
