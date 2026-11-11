package me.marcdoesntexist.galaxymines.mines.types;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.MineConfig;
import me.marcdoesntexist.galaxymines.mines.MineBounds;
import me.marcdoesntexist.galaxymines.mines.MineType;
import me.marcdoesntexist.galaxymines.utils.AsyncFileWriter;
import me.marcdoesntexist.galaxymines.utils.WorldEditIterator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PolygonMine extends BasicHologramMine {
    private final List<Vector> points;
    private final int height;
    private final Polygonal2DRegion area;

    public PolygonMine(final MineConfig config) {
        super(config);
        final FileConfiguration fileConfig = config.getConfig();
        final ConfigurationSection pointsConfig = fileConfig.getConfigurationSection("points");
        final ArrayList<BlockVector2> bvectors = new ArrayList<>();
        final ArrayList<Vector> vectors = new ArrayList<>();
        Integer lowestPoint = null;
        Integer highestPoint = null;
        if (pointsConfig != null) {
            for (final String pointsPath : pointsConfig.getKeys(false)) {
                final String pointPath = "points." + pointsPath;
                final int x = fileConfig.getInt(pointPath + ".x");
                final int y = fileConfig.getInt(pointPath + ".y");
                final int z = fileConfig.getInt(pointPath + ".z");
                final Vector v = new Vector(x, y, z);
                vectors.add(v);
                bvectors.add(BlockVector2.at(x, z));
                if (lowestPoint == null || y < lowestPoint) {
                    lowestPoint = y;
                }
                if (highestPoint == null || y > highestPoint) {
                    highestPoint = y;
                }
            }
        }
        if (lowestPoint == null) {
            lowestPoint = 0;
        }
        if (highestPoint == null) {
            highestPoint = lowestPoint;
        }
        this.area = new Polygonal2DRegion(BukkitAdapter.adapt(this.world), bvectors, lowestPoint,
                highestPoint);
        this.points = vectors;
        this.height = highestPoint - lowestPoint;
    }

    public PolygonMine(final Vector[] points, final String name, final World world,
                       final ItemStack icon) {
        super(name, world, icon);
        this.points = new ArrayList<>(Arrays.asList(points));
        final ArrayList<BlockVector2> bvectors = new ArrayList<>();
        Integer lowestPoint = null;
        Integer highestPoint = null;
        for (final Vector point : points) {
            bvectors.add(BlockVector2.at(point.getBlockX(), point.getBlockZ()));
            if (lowestPoint == null || point.getBlockY() < lowestPoint) {
                lowestPoint = point.getBlockY();
            }
            if (highestPoint == null || point.getBlockY() > highestPoint) {
                highestPoint = point.getBlockY();
            }
        }
        final int min = lowestPoint == null ? 0 : lowestPoint;
        final int max = highestPoint == null ? min : highestPoint;
        this.area = new Polygonal2DRegion(BukkitAdapter.adapt(world), bvectors, min, max);
        this.height = max - min;
    }

    public PolygonMine(final Polygonal2DRegion region, final String name, final World world) {
        super(name, world, ItemUtils.createItem(Material.REDSTONE_BLOCK, name));
        Integer lowestPoint = null;
        Integer highestPoint = null;
        final ArrayList<Vector> vectors = new ArrayList<>();
        boolean min = false;
        for (final BlockVector2 bv : region.getPoints()) {
            final Vector point =
                    new Vector(bv.x(), min ? region.getMinimumY() : region.getMaximumY(), bv.z());
            min = !min;
            vectors.add(point);
            if (lowestPoint == null || point.getBlockY() < lowestPoint) {
                lowestPoint = point.getBlockY();
            }
            if (highestPoint == null || point.getBlockY() > highestPoint) {
                highestPoint = point.getBlockY();
            }
        }
        this.points = vectors;
        this.area = region;
        final int min2 = lowestPoint == null ? region.getMinimumY() : lowestPoint;
        final int max2 = highestPoint == null ? region.getMaximumY() : highestPoint;
        this.height = max2 - min2;
    }

    @Override
    public void placeHolograms() {
        for (final Vector v : this.points) {
            this.holograms.add(GalaxyMines.hologramAdapter
                    .createHologram(v.clone().setY(this.getUpperY()).toLocation(this.world)));
        }
    }

    @Override
    public FileConfiguration exportConfiguration() {
        final FileConfiguration file = this.getBasicSavedConfig();
        file.set("points", null);
        int i = 0;
        for (final Vector v : this.points) {
            final String point = "points.point" + i;
            file.set(point + ".x", v.getBlockX());
            file.set(point + ".y", v.getBlockY());
            file.set(point + ".z", v.getBlockZ());
            ++i;
        }
        return file;
    }

    @Override
    public void save() {
        AsyncFileWriter.write(GalaxyMines.getMineFile(this),
                this.exportConfiguration().saveToString());
    }

    @Override
    public MineType getType() {
        return MineType.POLYGON;
    }

    @Override
    public boolean contains(final double x, final double y, final double z) {
        final boolean downGrade = y <= this.getUpperY() + 1 && y >= this.getUpperY();
        return this.area.contains(BlockVector3.at(x, downGrade ? (y - 1.0D) : y, z));
    }

    @Override
    public Location getCenter() {
        final Vector3 center = this.area.getCenter();
        return new Location(this.world, center.x(), this.getUpperY() - this.height / 2.0D,
                center.z());
    }

    @Override
    public int getLowerY() {
        return this.area.getMinimumY();
    }

    @Override
    public int getUpperY() {
        return this.area.getMaximumY();
    }

    @Override
    public int getVolume() {
        if (this.area == null) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, this.area.getVolume()));
    }

    @Override
    public MineBounds getBounds() {
        final BlockVector3 minimum = this.area.getMinimumPoint();
        final BlockVector3 maximum = this.area.getMaximumPoint();
        return new MineBounds(minimum.x(), this.getLowerY(), minimum.z(),
                maximum.x(), this.getUpperY(), maximum.z());
    }

    public Iterator<Block> iterator() {
        return new WorldEditIterator(this.area);
    }
}
