package me.marcdoesntexist.galaxymines.mines.types;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.MineConfig;
import me.marcdoesntexist.galaxymines.holograms.CompatibleHologram;
import me.marcdoesntexist.galaxymines.mines.MineBounds;
import me.marcdoesntexist.galaxymines.mines.MineType;
import me.marcdoesntexist.galaxymines.utils.AsyncFileWriter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CuboidMine extends BasicHologramMine {
    protected final Vector pos1;
    protected final Vector pos2;

    public CuboidMine(final MineConfig config) {
        super(config);
        this.pos1 = new Location(this.world, config.getConfig().getDouble("X1"),
                config.getConfig().getDouble("Y1"), config.getConfig().getDouble("Z1")).toVector();
        this.pos2 = new Location(this.world, config.getConfig().getDouble("X2"),
                config.getConfig().getDouble("Y2"), config.getConfig().getDouble("Z2")).toVector();
    }

    public CuboidMine(final String name, final Location loc1, final Location loc2) {
        super(name, loc1.getWorld(), ItemUtils.createItem(Material.STONE, name));
        this.pos1 = loc1.toVector();
        this.pos2 = loc2.toVector();
        this.config.set("X1", this.pos1.getBlockX());
        this.config.set("Y1", this.pos1.getBlockY());
        this.config.set("Z1", this.pos1.getBlockZ());
        this.config.set("X2", this.pos2.getBlockX());
        this.config.set("Y2", this.pos2.getBlockY());
        this.config.set("Z2", this.pos2.getBlockZ());
    }

    public Vector getPos2() {
        return this.pos2;
    }

    public Vector getPos1() {
        return this.pos1;
    }

    public File getSaveFile() {
        return this.saveFile;
    }

    public void setSaveFile(final File saveFile) {
        this.saveFile = saveFile;
    }

    public int getSizeX() {
        return this.pos2.getBlockX() - this.pos1.getBlockX() + 1;
    }

    public int getSizeY() {
        return this.pos2.getBlockY() - this.pos1.getBlockY() + 1;
    }

    public int getSizeZ() {
        return this.pos2.getBlockZ() - this.pos1.getBlockZ() + 1;
    }

    public Location getLowerNE() {
        return new Location(this.getWorld(), this.pos1.getX(), this.pos1.getY(), this.pos1.getZ());
    }

    public Location getUpperSW() {
        return new Location(this.getWorld(), this.pos2.getX(), this.pos2.getY(), this.pos2.getZ());
    }

    @Override
    public Location getCenter() {
        final int x1 = this.getUpperX() + 1;
        final int y1 = this.getUpperY() + 1;
        final int z1 = this.getUpperZ() + 1;
        return new Location(this.getWorld(), this.getLowerX() + (x1 - this.getLowerX()) / 2.0,
                this.getLowerY() + (y1 - this.getLowerY()) / 2.0,
                this.getLowerZ() + (z1 - this.getLowerZ()) / 2.0);
    }

    public int getLowerX() {
        return this.pos1.getBlockX();
    }

    public int getLowerZ() {
        return this.pos1.getBlockZ();
    }

    @Override
    public int getLowerY() {
        return this.pos1.getBlockY();
    }

    @Override
    public int getUpperY() {
        return this.pos2.getBlockY();
    }

    public int getUpperX() {
        return this.pos2.getBlockX();
    }

    public int getUpperZ() {
        return this.pos2.getBlockZ();
    }

    public Iterator<Block> iterator() {
        return new CuboidMineIterator(this.getWorld(), this.pos1.getBlockX(), this.pos1.getBlockY(),
                this.pos1.getBlockZ(), this.pos2.getBlockX(), this.pos2.getBlockY(),
                this.pos2.getBlockZ());
    }

    @Override
    public String getName() {
        return this.name;
    }

    public FileConfiguration toConfig() {
        try {
            final File f =
                    new File(GalaxyMines.minesFolder.getCanonicalPath() + "/" + this.name + ".yml");
            final FileConfiguration tempConfig = YamlConfiguration.loadConfiguration(f);
            tempConfig.set("X1", this.pos1.getBlockX());
            tempConfig.set("Y1", this.pos1.getBlockY());
            tempConfig.set("Z1", this.pos1.getBlockZ());
            tempConfig.set("X2", this.pos2.getBlockX());
            tempConfig.set("Y2", this.pos2.getBlockY());
            tempConfig.set("Z2", this.pos2.getBlockZ());
            tempConfig.set("name", this.name);
            tempConfig.set("world", this.world);
            final ArrayList<String> al = new ArrayList<String>();
            for (final Material m : this.getMaterials()) {
                al.add(m.name() + "=" + this.composition.get(m));
            }
            tempConfig.set("composition", al);
            tempConfig.set("resetDelay", this.totalResetDelay);
            return tempConfig;
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public MineType getType() {
        return MineType.CUBOID;
    }

    @Override
    public boolean supportsExpansion() {
        return true;
    }

    @Override
    public boolean expandHorizontally(final int blocks) {
        if (blocks <= 0) {
            return false;
        }
        final MineBounds bounds = this.getBounds();
        this.pos1.setX(bounds.minX() - blocks);
        this.pos1.setY(bounds.minY());
        this.pos1.setZ(bounds.minZ() - blocks);
        this.pos2.setX(bounds.maxX() + blocks);
        this.pos2.setY(bounds.maxY());
        this.pos2.setZ(bounds.maxZ() + blocks);
        this.config.set("X1", this.pos1.getBlockX());
        this.config.set("Y1", this.pos1.getBlockY());
        this.config.set("Z1", this.pos1.getBlockZ());
        this.config.set("X2", this.pos2.getBlockX());
        this.config.set("Y2", this.pos2.getBlockY());
        this.config.set("Z2", this.pos2.getBlockZ());
        this.save();
        return true;
    }

    @Override
    public boolean contains(final double x, final double y, final double z) {
        return x >= this.pos1.getBlockX() && x <= this.pos2.getBlockX()
                && y >= this.pos1.getBlockY() && y <= this.pos2.getBlockY()
                && z >= this.pos1.getBlockZ() && z <= this.pos2.getBlockZ();
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
    public void placeHolograms() {
        GalaxyMines.getInstance().debug("Creating holograms for " + this.name + "...");
        try {
            final Location l = this.getCenter();
            Location hololoc = l.clone();
            hololoc.setY(this.getUpperY());
            this.holograms.add(GalaxyMines.hologramAdapter.createHologram(hololoc));
            GalaxyMines.getInstance().debug("Created " + this.holograms.size() + " holograms.");
            this.hologramsReady = true;
        } catch (Exception e) {
            GalaxyMines.instance
                    .printException("There was an error creating hologram for mine " + this.name + "!",
                            e);
        }
    }

    @Override
    public int getVolume() {
        if (this.pos1 == null || this.pos2 == null) {
            return 0;
        }
        return this.getSizeX() * this.getSizeY() * this.getSizeZ();
    }

    @Override
    public MineBounds getBounds() {
        return new MineBounds(Math.min(this.pos1.getBlockX(), this.pos2.getBlockX()),
                Math.min(this.pos1.getBlockY(), this.pos2.getBlockY()),
                Math.min(this.pos1.getBlockZ(), this.pos2.getBlockZ()),
                Math.max(this.pos1.getBlockX(), this.pos2.getBlockX()),
                Math.max(this.pos1.getBlockY(), this.pos2.getBlockY()),
                Math.max(this.pos1.getBlockZ(), this.pos2.getBlockZ()));
    }

    @Override
    public List<CompatibleHologram> getHolograms() {
        return this.holograms;
    }

    @Override
    public FileConfiguration exportConfiguration() {
        final FileConfiguration file = this.getBasicSavedConfig();
        file.set("X1", this.pos1.getBlockX());
        file.set("Y1", this.pos1.getBlockY());
        file.set("Z1", this.pos1.getBlockZ());
        file.set("X2", this.pos2.getBlockX());
        file.set("Y2", this.pos2.getBlockY());
        file.set("Z2", this.pos2.getBlockZ());
        return file;
    }

    @Override
    public void save() {
        AsyncFileWriter.write(GalaxyMines.getMineFile(this),
                this.exportConfiguration().saveToString());
    }

    public enum CuboidDirection {
        North, East, South, West, Up, Down, Horizontal, Vertical, Both, Unknown;

        public CuboidDirection opposite() {
            switch (this) {
                case North: {
                    return CuboidDirection.South;
                }
                case East: {
                    return CuboidDirection.West;
                }
                case South: {
                    return CuboidDirection.North;
                }
                case West: {
                    return CuboidDirection.East;
                }
                case Horizontal: {
                    return CuboidDirection.Vertical;
                }
                case Vertical: {
                    return CuboidDirection.Horizontal;
                }
                case Up: {
                    return CuboidDirection.Down;
                }
                case Down: {
                    return CuboidDirection.Up;
                }
                case Both: {
                    return CuboidDirection.Both;
                }
                default: {
                    return CuboidDirection.Unknown;
                }
            }
        }
    }


    public static class CuboidMineIterator implements Iterator<Block> {
        private final World w;
        private final int baseX;
        private final int baseY;
        private final int baseZ;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private int x;
        private int y;
        private int z;

        public CuboidMineIterator(final World w, final int x1, final int y1, final int z1,
                                  final int x2, final int y2, final int z2) {
            this.w = w;
            this.baseX = x1;
            this.baseY = y1;
            this.baseZ = z1;
            this.sizeX = Math.abs(x2 - x1) + 1;
            this.sizeY = Math.abs(y2 - y1) + 1;
            this.sizeZ = Math.abs(z2 - z1) + 1;
            final int x3 = 0;
            this.z = x3;
            this.y = x3;
            this.x = x3;
        }

        @Override
        public boolean hasNext() {
            return this.x < this.sizeX && this.y < this.sizeY && this.z < this.sizeZ;
        }

        @Override
        public Block next() {
            final Block b =
                    this.w.getBlockAt(this.baseX + this.x, this.baseY + this.y, this.baseZ + this.z);
            if (++this.x >= this.sizeX) {
                this.x = 0;
                if (++this.y >= this.sizeY) {
                    this.y = 0;
                    ++this.z;
                }
            }
            return b;
        }

        @Override
        public void remove() {
        }
    }
}
