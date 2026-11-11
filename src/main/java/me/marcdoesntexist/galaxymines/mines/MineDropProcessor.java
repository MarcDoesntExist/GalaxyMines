package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.events.MineEventManager;
import me.marcdoesntexist.galaxymines.integration.drops.DropSinks;
import me.marcdoesntexist.galaxymines.integration.economy.EconomyService;
import me.marcdoesntexist.galaxymines.integration.economy.PriceRegistry;
import me.marcdoesntexist.galaxymines.integration.economy.SellService;
import me.marcdoesntexist.galaxymines.messages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;








public final class MineDropProcessor {
    private static volatile Map<Material, ItemStack> smeltResults = Map.of();
    private static volatile Map<Material, ItemStack> blockResults = Map.of();

    private MineDropProcessor() {
    }

    


    public static void buildRecipeCache() {
        final Map<Material, ItemStack> smelt = new HashMap<>();
        final Map<Material, ItemStack> blocks = new HashMap<>();
        final Iterator<Recipe> recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            final Recipe recipe = recipes.next();
            if (recipe instanceof FurnaceRecipe furnace) {
                addCookingRecipe(smelt, furnace);
            } else if (recipe instanceof ShapedRecipe shaped) {
                addBlockRecipe(blocks, shaped);
            }
        }
        smeltResults = Map.copyOf(smelt);
        blockResults = Map.copyOf(blocks);
        GalaxyMines.getInstance().debug("Cached " + smelt.size() + " smelting and "
                + blocks.size() + " block recipes for mine drops", 3);
    }

    public static int smeltRecipeCount() {
        return smeltResults.size();
    }

    public static int blockRecipeCount() {
        return blockResults.size();
    }

    private static void addCookingRecipe(final Map<Material, ItemStack> target,
                                         final CookingRecipe<?> recipe) {
        final ItemStack result = recipe.getResult();
        if (result == null || result.getType().isAir()) {
            return;
        }
        final RecipeChoice choice = recipe.getInputChoice();
        if (choice instanceof RecipeChoice.MaterialChoice materials) {
            for (final Material material : materials.getChoices()) {
                target.putIfAbsent(material, result.clone());
            }
        } else if (choice instanceof RecipeChoice.ExactChoice exact) {
            for (final ItemStack input : exact.getChoices()) {
                target.putIfAbsent(input.getType(), result.clone());
            }
        } else {
            final ItemStack input = recipe.getInput();
            if (input != null) {
                target.putIfAbsent(input.getType(), result.clone());
            }
        }
    }

    





    private static void addBlockRecipe(final Map<Material, ItemStack> target,
                                       final ShapedRecipe recipe) {
        final Map<Character, ItemStack> ingredients = recipe.getIngredientMap();
        Material single = null;
        int count = 0;
        for (final ItemStack ingredient : ingredients.values()) {
            if (ingredient == null || ingredient.getType().isAir()) {
                continue;
            }
            if (single == null) {
                single = ingredient.getType();
            } else if (single != ingredient.getType()) {
                return;
            }
            ++count;
        }
        if (single == null || count != 9) {
            return;
        }
        final ItemStack result = recipe.getResult();
        if (result == null || result.getType().isAir() || !result.getType().isBlock()) {
            return;
        }
        target.putIfAbsent(single, result.clone());
    }

    public static List<ItemStack> capture(final Block block, final Player player) {
        return new ArrayList<>(block.getDrops(
                player.getInventory().getItemInMainHand(), player));
    }

    



    public static boolean canAccept(final Player player, final Mine mine,
                                    final List<ItemStack> drops) {
        final MineDrops settings = mine.getDrops();
        if (!settings.autoPickup()
                || settings.fullInventoryPolicy() != MineDrops.FullInventoryPolicy.CANCEL_BREAK) {
            return true;
        }
        
        
        if (!DropSinks.get().isEmpty()) {
            return true;
        }
        if (settings.autoSell() && economyEnabled()) {
            return true;
        }
        return hasRoom(player, transformedPreview(drops, mine));
    }

    private static List<ItemStack> transformedPreview(final List<ItemStack> drops,
                                                      final Mine mine) {
        final List<ItemStack> preview = new ArrayList<>();
        for (final ItemStack stack : drops) {
            final boolean custom = PriceRegistry.isCustomItem(stack);
            final ItemStack smelted = mine.getDrops().autoSmelt() && !custom
                    ? smelt(stack) : stack.clone();
            if (mine.getDrops().autoBlock() && !custom) {
                preview.addAll(compact(smelted));
            } else {
                preview.add(smelted);
            }
        }
        return preview;
    }

    


    public static void deliver(final Player player, final Mine mine, final Location location,
                               final List<ItemStack> rawDrops) {
        final List<ItemStack> drops = new ArrayList<>();
        final double multiplier = Math.max(0.0D, mine.getDrops().dropMultiplier())
                * MineEventManager.get().dropMultiplier(mine)
                * (1.0D + Math.max(0.0D, mine.getProgressionDropBonus()))
                * me.marcdoesntexist.galaxymines.multiplier.BoosterService.get()
                .multiplier(player,
                        me.marcdoesntexist.galaxymines.multiplier.BoosterType.DROP)
                
                
                * me.marcdoesntexist.galaxymines.multiplier.MultiplierSources.get().drop(player);
        for (final ItemStack stack : rawDrops) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }
            final int amount = applyMultiplier(stack.getAmount(), multiplier);
            if (amount > 0) {
                drops.add(withAmount(stack, amount));
            }
        }
        drops.addAll(MineEventManager.get().rareDrops(mine));
        final List<ItemStack> processed = new ArrayList<>();
        for (final ItemStack stack : drops) {
            final boolean custom = PriceRegistry.isCustomItem(stack);
            final ItemStack smelted = mine.getDrops().autoSmelt() && !custom
                    ? smelt(stack) : stack;
            if (mine.getDrops().autoBlock() && !custom) {
                processed.addAll(compact(smelted));
            } else {
                processed.add(smelted);
            }
        }
        
        
        final List<ItemStack> finalDrops = new ArrayList<>();
        for (final ItemStack stack : processed) {
            finalDrops.addAll(split(stack));
        }
        
        
        final List<ItemStack> remaining = DropSinks.get().offer(player, mine, finalDrops);
        if (remaining.isEmpty()) {
            return;
        }
        if (mine.getDrops().autoSell() && economyEnabled()) {
            SellService.sellDrops(player, mine, remaining);
        }
        if (mine.getDrops().autoPickup()) {
            giveOrDrop(player, mine, location, remaining);
        } else {
            dropAll(player, location, remaining);
        }
    }

    



    public static void deliverRewards(final Player player, final Mine mine,
                                      final Location location,
                                      final List<ItemStack> rewards) {
        if (player == null || mine == null || location == null || rewards == null) {
            return;
        }
        final List<ItemStack> finalRewards = new ArrayList<>();
        for (final ItemStack stack : rewards) {
            finalRewards.addAll(split(stack));
        }
        if (finalRewards.isEmpty()) {
            return;
        }
        if (mine.getDrops().autoPickup()) {
            giveOrDrop(player, mine, location, finalRewards);
        } else {
            dropAll(player, location, finalRewards);
        }
    }

    private static List<ItemStack> split(final ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return List.of();
        }
        final List<ItemStack> result = new ArrayList<>();
        for (final int amount : splitAmounts(stack.getAmount(), stack.getMaxStackSize())) {
            final ItemStack part = stack.clone();
            part.setAmount(amount);
            result.add(part);
        }
        return result;
    }

    



    static List<Integer> splitAmounts(final int total, final int maxStackSize) {
        if (total <= 0) {
            return List.of();
        }
        final int stackSize = Math.max(1, maxStackSize);
        final List<Integer> amounts = new ArrayList<>((total + stackSize - 1) / stackSize);
        int remaining = total;
        while (remaining > 0) {
            final int amount = Math.min(stackSize, remaining);
            amounts.add(amount);
            remaining -= amount;
        }
        return amounts;
    }

    private static void giveOrDrop(final Player player, final Mine mine, final Location location,
                                   final List<ItemStack> drops) {
        final boolean voidLeftovers =
                mine.getDrops().fullInventoryPolicy() == MineDrops.FullInventoryPolicy.VOID;
        for (final ItemStack stack : drops) {
            final Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            if (leftover.isEmpty() || voidLeftovers) {
                continue;
            }
            for (final ItemStack remaining : leftover.values()) {
                player.getWorld().dropItemNaturally(location, remaining);
            }
        }
    }

    private static void dropAll(final Player player, final Location location,
                                final List<ItemStack> drops) {
        for (final ItemStack stack : drops) {
            player.getWorld().dropItemNaturally(location, stack);
        }
    }

    private static boolean hasRoom(final Player player, final List<ItemStack> drops) {
        final ItemStack[] contents = player.getInventory().getStorageContents();
        final ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; ++i) {
            copy[i] = contents[i] == null ? null : contents[i].clone();
        }
        for (final ItemStack drop : drops) {
            int remaining = drop.getAmount();
            final int max = Math.max(1, drop.getMaxStackSize());
            for (int slot = 0; slot < copy.length && remaining > 0; ++slot) {
                if (copy[slot] != null && copy[slot].isSimilar(drop)) {
                    final int space = max - copy[slot].getAmount();
                    if (space > 0) {
                        final int moved = Math.min(space, remaining);
                        copy[slot].setAmount(copy[slot].getAmount() + moved);
                        remaining -= moved;
                    }
                }
            }
            for (int slot = 0; slot < copy.length && remaining > 0; ++slot) {
                if (copy[slot] == null) {
                    final int moved = Math.min(max, remaining);
                    final ItemStack placed = drop.clone();
                    placed.setAmount(moved);
                    copy[slot] = placed;
                    remaining -= moved;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    static int applyMultiplier(final int amount, final double multiplier) {
        if (amount <= 0 || multiplier <= 0.0D) {
            return 0;
        }
        final double exact = amount * multiplier;
        final int whole = (int) Math.floor(exact);
        final double fraction = exact - whole;
        final int extra = fraction > 0.0D
                && ThreadLocalRandom.current().nextDouble() < fraction ? 1 : 0;
        return Math.max(0, whole + extra);
    }

    private static ItemStack smelt(final ItemStack input) {
        final ItemStack result = smeltResults.get(input.getType());
        if (result == null) {
            return input;
        }
        return withAmount(result, input.getAmount());
    }

    private static List<ItemStack> compact(final ItemStack input) {
        final ItemStack block = blockResults.get(input.getType());
        if (block == null) {
            return List.of(input);
        }
        final int blockAmount = input.getAmount() / 9;
        final int remainder = input.getAmount() % 9;
        final List<ItemStack> result = new ArrayList<>(2);
        if (blockAmount > 0) {
            result.add(withAmount(block, blockAmount));
        }
        if (remainder > 0) {
            result.add(withAmount(input, remainder));
        }
        return result;
    }

    private static ItemStack withAmount(final ItemStack stack, final int amount) {
        final ItemStack copy = stack.clone();
        copy.setAmount(Math.max(1, amount));
        return copy;
    }

    public static String fullInventoryMessage() {
        return Messages.get("messages.mine.inventoryFull",
                "Your inventory is full and this mine uses auto-pickup.");
    }

    private static boolean economyEnabled() {
        return me.marcdoesntexist.galaxymines.config.PluginSettings.get().economy.enabled
                && EconomyService.isAvailable();
    }
}
