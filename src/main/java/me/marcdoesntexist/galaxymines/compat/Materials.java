package me.marcdoesntexist.galaxymines.compat;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Materials {

    private static final String[] COLOURS = {
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    };

    private static final Map<String, String> COLOURED = new HashMap<>();

    private static final Map<String, String[]> VARIANTS = new HashMap<>();

    private static final Map<String, String> RENAMES = new HashMap<>();

    static {
        COLOURED.put("WOOL", "_WOOL");
        COLOURED.put("CARPET", "_CARPET");
        COLOURED.put("STAINED_GLASS", "_STAINED_GLASS");
        COLOURED.put("STAINED_GLASS_PANE", "_STAINED_GLASS_PANE");
        COLOURED.put("STAINED_CLAY", "_TERRACOTTA");
        COLOURED.put("GLAZED_TERRACOTTA", "_GLAZED_TERRACOTTA");
        COLOURED.put("CONCRETE", "_CONCRETE");
        COLOURED.put("CONCRETE_POWDER", "_CONCRETE_POWDER");
        COLOURED.put("BANNER", "_BANNER");
        COLOURED.put("SHULKER_BOX", "_SHULKER_BOX");
        COLOURED.put("BED_BLOCK", "_BED");

        VARIANTS.put("WOOD", new String[]{
                "OAK_PLANKS", "SPRUCE_PLANKS", "BIRCH_PLANKS", "JUNGLE_PLANKS", "ACACIA_PLANKS",
                "DARK_OAK_PLANKS"
        });
        VARIANTS.put("LOG", new String[]{"OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG"});
        VARIANTS.put("LOG_2", new String[]{"ACACIA_LOG", "DARK_OAK_LOG"});
        VARIANTS.put("LEAVES",
                new String[]{"OAK_LEAVES", "SPRUCE_LEAVES", "BIRCH_LEAVES", "JUNGLE_LEAVES"});
        VARIANTS.put("LEAVES_2", new String[]{"ACACIA_LEAVES", "DARK_OAK_LEAVES"});
        VARIANTS.put("SAPLING", new String[]{
                "OAK_SAPLING", "SPRUCE_SAPLING", "BIRCH_SAPLING", "JUNGLE_SAPLING", "ACACIA_SAPLING",
                "DARK_OAK_SAPLING"
        });
        VARIANTS.put("WOOD_STEP", new String[]{
                "OAK_SLAB", "SPRUCE_SLAB", "BIRCH_SLAB", "JUNGLE_SLAB", "ACACIA_SLAB", "DARK_OAK_SLAB"
        });
        VARIANTS.put("WOOD_DOUBLE_STEP", VARIANTS.get("WOOD_STEP"));
        VARIANTS.put("STEP", new String[]{
                "SMOOTH_STONE_SLAB", "SANDSTONE_SLAB", "PETRIFIED_OAK_SLAB", "COBBLESTONE_SLAB",
                "BRICK_SLAB", "STONE_BRICK_SLAB", "NETHER_BRICK_SLAB", "QUARTZ_SLAB"
        });
        VARIANTS.put("DOUBLE_STEP", VARIANTS.get("STEP"));
        VARIANTS.put("STONE_SLAB2", new String[]{"RED_SANDSTONE_SLAB"});
        VARIANTS.put("DOUBLE_STONE_SLAB2", VARIANTS.get("STONE_SLAB2"));
        VARIANTS.put("STONE", new String[]{
                "STONE", "GRANITE", "POLISHED_GRANITE", "DIORITE", "POLISHED_DIORITE", "ANDESITE",
                "POLISHED_ANDESITE"
        });
        VARIANTS.put("DIRT", new String[]{"DIRT", "COARSE_DIRT", "PODZOL"});
        VARIANTS.put("SAND", new String[]{"SAND", "RED_SAND"});
        VARIANTS.put("SANDSTONE",
                new String[]{"SANDSTONE", "CHISELED_SANDSTONE", "CUT_SANDSTONE"});
        VARIANTS.put("RED_SANDSTONE",
                new String[]{"RED_SANDSTONE", "CHISELED_RED_SANDSTONE", "CUT_RED_SANDSTONE"});
        VARIANTS.put("QUARTZ_BLOCK",
                new String[]{"QUARTZ_BLOCK", "CHISELED_QUARTZ_BLOCK", "QUARTZ_PILLAR"});
        VARIANTS.put("PRISMARINE",
                new String[]{"PRISMARINE", "PRISMARINE_BRICKS", "DARK_PRISMARINE"});
        VARIANTS.put("SMOOTH_BRICK", new String[]{
                "STONE_BRICKS", "MOSSY_STONE_BRICKS", "CRACKED_STONE_BRICKS", "CHISELED_STONE_BRICKS"
        });
        VARIANTS.put("MONSTER_EGGS", new String[]{
                "INFESTED_STONE", "INFESTED_COBBLESTONE", "INFESTED_STONE_BRICKS",
                "INFESTED_MOSSY_STONE_BRICKS", "INFESTED_CRACKED_STONE_BRICKS",
                "INFESTED_CHISELED_STONE_BRICKS"
        });
        VARIANTS.put("COBBLESTONE_WALL", new String[]{"COBBLESTONE_WALL", "MOSSY_COBBLESTONE_WALL"});
        VARIANTS.put("SPONGE", new String[]{"SPONGE", "WET_SPONGE"});
        VARIANTS.put("GOLDEN_APPLE", new String[]{"GOLDEN_APPLE", "ENCHANTED_GOLDEN_APPLE"});
        VARIANTS.put("COAL", new String[]{"COAL", "CHARCOAL"});

        RENAMES.put("IRON_FENCE", "IRON_BARS");
        RENAMES.put("THIN_GLASS", "GLASS_PANE");
        RENAMES.put("FENCE", "OAK_FENCE");
        RENAMES.put("FENCE_GATE", "OAK_FENCE_GATE");
        RENAMES.put("TRAP_DOOR", "OAK_TRAPDOOR");
        RENAMES.put("WOODEN_DOOR", "OAK_DOOR");
        RENAMES.put("WOOD_DOOR", "OAK_DOOR");
        RENAMES.put("SPRUCE_DOOR_ITEM", "SPRUCE_DOOR");
        RENAMES.put("BIRCH_DOOR_ITEM", "BIRCH_DOOR");
        RENAMES.put("JUNGLE_DOOR_ITEM", "JUNGLE_DOOR");
        RENAMES.put("ACACIA_DOOR_ITEM", "ACACIA_DOOR");
        RENAMES.put("DARK_OAK_DOOR_ITEM", "DARK_OAK_DOOR");
        RENAMES.put("IRON_DOOR_BLOCK", "IRON_DOOR");
        RENAMES.put("SIGN", "OAK_SIGN");
        RENAMES.put("SIGN_POST", "OAK_SIGN");
        RENAMES.put("WALL_SIGN", "OAK_WALL_SIGN");
        RENAMES.put("WOOD_BUTTON", "OAK_BUTTON");
        RENAMES.put("WOOD_PLATE", "OAK_PRESSURE_PLATE");
        RENAMES.put("STONE_PLATE", "STONE_PRESSURE_PLATE");
        RENAMES.put("IRON_PLATE", "HEAVY_WEIGHTED_PRESSURE_PLATE");
        RENAMES.put("GOLD_PLATE", "LIGHT_WEIGHTED_PRESSURE_PLATE");
        RENAMES.put("REDSTONE_COMPARATOR", "COMPARATOR");
        RENAMES.put("REDSTONE_COMPARATOR_ON", "COMPARATOR");
        RENAMES.put("REDSTONE_COMPARATOR_OFF", "COMPARATOR");
        RENAMES.put("REDSTONE_TORCH_ON", "REDSTONE_TORCH");
        RENAMES.put("REDSTONE_TORCH_OFF", "REDSTONE_TORCH");
        RENAMES.put("DIODE", "REPEATER");
        RENAMES.put("DIODE_BLOCK_ON", "REPEATER");
        RENAMES.put("DIODE_BLOCK_OFF", "REPEATER");
        RENAMES.put("PISTON_BASE", "PISTON");
        RENAMES.put("PISTON_STICKY_BASE", "STICKY_PISTON");
        RENAMES.put("PISTON_EXTENSION", "PISTON_HEAD");
        RENAMES.put("BURNING_FURNACE", "FURNACE");
        RENAMES.put("GLOWING_REDSTONE_ORE", "REDSTONE_ORE");
        RENAMES.put("ENDER_STONE", "END_STONE");
        RENAMES.put("ENDER_PORTAL", "END_PORTAL");
        RENAMES.put("ENDER_PORTAL_FRAME", "END_PORTAL_FRAME");
        RENAMES.put("NETHER_FENCE", "NETHER_BRICK_FENCE");
        RENAMES.put("NETHER_BRICK", "NETHER_BRICKS");
        RENAMES.put("NETHER_BRICK_ITEM", "NETHER_BRICK");
        RENAMES.put("NETHER_WARTS", "NETHER_WART");
        RENAMES.put("NETHER_STALK", "NETHER_WART");
        RENAMES.put("MOB_SPAWNER", "SPAWNER");
        RENAMES.put("SUGAR_CANE_BLOCK", "SUGAR_CANE");
        RENAMES.put("WATER_LILY", "LILY_PAD");
        RENAMES.put("HUGE_MUSHROOM_1", "BROWN_MUSHROOM_BLOCK");
        RENAMES.put("HUGE_MUSHROOM_2", "RED_MUSHROOM_BLOCK");
        RENAMES.put("GRASS", "GRASS_BLOCK");
        RENAMES.put("GRASS_PATH", "DIRT_PATH");
        RENAMES.put("LONG_GRASS", "SHORT_GRASS");
        RENAMES.put("DOUBLE_PLANT", "TALL_GRASS");
        RENAMES.put("YELLOW_FLOWER", "DANDELION");
        RENAMES.put("RED_ROSE", "POPPY");
        RENAMES.put("PURPUR_DOUBLE_SLAB", "PURPUR_SLAB");
        RENAMES.put("WOOD_STAIRS", "OAK_STAIRS");
        RENAMES.put("SPRUCE_WOOD_STAIRS", "SPRUCE_STAIRS");
        RENAMES.put("BIRCH_WOOD_STAIRS", "BIRCH_STAIRS");
        RENAMES.put("JUNGLE_WOOD_STAIRS", "JUNGLE_STAIRS");
        RENAMES.put("COBBLE_WALL", "COBBLESTONE_WALL");
        RENAMES.put("BRICK", "BRICKS");
        RENAMES.put("HARD_CLAY", "TERRACOTTA");
        RENAMES.put("STAINED_HARDENED_CLAY", "TERRACOTTA");
        RENAMES.put("HARDENED_CLAY", "TERRACOTTA");
        RENAMES.put("SILVER_GLAZED_TERRACOTTA", "LIGHT_GRAY_GLAZED_TERRACOTTA");
        RENAMES.put("MAGMA", "MAGMA_BLOCK");
        RENAMES.put("BOAT", "OAK_BOAT");
        RENAMES.put("WOOD_PICKAXE", "WOODEN_PICKAXE");
        RENAMES.put("WOOD_AXE", "WOODEN_AXE");
        RENAMES.put("WOOD_SPADE", "WOODEN_SHOVEL");
        RENAMES.put("WOOD_HOE", "WOODEN_HOE");
        RENAMES.put("WOOD_SWORD", "WOODEN_SWORD");
        RENAMES.put("STONE_SPADE", "STONE_SHOVEL");
        RENAMES.put("IRON_SPADE", "IRON_SHOVEL");
        RENAMES.put("GOLD_SPADE", "GOLDEN_SHOVEL");
        RENAMES.put("DIAMOND_SPADE", "DIAMOND_SHOVEL");
        RENAMES.put("GOLD_PICKAXE", "GOLDEN_PICKAXE");
        RENAMES.put("GOLD_AXE", "GOLDEN_AXE");
        RENAMES.put("GOLD_HOE", "GOLDEN_HOE");
        RENAMES.put("GOLD_SWORD", "GOLDEN_SWORD");
        RENAMES.put("SULPHUR", "GUNPOWDER");
        RENAMES.put("FIREBALL", "FIRE_CHARGE");
        RENAMES.put("EXP_BOTTLE", "EXPERIENCE_BOTTLE");
        RENAMES.put("WATCH", "CLOCK");
        RENAMES.put("BOOK_AND_QUILL", "WRITABLE_BOOK");
        R_MAP:
        RENAMES.put("EMPTY_MAP", "MAP");
        RENAMES.put("STORAGE_MINECART", "CHEST_MINECART");
        RENAMES.put("POWERED_MINECART", "FURNACE_MINECART");
        RENAMES.put("EXPLOSIVE_MINECART", "TNT_MINECART");
        RENAMES.put("COMMAND_MINECART", "COMMAND_BLOCK_MINECART");
        RENAMES.put("PORK", "PORKCHOP");
        RENAMES.put("GRILLED_PORK", "COOKED_PORKCHOP");
        RENAMES.put("RAW_FISH", "COD");
        RENAMES.put("COOKED_FISH", "COOKED_COD");
        RENAMES.put("RAW_BEEF", "BEEF");
        RENAMES.put("RAW_CHICKEN", "CHICKEN");
        RENAMES.put("SEEDS", "WHEAT_SEEDS");
        RENAMES.put("INK_SACK", "INK_SAC");
        RENAMES.put("BED", "RED_BED");
        RENAMES.put("CAULDRON_ITEM", "CAULDRON");
        RENAMES.put("BREWING_STAND_ITEM", "BREWING_STAND");
        RENAMES.put("FLOWER_POT_ITEM", "FLOWER_POT");
        RENAMES.put("SKULL_ITEM", "PLAYER_HEAD");
        RENAMES.put("FIREWORK", "FIREWORK_ROCKET");
        RENAMES.put("FIREWORK_CHARGE", "FIREWORK_STAR");
        RENAMES.put("MONSTER_EGG", "ZOMBIE_SPAWN_EGG");
    }

    private Materials() {
    }

    public static Material resolve(final String rawName, final int data) {
        if (rawName == null) {
            return null;
        }
        final String name = rawName.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (name.isEmpty()) {
            return null;
        }

        final String colourSuffix = COLOURED.get(name);
        if (colourSuffix != null && data >= 0 && data < COLOURS.length) {
            final Material coloured = match(COLOURS[data] + colourSuffix);
            if (coloured != null) {
                return coloured;
            }
        }

        final String[] variants = VARIANTS.get(name);
        if (variants != null) {
            final int index = variantIndex(name, data);
            if (index >= 0 && index < variants.length) {
                final Material variant = match(variants[index]);
                if (variant != null) {
                    return variant;
                }
            }
        }

        final Material direct = match(name);
        if (direct != null) {
            return direct;
        }

        final String renamed = RENAMES.get(name);
        if (renamed != null) {
            final Material material = match(renamed);
            if (material != null) {
                return material;
            }
        }

        if ("SHORT_GRASS".equals(name)) {
            final Material grass = match("GRASS");
            if (grass != null) {
                return grass;
            }
        }
        if ("DIRT_PATH".equals(name)) {
            return match("GRASS_PATH");
        }
        return null;
    }

    public static Material resolve(final String rawName) {
        return resolve(rawName, 0);
    }

    public static ItemStack item(final String rawName, final int data, final int amount) {
        final Material material = resolve(rawName, data);
        return material == null ? null : new ItemStack(material, Math.max(1, amount));
    }

    private static int variantIndex(final String legacyName, final int data) {
        return switch (legacyName) {
            case "LOG", "LEAVES" -> data < 0 ? 0 : data & 3;
            case "LOG_2", "LEAVES_2" -> data < 0 ? 0 : data & 1;
            case "STEP", "DOUBLE_STEP", "WOOD_STEP", "WOOD_DOUBLE_STEP", "STONE_SLAB2", "DOUBLE_STONE_SLAB2", "STONE",
                 "WOOD", "SAPLING" -> data < 0 ? 0 : data & 7;
            default -> Math.max(0, data);
        };
    }

    private static Material match(final String name) {
        try {
            final Material material = Registry.MATERIAL.get(
                    NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
            if (material != null) {
                return material;
            }
        } catch (final Throwable ignored) {
        }
        try {
            return Material.matchMaterial(name.toLowerCase(Locale.ROOT));
        } catch (final Throwable ignored) {
            return null;
        }
    }
}