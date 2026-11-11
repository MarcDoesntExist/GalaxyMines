package me.marcdoesntexist.galaxymines.gui.menus;

import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.api.utils.inventory.BasicInventoryMenu;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.BasicClickableInventoryObject;
import me.marcdoesntexist.galaxymines.api.utils.inventory.objects.interfaces.InventoryObjectClickListener;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.MenuConfig;
import me.marcdoesntexist.galaxymines.gui.GuiChatInput;
import me.marcdoesntexist.galaxymines.integration.economy.PriceEditorService;
import me.marcdoesntexist.galaxymines.integration.economy.PriceRegistry;
import me.marcdoesntexist.galaxymines.integration.economy.SellService;
import me.marcdoesntexist.galaxymines.messages.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;


public final class PriceEditorMenu extends BasicInventoryMenu {
    public static final String PERMISSION = "galaxymines.prices.edit";
    private static final String MENU = "priceeditor";
    private static final String ADMIN = "galaxymines.admin";

    private final File file;
    private Player viewer;
    private int page;
    private int pageSize;
    private int navSlot;

    public PriceEditorMenu(final File file) {
        super(MenuConfig.title(MENU, "&1&lSell prices"), MenuConfig.rows(MENU, 6));
        this.file = file;
    }

    public static void open(final Player player, final File file) {
        if (player == null || file == null) {
            return;
        }
        if (!player.hasPermission(PERMISSION) && !player.hasPermission(ADMIN)) {
            player.sendMessage(Text.parse(Messages.get("messages.command.noPermission",
                    "You do not have permission to use this command.")));
            return;
        }
        final PriceEditorMenu menu = new PriceEditorMenu(file);
        menu.initialSetup();
        menu.open(player);
    }

    private static List<String> keys() {
        final List<String> keys = new ArrayList<>(PriceRegistry.snapshot().keySet());
        keys.sort(Comparator.naturalOrder());
        return keys;
    }

    private static ItemStack icon(final Material material) {
        return new ItemStack(material == null ? Material.PAPER : material);
    }

    private static ItemStack named(final ItemStack base, final String name, final List<String> lore) {
        return ItemUtils.createItem(base, name, lore);
    }

    private static String money(final double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }


    @Override
    public void open(final Player player) {
        this.viewer = player;
        super.open(player);
    }

    private int pages(final int entries) {
        return Math.max(1, (entries + this.pageSize - 1) / this.pageSize);
    }


    private int slot(final String key, final int fallback) {
        final int configured = MenuConfig.slot(MENU, key, fallback);
        return Math.max(0, Math.min(configured, this.mainInventory.getSize() - 1));
    }

    @Override
    protected void setupItems() {
        final int size = this.mainInventory.getSize();
        this.navSlot = Math.max(0, size - 9);
        this.pageSize = Math.max(9, size - 9);

        final ItemStack fill = MenuConfig.fill(MENU);
        this.fill(fill == null ? icon(Material.BLACK_STAINED_GLASS_PANE) : fill);

        final List<String> keys = keys();
        final double multiplier = SellService.playerMultiplier(this.viewer);
        final int pages = this.pages(keys.size());
        this.page = Math.max(0, Math.min(this.page, pages - 1));
        final int start = this.page * this.pageSize;
        for (int index = 0; index < this.pageSize && start + index < keys.size(); ++index) {
            this.registerInventoryObject(this.entry(keys.get(start + index), multiplier),
                    index);
        }
        if (keys.isEmpty()) {
            this.mainInventory.setItem(this.navSlot / 2, named(icon(Material.PAPER),
                    Messages.get("priceEditor.emptyName", "<red>No prices configured"),
                    List.of(Messages.get("priceEditor.emptyLore",
                            "<gray>Use \"Add\" to create the first entry."))));
        }

        final int prevSlot = this.slot("prev", this.navSlot);
        final int nextSlot = this.slot("next", size - 1);
        final int addSlot = this.slot("add", Math.min(this.navSlot + 3, size - 1));
        final int closeSlot = this.slot("close", Math.min(this.navSlot + 4, size - 1));
        final int infoSlot = this.slot("info", Math.min(this.navSlot + 5, size - 1));

        this.registerInventoryObject(this.button(prevSlot, Material.ARROW,
                "priceEditor.prevName", "<yellow>Previous page", event -> {
                    if (this.page > 0) {
                        --this.page;
                        this.update();
                    }
                }), prevSlot);
        this.registerInventoryObject(this.button(nextSlot, Material.ARROW,
                "priceEditor.nextName", "<yellow>Next page", event -> {
                    if (this.page + 1 < this.pages(keys().size())) {
                        ++this.page;
                        this.update();
                    }
                }), nextSlot);
        this.registerInventoryObject(this.button(addSlot, Material.EMERALD,
                "priceEditor.addName", "<green>Add a price",
                event -> this.promptKey(event.getPlayer())), addSlot);
        this.registerInventoryObject(this.button(closeSlot, Material.BARRIER,
                "priceEditor.closeName", "<red>Close",
                event -> event.getPlayer().closeInventory()), closeSlot);

        this.mainInventory.setItem(infoSlot, named(icon(Material.GOLD_INGOT),
                Messages.get("priceEditor.infoName", "<aqua>Sell prices"),
                List.of(Messages.get("priceEditor.infoLore",
                                "<gray>Entries: <aqua>%entries%<gray>, page <aqua>%page%"
                                        + "<gray>/<aqua>%pages%",
                                "%entries%", Integer.toString(keys.size()),
                                "%page%", Integer.toString(this.page + 1),
                                "%pages%", Integer.toString(pages)),
                        Messages.get("priceEditor.infoFile",
                                "<gray>File: <white>%file%",
                                "%file%", this.file == null ? "-" : this.file.getName()),
                        Messages.get("priceEditor.infoMultiplier",
                                "<gray>Your sell multiplier: <aqua>x%multiplier%",
                                "%multiplier%", money(multiplier)))));
    }

