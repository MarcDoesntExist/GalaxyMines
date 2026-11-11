package me.marcdoesntexist.galaxymines.mines.types;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.config.GalaxyMinesConfiguration;
import me.marcdoesntexist.galaxymines.config.MineConfig;
import me.marcdoesntexist.galaxymines.events.mine.MineHologramUpdateEvent;
import me.marcdoesntexist.galaxymines.holograms.CompatibleHologram;
import me.marcdoesntexist.galaxymines.messages.GalaxyMineMessageManager;
import me.marcdoesntexist.galaxymines.mines.CompositionMath;
import me.marcdoesntexist.galaxymines.mines.HologramCompatible;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public abstract class BasicHologramMine extends BasicMine implements HologramCompatible {
    protected final List<Integer> tagLines;
    protected final ArrayList<CompatibleHologram> holograms;
    private final boolean useCustomHologramText;
    protected boolean hologramsReady;
    protected List<String> hologramsLines;
    private BukkitTask lightHologramUpdateTask;

    public BasicHologramMine(final String name, final World world, final ItemStack icon) {
        super(name, world, icon);
        this.hologramsReady = false;
        this.tagLines = new ArrayList<>();
        this.holograms = new ArrayList<>();
        this.useCustomHologramText = false;
    }

    public BasicHologramMine(final MineConfig config) {
        super(config);
        this.hologramsReady = false;
        this.tagLines = new ArrayList<>();
        this.holograms = new ArrayList<>();
        this.useCustomHologramText = config.getConfig().getBoolean("useCustomHologramText");
    }

    @Override
    public void setupHolograms() {
        if (GalaxyMinesConfiguration.forceDefaultHolograms) {
            GalaxyMines.getInstance().debug("Setting default hologram text for mine " + this.name
                    + " because forceDefaultHologramOnAllMines is enabled on the config");
            this.setHologramsLines(GalaxyMines.defaultHologramText);
        } else if (this.config.getBoolean("useCustomHologramText")) {
            GalaxyMines.getInstance().debug("Setting custom hologram text for mine " + this.name);
            this.setHologramsLines(this.config.getStringList("hologramsText"));
        } else {
            GalaxyMines.getInstance().debug("Setting default hologram text for mine " + this.name
                    + " since useCustomHologramText is disabled");
            this.setHologramsLines(GalaxyMines.defaultHologramText);
        }
        for (int i = 0; i < this.getHologramsLines().size(); ++i) {
            final String text = this.getHologramsLines().get(i);
            if (text.contains("%") && !text.equalsIgnoreCase("%item%")) {
                this.tagLines.add(i);
            }
        }
        this.placeHolograms();
        final int totallines = this.getHologramsLines().size();
        for (final CompatibleHologram h : this.getHolograms()) {
            h.move(h.getLocation().clone().add(0.0, 0.3 * totallines + 1.3, 0.0));
            h.clearLines();
            for (final String text2 : this.getHologramsLines()) {
                if (text2.equalsIgnoreCase("%icon%")) {
                    h.appendItemLine(this.icon);
                } else {
                    h.appendTextLine(
                            GalaxyMineMessageManager.getInstance().translateAll(text2, this));
                }
            }
        }
        this.updateHolograms();
    }

    public abstract void placeHolograms();

    @Override
    public void showHolograms() {
        this.updateHolograms();
    }

    @Override
    public void hideHolograms() {
        for (final CompatibleHologram m : this.getHolograms()) {
            m.clearLines();
        }
    }

    @Override
    public void softHologramUpdate() {
        if (this.holograms.isEmpty()) {
            return;
        }
        if (this.lightHologramUpdateTask == null || this.lightHologramUpdateTask.isCancelled()) {
            GalaxyMines.getInstance().debug("Updating hologram softly.");
            this.lightHologramUpdateTask = Bukkit.getScheduler()
                    .runTaskLater(GalaxyMines.instance, () -> {
                        BasicHologramMine.this.updateHolograms();
                        BasicHologramMine.this.lightHologramUpdateTask = null;
                    }, 60L);
        }
    }

    private void cancelPendingHologramUpdate() {
        if (this.lightHologramUpdateTask != null) {
            this.lightHologramUpdateTask.cancel();
            this.lightHologramUpdateTask = null;
        }
    }

    @Override
    public void disable() {
        this.cancelPendingHologramUpdate();
        super.disable();
    }

    @Override
    public void delete() {
        this.cancelPendingHologramUpdate();
        super.delete();
    }

    @Override
    public boolean isHologramsVisible() {
        return this.hologramsReady;
    }

    @Override
    public void updateHolograms() {
        if (this.isDeleted()) {
            return;
        }
        if (this.getHolograms().isEmpty()) {
            return;
        }
        GalaxyMines.getInstance().debug("Updating holograms for " + this.name, 1);
        GalaxyMines.getInstance().debug("Total lines: " + this.getHologramsLines().size(), 1);
        boolean cancelled = false;
        if (MineHologramUpdateEvent.getHandlerList().getRegisteredListeners().length != 0) {
            cancelled = ((MineHologramUpdateEvent) new MineHologramUpdateEvent(this).call())
                    .isCancelled();
        }
        if (!cancelled) {
            for (final CompatibleHologram h : this.getHolograms()) {
                for (final int i : this.tagLines) {
                    final String text = this.getHologramsLines().get(i);
                    h.setLine(i, GalaxyMineMessageManager.getInstance().translateAll(text, this));
                }
            }
            GalaxyMines.getInstance().debug("Holograms updated", 1);
        } else {
            GalaxyMines.getInstance()
                    .debug("Hologram Update Event for mine " + this.name + " was cancelled", 1);
        }
    }

    @Override
    public boolean useCustomHologramText() {
        return this.useCustomHologramText;
    }

    @Override
    public List<CompatibleHologram> getHolograms() {
        return this.holograms;
    }

    @Override
    public List<String> getHologramsLines() {
        return this.hologramsLines;
    }

    @Override
    public void setHologramsLines(final List<String> lines) {
        this.hologramsLines = lines;
    }

    @Override
    public boolean isExceedingMaterials() {
        return CompositionMath.exceeds(this.getTotalPercentage());
    }
}
