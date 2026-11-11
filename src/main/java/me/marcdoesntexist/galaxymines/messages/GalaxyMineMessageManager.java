package me.marcdoesntexist.galaxymines.messages;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.BasicMessageManager;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.api.exceptions.ItemConversionException;
import me.marcdoesntexist.galaxymines.api.utils.StringUtils;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.GalaxyMinesConfiguration;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.storage.StorageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GalaxyMineMessageManager extends BasicMessageManager {
    public static String globalResetMessage;
    public static String mineCreateMessage;
    public static String noPermission;
    public static String invalidArguments;
    private static GalaxyMineMessageManager instance;

    public static GalaxyMineMessageManager getInstance() {
        if (GalaxyMineMessageManager.instance == null) {
            GalaxyMineMessageManager.instance = new GalaxyMineMessageManager();
        }
        return GalaxyMineMessageManager.instance;
    }

    private static Component itemText(final String input) {
        return Text.parse(input).decoration(TextDecoration.ITALIC, false);
    }

    public ItemStack createIcon(final ConfigurationSection iconsection, final Mine m) {
        return this.translateItemStack(ItemUtils.createItem(iconsection), m);
    }

    public ItemStack translateItemStack(final ItemStack itemStack, final Mine m) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }
        if (itemMeta.hasDisplayName()) {
            itemMeta.displayName(itemText(this.translateAll(itemMeta.getDisplayName(), m)));
        }
        if (itemMeta.hasLore()) {
            itemMeta.lore(this.translateLore(itemMeta.getLore(), m));
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private List<Component> translateLore(final List<String> lore, final Mine m) {
        final List<Component> lines = new ArrayList<>();
        if (lore == null) {
            return lines;
        }
        for (final String line : lore) {
            lines.add(itemText(this.translateAll(line, m)));
        }
        return lines;
    }

    @Override
    public void postSetup() {
        GalaxyMines galaxyMines = GalaxyMines.getInstance();
        try {
            galaxyMines.debug("Loading...");
            GalaxyMineMessageManager.globalResetMessage = this.translateColors(
                    GalaxyMinesConfiguration.messagesConfig.getString("messages.mineReset", ""));
            GalaxyMineMessageManager.mineCreateMessage = this.translateColors(
                    GalaxyMinesConfiguration.messagesConfig.getString("messages.mineCreate", ""));
            GalaxyMineMessageManager.messageSeparator = this.translateColors(
                    GalaxyMinesConfiguration.messagesConfig.getString("messages.messageSeparator", ""));
            GalaxyMineMessageManager.pluginPrefix = this.translateColors(
                    GalaxyMinesConfiguration.messagesConfig.getString("messages.messagePrefix", ""));
            GalaxyMineMessageManager.header = this.translateAll(
                    GalaxyMinesConfiguration.messagesConfig.getString("messages.header", ""));
            GalaxyMineMessageManager.noPermission = this.translateColors(
                    GalaxyMinesConfiguration.messagesConfig.getString("messages.noPermission", ""));
            GalaxyMineMessageManager.invalidArguments = this.translateColors(
                    GalaxyMinesConfiguration.messagesConfig.getString("messages.invalidArguments", ""));
            galaxyMines.debug("Messages loaded");
            galaxyMines.debug();
        } catch (Exception e) {
            galaxyMines.printException("An error occurred while loading the config", e);
        }
    }


    public Component translateComponent(final String input, final Mine m) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        if (input.contains("%composition:")) {
            return Text.parse(this.translateAll(input, m));
        }
        String template = this.translateTags(input);
        final TagResolver.Builder resolver = TagResolver.builder();
        if (m != null) {
            final boolean needsBrokenStats = template.contains("%totalminedblocks%");
            final boolean needsResetStats = template.contains("%totalresets%");
            final StorageManager.MineStats stats = needsBrokenStats || needsResetStats
                    ? StorageManager.getStats(m) : null;
            template = template
                    .replace("%mine%", "<mine>")
                    .replace("%alias%", "<alias>")
                    .replace("%type%", "<type>")
                    .replace("%minedblocks%", "<minedblocks>")
                    .replace("%minedblockspercent%", "<minedblockspercent>")
                    .replace("%remainingblocks%", "<remainingblocks>")
                    .replace("%remainingblockspercent%", "<remainingblockspercent>")
                    .replace("%volume%", "<volume>")
                    .replace("%resettime%", "<resettime>")
                    .replace("%totalpercentage%", "<totalpercentage>")
                    .replace("%totalmaterials%", "<totalmaterials>")
                    .replace("%avgspeed%", "<avgspeed>")
                    .replace("%totalminedblocks%", "<totalminedblocks>")
                    .replace("%totalresets%", "<totalresets>");
            resolver.resolver(Placeholder.unparsed("mine", m.getName()));
            resolver.resolver(Placeholder.unparsed("alias", m.getAlias()));
            resolver.resolver(Placeholder.unparsed("type", m.getType().name()));
            resolver.resolver(Placeholder.unparsed("minedblocks",
                    String.valueOf(m.getMinedBlocks())));
            resolver.resolver(Placeholder.unparsed("minedblockspercent",
                    String.valueOf(m.getPercentageMined())));
            resolver.resolver(Placeholder.unparsed("remainingblocks",
                    String.valueOf(m.getRemainingBlocks())));
            resolver.resolver(Placeholder.unparsed("remainingblockspercent",
                    String.valueOf(m.getPercentageRemaining())));
            resolver.resolver(Placeholder.unparsed("volume", String.valueOf(m.getVolume())));
            resolver.resolver(Placeholder.unparsed("resettime",
                    StringUtils.secondsToString(m.getTimeToNextReset())));
            resolver.resolver(Placeholder.unparsed("totalpercentage",
                    String.valueOf(m.getTotalPercentage())));
            resolver.resolver(Placeholder.unparsed("totalmaterials",
                    String.valueOf(m.getTotalMaterials())));
            resolver.resolver(Placeholder.unparsed("avgspeed",
                    String.valueOf(m.averageBreakSpeed())));
            if (needsBrokenStats) {
                resolver.resolver(Placeholder.unparsed("totalminedblocks",
                        String.valueOf(stats.brokenBlocks())));
            }
            if (needsResetStats) {
                resolver.resolver(Placeholder.unparsed("totalresets",
                        String.valueOf(stats.resets())));
            }
        }
        return Text.parse(template, resolver.build());
    }

    public List<String> translateAll(final Iterable<String> get, final Mine m) {
        final ArrayList<String> strings = new ArrayList<>();
        for (final String s : get) {
            strings.add(this.translateAll(s, m));
        }
        return strings;
    }

    public String translateAll(String input, final Mine m) {
        StorageManager.MineStats stats = null;
        boolean needsBrokenStats = false;
        boolean needsResetStats = false;
        if (m != null) {
            needsBrokenStats = input.contains("%totalminedblocks%");
            needsResetStats = input.contains("%totalresets%");
            if (needsBrokenStats || needsResetStats) {
                stats = StorageManager.getStats(m);
            }
            input = input.replace("%mine%", Text.escape(m.getName()));
            input = input.replace("%minedblocks%", String.valueOf(m.getMinedBlocks()));
            input = input.replace("%minedblockspercent%", String.valueOf(m.getPercentageMined()));
            input = input.replace("%remainingblocks%", String.valueOf(m.getRemainingBlocks()));
            input = input
                    .replace("%remainingblockspercent%", String.valueOf(m.getPercentageRemaining()));
            input = input.replace("%volume%", String.valueOf(m.getVolume()));
            input =
                    input.replace("%resettime%", StringUtils.secondsToString(m.getTimeToNextReset()));
            input = input.replace("%alias%", Text.escape(m.getAlias()));
            input = input.replace("%type%", m.getType().name());
            input = input.replace("%totalpercentage%", String.valueOf(m.getTotalPercentage()));
            input = input.replace("%totalmaterials%", String.valueOf(m.getTotalMaterials()));
            input = input.replace("%avgspeed%", String.valueOf(m.averageBreakSpeed()));
            if (needsBrokenStats) {
                input = input.replace("%totalminedblocks%",
                        String.valueOf(stats.brokenBlocks()));
            }
            if (needsResetStats) {
                input = input.replace("%totalresets%", String.valueOf(stats.resets()));
            }
        }
        for (boolean translateComposition = input.contains("%composition:"); translateComposition;
             translateComposition = input.contains("%composition:")) {
            final int start = input.indexOf("%composition:") + 13;
            Integer end = null;
            for (int i = start - 12; i < input.length(); ++i) {
                final char c = input.charAt(i);
                if (c == '%') {
                    end = i;
                }
            }
            if (end == null) {
                break;
            }
            final String itemName = input.substring(start, end);
            ItemStack item;
            try {
                item = ItemUtils.convertFromInput(itemName);
            } catch (ItemConversionException ex) {
                GalaxyMines.instance.printException(input, ex);
                break;
            }
            final String find = "%composition:" + itemName + "%";
            assert m != null;
            input = input.replace(find, String.valueOf(m.getPercentage(item.getType())));
        }
        return this.translateAll(input);
    }

    public String getResetMessage(final Mine m) {
        return this.translateAll(GalaxyMineMessageManager.globalResetMessage, m);
    }

    @Override
    public String translateTags(String input) {
        if (GalaxyMineMessageManager.pluginPrefix != null) {
            input = input.replace("%prefix%", GalaxyMineMessageManager.pluginPrefix);
        }
        if (GalaxyMineMessageManager.messageSeparator != null) {
            input = input.replace("%separator%", GalaxyMineMessageManager.messageSeparator);
        }
        if (GalaxyMineMessageManager.header != null) {
            input = input.replace("%header%", GalaxyMineMessageManager.header);
        }
        return input;
    }

    public ItemStack createIcon(final ConfigurationSection iconSection) {
        return this.translateItemStack(ItemUtils.createItem(iconSection));
    }

    public ItemStack translateItemStack(final ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }
        if (itemMeta.hasDisplayName()) {
            itemMeta.displayName(itemText(this.translateAll(itemMeta.getDisplayName())));
        }
        if (itemMeta.hasLore()) {
            itemMeta.lore(this.translateLore(itemMeta.getLore(), null));
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
