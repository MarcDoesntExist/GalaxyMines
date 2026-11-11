package me.marcdoesntexist.galaxymines.holograms.impl;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.chat.Text;
import me.marcdoesntexist.galaxymines.config.PluginSettings;
import me.marcdoesntexist.galaxymines.holograms.CompatibleHologram;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;


public class DisplayHologram implements CompatibleHologram {
    private final List<LineDefinition> definitions = new ArrayList<>();
    private final List<Entity> lines = new ArrayList<>();
    private final Map<Entity, String> textCache = new HashMap<>();
    private final PluginSettings.Holograms settings;
    private Location base;

    public DisplayHologram(final Location base) {
        this.base = base.clone();
        this.settings = PluginSettings.get().holograms;
    }

    private static Display.Billboard billboard(final String name,
                                               final Display.Billboard fallback) {
        try {
            return Display.Billboard.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return fallback;
        }
    }

    private static NamespacedKey key() {
        return new NamespacedKey(GalaxyMines.getInstance(), "hologram");
    }

    private static void tag(final Entity entity) {
        entity.getPersistentDataContainer().set(key(), PersistentDataType.BYTE, (byte) 1);
    }


    public static int cleanupOrphans() {
        int removed = 0;
        for (final org.bukkit.World world : Bukkit.getWorlds()) {
            for (final Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(key(), PersistentDataType.BYTE)) {
                    entity.remove();
                    ++removed;
                }
            }
        }
        return removed;
    }

    private static void remove(final Entity entity) {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    @Override
    public void clearLines() {
        for (final Entity line : this.lines) {
            remove(line);
        }
        this.lines.clear();
        this.definitions.clear();
        this.textCache.clear();
    }

    @Override
    public void appendItemLine(final ItemStack icon) {
        final ItemStack item = icon == null ? new ItemStack(Material.STONE) : icon.clone();
        item.setAmount(1);
        this.definitions.add(LineDefinition.item(item));
        this.lines.add(this.spawn(this.definitions.size() - 1));
    }

    @Override
    public void appendTextLine(final String text) {
        this.definitions.add(LineDefinition.text(text));
        this.lines.add(this.spawn(this.definitions.size() - 1));
    }

    @Override
    public void setLine(final int line, final String text) {
        if (line < 0 || line >= this.definitions.size()) {
            return;
        }
        final LineDefinition definition = this.definitions.get(line);
        if (definition.item() != null) {
            return;
        }
        this.definitions.set(line, LineDefinition.text(text));
        final Entity entity = this.lines.get(line);
        if (entity instanceof TextDisplay && entity.isValid()) {
            if (text.equals(this.textCache.get(entity))) {
                return;
            }
            final Component component = Text.parse(text);
            ((TextDisplay) entity).text(component);
            this.textCache.put(entity, text);
        }
    }

    @Override
    public void removeLine(final int line) {
        if (line < 0 || line >= this.definitions.size()) {
            return;
        }
        this.definitions.remove(line);
        final Entity removed = this.lines.remove(line);
        this.textCache.remove(removed);
        remove(removed);
        this.layout();
    }

    @Override
    public void delete() {
        this.clearLines();
    }

    @Override
    public void move(final World world, final double x, final double y, final double z) {
        this.move(new Location(world, x, y, z));
    }

    @Override
    public void move(final Location location) {
        this.base = location.clone();
        this.layout();
    }

    @Override
    public Location getLocation() {
        return this.base.clone();
    }

    @Override
    public int size() {
        return this.definitions.size();
    }

    @Override
    public boolean isSpawned() {
        if (this.definitions.isEmpty()) {
            return true;
        }
        if (this.lines.size() != this.definitions.size()) {
            return false;
        }
        for (final Entity line : this.lines) {
            if (line == null || !line.isValid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void recreate() {
        for (final Entity line : this.lines) {
            remove(line);
        }
        this.lines.clear();
        this.textCache.clear();
        for (int i = 0; i < this.definitions.size(); ++i) {
            this.lines.add(this.spawn(i));
        }
    }

    private Entity spawn(final int index) {
        final LineDefinition definition = this.definitions.get(index);
        try {
            if (definition.item() != null) {
                return this.spawnItem(this.lineLocation(index), definition.item());
            }
            final TextDisplay display = this.spawnText(this.lineLocation(index));
            display.text(Text.parse(definition.text()));
            this.textCache.put(display, definition.text());
            return display;
        } catch (RuntimeException ex) {
            GalaxyMines.getInstance().printException(
                    "Could not spawn a hologram line at " + this.lineLocation(index), ex);
            return null;
        }
    }

    private TextDisplay spawnText(final Location location) {
        return location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.setBillboard(billboard(this.settings.billboard, Display.Billboard.CENTER));
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(this.settings.shadowed);
            display.setSeeThrough(this.settings.seeThrough);
            display.setDefaultBackground(false);
            display.setBackgroundColor(
                    Color.fromARGB(this.settings.backgroundAlpha, 0, 0, 0));
            display.setLineWidth(this.settings.lineWidth);
            display.setPersistent(false);
            display.setTeleportDuration(this.settings.teleportDuration);
            display.setGravity(false);
            display.setInvulnerable(true);
            tag(display);
        });
    }

    private ItemDisplay spawnItem(final Location location, final ItemStack item) {
        return location.getWorld().spawn(location, ItemDisplay.class, display -> {
            display.setItemStack(item);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            display.setBillboard(billboard(this.settings.itemBillboard, Display.Billboard.FIXED));
            display.setPersistent(false);
            display.setTeleportDuration(this.settings.teleportDuration);
            display.setGravity(false);
            display.setInvulnerable(true);
            tag(display);
        });
    }

    private Location lineLocation(final int index) {
        return this.base.clone().add(0.0D, -this.settings.lineSpacing * index, 0.0D);
    }

    private void layout() {
        for (int i = 0; i < this.lines.size(); ++i) {
            final Entity entity = this.lines.get(i);
            if (entity == null || !entity.isValid()) {
                continue;
            }
            entity.teleport(this.lineLocation(i));
        }
    }

    private record LineDefinition(String text, ItemStack item) {
        private static LineDefinition text(final String text) {
            return new LineDefinition(text, null);
        }

        private static LineDefinition item(final ItemStack item) {
            return new LineDefinition(null, item);
        }
    }
}