    @Override
    public void update() {
        this.clear();
        this.setupItems();
    }

    private BasicClickableInventoryObject button(final int slot, final Material material,
                                                 final String nameKey, final String nameFallback,
                                                 final InventoryObjectClickListener action) {
        return new BasicClickableInventoryObject(
                named(icon(material), Messages.get(nameKey, nameFallback), List.of()),
                action, this);
    }

    private BasicClickableInventoryObject entry(final String key, final double multiplier) {
        final Double price = PriceRegistry.snapshot().get(key);
        final Material material = Material.matchMaterial(key);
        final List<String> lore = new ArrayList<>();
        lore.add(Messages.get("priceEditor.entryPrice",
                "<gray>Price: <aqua>%price%<gray> per item",
                "%price%", price == null ? "-" : Double.toString(price)));
        if (price != null) {
            lore.add(Messages.get("priceEditor.entryEffective",
                    "<gray>Effective: <green>%effective%<gray> (<white>x%multiplier%<gray>)",
                    "%effective%", money(price * multiplier),
                    "%multiplier%", money(multiplier)));
        }
        lore.add("");
        lore.add(Messages.get("priceEditor.entryEdit",
                "<yellow>Click: type a new price"));
        lore.add(Messages.get("priceEditor.entryRemove",
                "<red>Type \"remove\" to delete the entry"));
        return new BasicClickableInventoryObject(
                named(icon(material),
                        Messages.get("priceEditor.entryName", "<aqua>%key%", "%key%", key),
                        lore),
                event -> this.promptPrice(event.getPlayer(), key), this);
    }

    private void promptPrice(final Player player, final String key) {
        player.sendMessage(Text.parse(Messages.get("priceEditor.promptKeyName",
                "<gray>Editing <white>%key%<gray>.", "%key%", key)));
        GuiChatInput.request(player,
                "priceEditor.promptPrice",
                "<yellow>Type the new price for <white>%key%<yellow> in chat (or \"remove\"). "
                        + "You have %seconds% seconds.",
                value -> this.apply(player, key, value));
    }

    private void promptKey(final Player player) {
        GuiChatInput.request(player,
                "priceEditor.promptKey",
                "<yellow>Type the item key in chat (MATERIAL or provider:id), or \"cancel\". "
                        + "You have %seconds% seconds.",
                key -> {
                    final String normalized = PriceEditorService.validateKey(key);
                    if (normalized == null) {
                        player.sendMessage(Text.parse(Messages.get("priceEditor.invalidKey",
                                "<red>%key% is not a valid material or provider key.",
                                "%key%", key)));
                        return;
                    }
                    GuiChatInput.request(player,
                            "priceEditor.promptNewPrice",
                            "<yellow>Type the price for <white>%key%<yellow> in chat. "
                                    + "You have %seconds% seconds.",
                            value -> this.apply(player, normalized, value));
                });
    }

    private void apply(final Player player, final String key, final String value) {
        final PriceEditorService.Outcome outcome = PriceEditorService.isRemoval(value)
                ? PriceEditorService.remove(this.file, key)
                : PriceEditorService.set(this.file, key, value);
        switch (outcome.result()) {
            case OK -> {
                PriceRegistry.load(this.file);
                if (PriceEditorService.isRemoval(value)) {
                    player.sendMessage(Text.parse(Messages.get("priceEditor.removed",
                            "<green>Removed <white>%key%<green> from prices.yml.",
                            "%key%", outcome.key())));
                } else {
                    player.sendMessage(Text.parse(Messages.get("priceEditor.saved",
                            "<green>Saved <white>%key%<green> at <white>%price%<green>.",
                            "%key%", outcome.key(),
                            "%price%", Double.toString(outcome.value()))));
                }
                this.update();
            }
            case INVALID_KEY -> player.sendMessage(Text.parse(Messages.get("priceEditor.invalidKey",
                    "<red>%key% is not a valid material or provider key.",
                    "%key%", key)));
            case INVALID_PRICE -> player.sendMessage(Text.parse(Messages.get("priceEditor.invalidPrice",
                    "<red>%value% is not a valid price (0 &lt; price &lt;= %max%).",
                    "%value%", value,
                    "%max%", Double.toString(PriceEditorService.MAX_PRICE))));
            case NOT_FOUND -> player.sendMessage(Text.parse(Messages.get("priceEditor.notFound",
                    "<red>%key% is not configured in prices.yml.", "%key%", outcome.key())));
            case WRITE_FAILED -> player.sendMessage(Text.parse(Messages.get("priceEditor.writeFailed",
                    "<red>Could not write prices.yml.")));
        }
    }
}
