package me.marcdoesntexist.galaxymines.luckyblocks;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockRegistry;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockTokens;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.mines.MineDropProcessor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;







public final class LuckyBlockManager {
    private static final LuckyBlockManager INSTANCE = new LuckyBlockManager();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private Map<String, LuckyBlockTable> tables = Map.of();
    private Map<String, MineLuckyBlocks> mines = Map.of();
    private LuckyBlockFeedback feedback = LuckyBlockFeedback.NONE;
    private boolean enabled;

    private LuckyBlockManager() {
    }

    public static LuckyBlockManager get() {
        return INSTANCE;
    }




    public static LuckyBlockParser.Result validate(final File file) {
        if (file == null || !file.isFile()) {
            return LuckyBlockParser.Result.EMPTY;
        }
        return LuckyBlockParser.parse(YamlConfiguration.loadConfiguration(file));
    }

    private static String prepareMessage(final String template, final Player player,
                                         final Mine mine, final Block block,
                                         final LuckyBlockTable table,
                                         final LuckyBlockTable.Reward reward, final int amount) {
        return template
                .replace("%player%", Text.escape(player.getName()))
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%mine%", Text.escape(mine.getName()))
                .replace("%alias%", Text.escape(mine.getAlias()))
                .replace("%block%", Text.escape(blockKey(mine, block)))
                .replace("%table%", Text.escape(table.name()))
                .replace("%amount%", String.valueOf(amount))
                .replace("%reward%", Text.escape(rewardLabel(reward, amount)));
    }

    private static String prepareCommand(final String template, final Player player,
                                         final Mine mine, final Block block,
                                         final LuckyBlockTable table, final int amount) {
        return template
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%mine%", mine.getName())
                .replace("%alias%", mine.getAlias())
                .replace("%block%", String.valueOf(blockKey(mine, block)))
                .replace("%table%", table.name())
                .replace("%amount%", String.valueOf(amount));
    }




    static ItemStack providerItem(final String token, final int amount) {
        final ItemStack resolved = CustomBlockRegistry.item(token);
        if (resolved == null || resolved.getType().isAir()) {
            debug("Lucky-block provider item '" + token
                    + "' could not be resolved; skipping it.");
            return null;
        }
        final ItemStack copy = resolved.clone();
        copy.setAmount(amount);
        return copy;
    }

    private static String rewardLabel(final LuckyBlockTable.Reward reward, final int amount) {
        return switch (reward.type()) {
            case ITEM -> (reward.providerId() != null ? reward.providerId()
                    : reward.material().name().toLowerCase(Locale.ROOT)) + " x" + amount;
            case COMMAND -> "command";
            case MESSAGE -> "message";
            case NOTHING -> "nothing";
        };
    }

    private static boolean rolls(final double chance) {
        if (chance >= 100.0D) {
            return true;
        }
        return chance > 0.0D && ThreadLocalRandom.current().nextDouble(100.0D) < chance;
    }

    private static String blockKey(final Mine mine, final Block block) {
        if (mine == null || block == null) {
            return null;
        }
        final String token = mine.customBlockToken(block);
        if (token != null) {
            return CustomBlockTokens.normalize(token);
        }
        if (!mine.containsBlock(block)) {
            return null;
        }
        return block.getType().name();
    }

    private static void debug(final String message) {
        if (GalaxyMines.getInstance() != null) {
            GalaxyMines.getInstance().debug(message, true);
        }
    }



    public Map<String, LuckyBlockTable> tables() {
        return Map.copyOf(this.tables);
    }

