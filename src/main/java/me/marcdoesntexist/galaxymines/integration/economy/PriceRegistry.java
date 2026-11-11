package me.marcdoesntexist.galaxymines.integration.economy;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockRegistry;
import me.marcdoesntexist.galaxymines.integration.customblocks.CustomBlockTokens;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;


public final class PriceRegistry {
    private static volatile Table table = Table.empty();
    private static volatile File source;
    private static volatile List<String> unresolved = List.of();

    private PriceRegistry() {
    }

    public static void load(final File file) {
        source = file;
        if (file == null || !file.isFile()) {
            install(Collections.emptyMap());
            return;
        }
        install(parse(YamlConfiguration.loadConfiguration(file)));
    }


    public static void reloadAsync(final File file, final Runnable onComplete) {
        source = file;
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(GalaxyMines.getInstance(), () -> {
            Map<String, Double> loaded = Collections.emptyMap();
            try {
                if (file != null && file.isFile()) {
                    loaded = parse(YamlConfiguration.loadConfiguration(file));
                }
            } catch (Throwable t) {
                GalaxyMines.getInstance().printException("Could not read " + file, t);
            }
            final Map<String, Double> result = loaded;
            org.bukkit.Bukkit.getScheduler().runTask(GalaxyMines.getInstance(), () -> {
                install(result);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        });
    }

    static Map<String, Double> parse(final YamlConfiguration config) {
        final ConfigurationSection section = config.getConfigurationSection("prices");
        if (section == null) {
            return Collections.emptyMap();
        }
        final Map<String, Double> result = new LinkedHashMap<>();
        for (final String key : section.getKeys(false)) {
            final double value = section.getDouble(key, -1.0D);
            if (value < 0.0D || Double.isNaN(value)) {
                continue;
            }
            final String token = CustomBlockTokens.normalize(key);
            final String normalized = token != null
                    ? token
                    : key.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                result.put(normalized, value);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static void install(final Map<String, Double> loaded) {
        final Map<String, Double> immutable = Collections.unmodifiableMap(
                new LinkedHashMap<>(loaded));
        final Map<String, Double> customPrices = new LinkedHashMap<>();
        final Map<Material, List<CustomPrice>> customItems = new LinkedHashMap<>();
        final List<String> unresolvedKeys = new ArrayList<>();
        for (final Map.Entry<String, Double> entry : immutable.entrySet()) {
            if (!CustomBlockTokens.isToken(entry.getKey())) {
                continue;
            }
            customPrices.put(entry.getKey(), entry.getValue());
            final ItemStack item = CustomBlockRegistry.item(entry.getKey());
            if (item == null || item.getType().isAir()) {
                unresolvedKeys.add(entry.getKey());
                final GalaxyMines plugin = GalaxyMines.getInstance();
                if (plugin != null) {
                    plugin.getLogger().warning("Custom price '" + entry.getKey()
                            + "' could not be resolved. Install/enable its provider and check "
                            + "the item id; the key is ignored until it can be resolved.");
                }
                continue;
            }
            final ItemStack template = item.clone();
            template.setAmount(1);
            customItems.computeIfAbsent(template.getType(), ignored -> new ArrayList<>())
                    .add(new CustomPrice(entry.getValue(), template));
        }
        final Map<Material, List<CustomPrice>> byMaterial = new LinkedHashMap<>();
        customItems.forEach((material, prices) ->
                byMaterial.put(material, List.copyOf(prices)));
        table = new Table(immutable, Collections.unmodifiableMap(customPrices),
                Collections.unmodifiableMap(byMaterial));
        unresolved = List.copyOf(unresolvedKeys);
    }


    public static List<String> unresolvedCustomKeys() {
        return unresolved;
    }


    public static int customKeyCount() {
        return table.customPrices().size();
    }

    public static double price(final ItemStack stack) {
        return price(stack, SellPriority.PROVIDER_FIRST);
    }


    public static double price(final ItemStack stack, final SellPriority priority) {
        if (stack == null || stack.getType().isAir()) {
            return 0.0D;
        }
        final Table current = table;
        boolean providerResolved = false;
        double providerPrice = 0.0D;
        if (!current.customPrices().isEmpty() || !current.customItems().isEmpty()) {
            final String customId = CustomBlockRegistry.itemId(stack);
            if (customId != null) {
                final Double exact = current.customPrices().get(customId);
                if (exact != null) {
                    providerResolved = true;
                    providerPrice = exact;
                }
            }
            if (!providerResolved) {
                for (final CustomPrice custom : current.customItemsFor(stack.getType())) {
                    if (custom.matches(stack)) {
                        providerResolved = true;
                        providerPrice = custom.price();
                        break;
                    }
                }
            }
        }
        final double materialPrice = current.prices().getOrDefault(stack.getType().name(), 0.0D);
        return (priority == null ? SellPriority.PROVIDER_FIRST : priority)
                .select(providerPrice, providerResolved, materialPrice);
    }

    public static double price(final Material material) {
        if (material == null) {
            return 0.0D;
        }
        return table.prices().getOrDefault(material.name(), 0.0D);
    }

    public static boolean isSellable(final ItemStack stack) {
        return price(stack) > 0.0D;
    }


    public static boolean isCustomItem(final ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        final Table current = table;
        if (current.customPrices().isEmpty() && current.customItems().isEmpty()) {
            return false;
        }
        final String customId = CustomBlockRegistry.itemId(stack);
        if (customId != null && current.customPrices().containsKey(customId)) {
            return true;
        }
        for (final CustomPrice custom : current.customItemsFor(stack.getType())) {
            if (custom.matches(stack)) {
                return true;
            }
        }
        return false;
    }


    public static Map<String, Double> snapshot() {
        return table.prices();
    }

    public static File source() {
        return source;
    }

    private record Table(Map<String, Double> prices, Map<String, Double> customPrices,
                         Map<Material, List<CustomPrice>> customItems) {
        private static Table empty() {
            return new Table(Map.of(), Map.of(), Map.of());
        }

        private List<CustomPrice> customItemsFor(final Material material) {
            if (material == null) {
                return List.of();
            }
            return this.customItems.getOrDefault(material, List.of());
        }
    }

    private record CustomPrice(double price, ItemStack item) {
        private boolean matches(final ItemStack stack) {
            return stack != null && !stack.getType().isAir() && this.item.isSimilar(stack);
        }
    }
}
