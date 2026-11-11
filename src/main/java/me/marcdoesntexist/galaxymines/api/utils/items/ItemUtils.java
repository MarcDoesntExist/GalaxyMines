package me.marcdoesntexist.galaxymines.api.utils.items;

import me.marcdoesntexist.galaxymines.api.CustomPlugin;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.api.exceptions.ItemConversionException;
import me.marcdoesntexist.galaxymines.compat.Materials;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ItemUtils {
    public static final ItemStack NA;
    public static final ItemStack Gray;

    static {
        NA = createItem(Material.BARRIER, "<dark_red><bold><italic><underlined>N/A");
        Gray = createItem(Material.IRON_BARS, "<gray>");
    }

    public static String toString(final ItemStack i) {
        return i.getType().name();
    }


    public static Component itemText(final String input) {
        return Text.parse(input).decoration(TextDecoration.ITALIC, false);
    }

    public static ItemStack addToLore(final ItemStack is, final String... strings) {
        final List<String> added = new ArrayList<>();
        Collections.addAll(added, strings);
        return addToLore(is, added);
    }

    public static ItemStack createItem(final Material material, final String nome) {
        return createItem(new ItemStack(material), nome);
    }


    public static ItemStack createItem(final ConfigurationSection itemSection)
            throws IllegalArgumentException {
        try {
            CustomPlugin.instance.debug("Loading item " + itemSection.getName() + " from config.");
            final String itemName =
                    CustomPlugin.messageManager.translateAll(itemSection.getString("name", ""));
            final List<String> itemLore =
                    CustomPlugin.messageManager.translateAll(itemSection.getStringList("lore"));
            final Material material =
                    Materials.resolve(itemSection.getString("type"), itemSection.getInt("data", 0));
            if (material == null) {
                throw new IllegalArgumentException("Unknown material " + itemSection.getString("type"));
            }
            return createItem(new ItemStack(material, 1), itemName, itemLore);
        } catch (final Exception var5) {
            throw new IllegalArgumentException(
                    "Configuration Section " + itemSection.getCurrentPath() + " is baddly formated!",
                    var5);
        }
    }

    public static ItemStack createItem(final ItemStack material, final String nome) {
        final ItemStack is = new ItemStack(material);
        final ItemMeta im = is.getItemMeta();
        if (im == null) {
            return is;
        }
        if (nome != null) {
            im.displayName(itemText(nome));
        }
        im.addItemFlags(ItemFlag.values());
        is.setItemMeta(im);
        return is;
    }

    public static ItemStack createItem(final ItemStack material, final String nome,
                                       final Collection<String> desc) {
        final ItemStack is = createItem(material, nome);
        final ItemMeta im = is.getItemMeta();
        if (im == null) {
            return is;
        }
        final List<Component> lore = new ArrayList<>();
        for (final String line : desc) {
            lore.add(itemText(line));
        }
        im.lore(lore);
        is.setItemMeta(im);
        return is;
    }

    public static ItemStack createItem(final Material material, final String nome,
                                       final Collection<String> desc) {
        return createItem(new ItemStack(material), nome, desc);
    }


    public static void addGlow(final ItemStack i) {
        final ItemMeta im = i.getItemMeta();
        if (im == null) {
            return;
        }
        im.setEnchantmentGlintOverride(true);
        i.setItemMeta(im);
    }

    public static boolean checkDisplayName(final ItemStack i) {
        return checkItemMeta(i) && i.getItemMeta().hasDisplayName();
    }

    public static boolean checkItemMeta(final ItemStack i) {
        return i != null && i.getItemMeta() != null;
    }

    public static boolean checkLore(final ItemStack i) {
        return checkItemMeta(i) && i.getItemMeta().hasLore();
    }

    public static ItemStack criarBotaoSim(final String itemVendido, final String finalCusto) {
        final ItemStack itemCompra = new ItemStack(Material.EMERALD_BLOCK);
        final ItemMeta IMCompra = itemCompra.getItemMeta();
        if (IMCompra == null) {
            return itemCompra;
        }
        IMCompra.displayName(itemText("<green><bold>Buy this " + itemVendido));
        IMCompra.lore(Collections.singletonList(
                itemText("<gray>Buy this " + itemVendido + " for " + finalCusto)));
        itemCompra.setItemMeta(IMCompra);
        return itemCompra;
    }

    public static boolean isValido(final ItemStack item) {
        return isValido(item, true);
    }

    public static boolean isValido(final ItemStack item, final boolean comNome) {
        if (!checkItemMeta(item) || !comNome) {
            return false;
        }
        return item.getItemMeta().hasDisplayName();
    }

    public static List<String> getLore(final ItemStack i) {
        if (checkLore(i)) {
            return i.getItemMeta().getLore();
        }
        return new ArrayList<>();
    }

    public static ItemStack convertFromInput(final String input, final String name)
            throws ItemConversionException {
        return createItem(convertFromInput(input), name);
    }

    public static ItemStack convertFromInput(final String input) throws ItemConversionException {
        try {
            final String[] materialAndData = input.split(":", 2);
            int data = 0;
            if (materialAndData.length > 1) {
                try {
                    data = Integer.parseInt(materialAndData[1].trim());
                } catch (final NumberFormatException exception) {
                    if (CustomPlugin.instance != null) {
                        CustomPlugin.instance.debug(
                                materialAndData[1] + " in " + input + " isn't a number! Setting data to 0");
                    }
                }
            }
            final Material material = Materials.resolve(materialAndData[0], data);
            if (material == null) {
                throw new ItemConversionException(input);
            }
            return new ItemStack(material, 1);
        } catch (final ItemConversionException e) {
            throw e;
        } catch (final Exception e) {
            throw new ItemConversionException(input);
        }
    }

    public static boolean equals(final ItemStack a, final ItemStack b) {
        return checkDisplayName(a) && checkDisplayName(b)
                && a.getItemMeta().displayName().equals(b.getItemMeta().displayName());
    }


    public static boolean equalMaterial(final ItemStack a, final ItemStack b) {
        return a != null && b != null && a.getType() == b.getType();
    }

    public static ItemStack clearLore(final ItemStack i) {
        final ItemMeta itemMeta = i.getItemMeta();
        if (itemMeta == null) {
            return i;
        }
        itemMeta.lore(null);
        i.setItemMeta(itemMeta);
        return i;
    }

    public static ItemStack addToLore(final ItemStack i, final List<String> strings) {
        final ItemMeta itemMeta = i.getItemMeta();
        if (itemMeta == null) {
            return i;
        }
        final List<Component> lore = itemMeta.lore() == null
                ? new ArrayList<>()
                : new ArrayList<>(itemMeta.lore());
        for (final String line : strings) {
            lore.add(itemText(line));
        }
        itemMeta.lore(lore);
        i.setItemMeta(itemMeta);
        return i;
    }
}