    public LuckyBlockParser.Result load(final File file, final boolean enabled) {
        this.unload();
        if (!enabled || file == null || !file.isFile()) {
            return LuckyBlockParser.Result.EMPTY;
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        final LuckyBlockParser.Result result = LuckyBlockParser.parse(config);
        for (final String warning : result.warnings()) {
            debug(warning);
        }
        this.tables = result.tables();
        this.mines = result.mines();
        this.feedback = result.feedback();
        this.enabled = !this.tables.isEmpty() && !this.mines.isEmpty();
        if (this.enabled) {
            debug("Loaded " + this.tables.size() + " lucky-block table(s) for "
                    + this.mines.size() + " mine(s).");
        }
        return result;
    }

    public void unload() {
        this.tables = Map.of();
        this.mines = Map.of();
        this.feedback = LuckyBlockFeedback.NONE;
        this.cooldowns.clear();
        this.enabled = false;
    }

    public LuckyBlockFeedback feedback() {
        return this.feedback;
    }

    public boolean isEnabled() {
        return this.enabled;
    }




    public boolean isLuckyBlock(final Mine mine, final Block block) {
        return tableName(mine, block) != null;
    }




    public boolean replacesDrops(final Mine mine, final Block block) {
        final MineLuckyBlocks config = mineConfig(mine);
        return config != null && config.replaceDrops() && tableName(mine, block) != null;
    }




    public boolean trigger(final Player player, final Mine mine, final Block block) {
        if (!this.enabled || player == null || mine == null || block == null) {
            return false;
        }
        final MineLuckyBlocks mineConfig = mineConfig(mine);
        if (mineConfig == null) {
            return false;
        }
        final String key = blockKey(mine, block);
        if (key == null) {
            return false;
        }
        final String tableName = mineConfig.blocks().get(key);
        final LuckyBlockTable table = tableName == null ? null : this.tables.get(tableName);
        if (table == null) {
            return false;
        }
        final long now = System.currentTimeMillis();
        if (this.cooldowns.size() > 512) {
            this.cooldowns.values().removeIf(expiry -> expiry <= now);
        }
        final Long until = this.cooldowns.get(player.getUniqueId());
        if (until != null && until > now) {
            return false;
        }

        final List<ItemStack> items = new ArrayList<>();
        for (int roll = 0; roll < table.rolls(); ++roll) {
            for (final LuckyBlockTable.Reward reward : table.rewards()) {
                if (rolls(reward.chance())) {
                    apply(player, mine, block, table, reward, items);
                }
            }
        }
        if (!items.isEmpty()) {
            final Location location = block.getLocation().clone().add(0.5D, 0.5D, 0.5D);
            MineDropProcessor.deliverRewards(player, mine, location, items);
        }
        if (this.feedback.hasCooldown()) {
            this.cooldowns.put(player.getUniqueId(),
                    now + this.feedback.cooldownSeconds() * 1000L);
        }
        applyFeedback(player, block);
        return true;
    }




    private void applyFeedback(final Player player, final Block block) {
        if (!this.feedback.hasSound() && !this.feedback.hasParticle()) {
            return;
        }
        final Location location = block.getLocation().clone().add(0.5D, 0.5D, 0.5D);
        if (this.feedback.hasSound()) {
            try {
                player.getWorld().playSound(location, this.feedback.sound(), 1.0F, 1.0F);
            } catch (RuntimeException ex) {
                debug("Invalid lucky-block sound '" + this.feedback.sound() + "': "
                        + ex.getMessage());
            }
        }
        if (this.feedback.hasParticle()) {
            try {
                final Particle particle = Particle.valueOf(
                        this.feedback.particle().trim().toUpperCase(Locale.ROOT));
                player.getWorld().spawnParticle(particle, location, 12, 0.3D, 0.3D, 0.3D);
            } catch (IllegalArgumentException ex) {
                debug("Invalid lucky-block particle '" + this.feedback.particle() + "'.");
            }
        }
    }

    private void apply(final Player player, final Mine mine, final Block block,
                       final LuckyBlockTable table, final LuckyBlockTable.Reward reward,
                       final List<ItemStack> items) {
        final int amount = reward.maxAmount() > reward.minAmount()
                ? ThreadLocalRandom.current().nextInt(reward.minAmount(), reward.maxAmount() + 1)
                : reward.minAmount();
        switch (reward.type()) {
            case ITEM -> {
                final ItemStack item = reward.providerId() == null
                        ? new ItemStack(reward.material(), amount)
                        : providerItem(reward.providerId(), amount);
                if (item != null) {
                    items.add(item);
                }
            }
            case COMMAND -> runCommand(reward.command(), player, mine, block, table, amount);
            case MESSAGE -> sendMessage(reward.message(), player, mine, block, table, reward, amount);
            case NOTHING -> {
            }
        }
        if (reward.type() != LuckyBlockTable.Type.MESSAGE && !reward.message().isBlank()) {
            sendMessage(reward.message(), player, mine, block, table, reward, amount);
        }
    }

    private void runCommand(final String template, final Player player, final Mine mine,
                            final Block block, final LuckyBlockTable table, final int amount) {
        String command = prepareCommand(template, player, mine, block, table, amount);
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isBlank()) {
            return;
        }
        try {
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                debug("Lucky-block command returned false: " + command);
            }
        } catch (RuntimeException ex) {
            debug("Lucky-block command failed: " + command + " (" + ex.getMessage() + ")");
        }
    }

    private void sendMessage(final String template, final Player player, final Mine mine,
                             final Block block, final LuckyBlockTable table,
                             final LuckyBlockTable.Reward reward, final int amount) {
        if (template == null || template.isBlank()) {
            return;
        }
        try {
            player.sendMessage(Text.parse(
                    prepareMessage(template, player, mine, block, table, reward, amount)));
        } catch (RuntimeException ex) {
            debug("Invalid lucky-block message for table '" + table.name() + "'.");
        }
    }

    private MineLuckyBlocks mineConfig(final Mine mine) {
        if (mine == null) {
            return null;
        }
        return this.mines.get(mine.getName().toLowerCase(Locale.ROOT));
    }

    private String tableName(final Mine mine, final Block block) {
        final MineLuckyBlocks config = mineConfig(mine);
        if (config == null || block == null) {
            return null;
        }
        final String key = blockKey(mine, block);
        return key == null ? null : config.blocks().get(key);
    }
}
