package me.marcdoesntexist.galaxymines.mines.types;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CylinderRegion;
import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.api.utils.items.ItemUtils;
import me.marcdoesntexist.galaxymines.config.MineConfig;
import me.marcdoesntexist.galaxymines.mines.MineBounds;
import me.marcdoesntexist.galaxymines.mines.MineType;
import me.marcdoesntexist.galaxymines.utils.AsyncFileWriter;
import me.marcdoesntexist.galaxymines.utils.WorldEditIterator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.util.Iterator;

public class CircularMine extends BasicHologramMine {
    private final Vector center;
    private final double radius;
    private final int minY;
    private final int maxY;
    private final CylinderRegion area;

    public CircularMine(final MineConfig config) {
        super(config);
        final FileConfiguration file = config.getConfig();
        final double x = file.getDouble("X");
        final double y = file.getDouble("Y");
        final double z = file.getDouble("Z");
        this.radius = Math.max(0.5D, file.getDouble("radius", 1.0D));
        this.minY = (int) Math.floor(y);
        this.maxY = this.minY + Math.max(0, file.getInt("height", 0));
        this.area = new CylinderRegion(BukkitAdapter.adapt(this.world), BlockVector3.at(x, y, z),
                Vector2.at(this.radius, this.radius), this.minY, this.maxY);
        final Vector3 center2 = this.area.getCenter();
        this.center = new Vector(center2.x() + 0.5D, (this.minY + this.maxY) / 2.0D,
                center2.z() + 0.5D);
    }

    public CircularMine(final String name, final Location center, final double radius,
                        final int height) {
        super(name, center.getWorld(), ItemUtils.createItem(Material.STONE, name));
        this.radius = Math.max(0.5D, radius);
        this.minY = center.getBlockY();
        this.maxY = this.minY + Math.max(0, height);
        this.area = new CylinderRegion(BukkitAdapter.adapt(this.world),
                BlockVector3.at(center.getX(), center.getY(), center.getZ()),
                Vector2.at(this.radius, this.radius), this.minY, this.maxY);
        final Vector3 center2 = this.area.getCenter();
        this.center = new Vector(center2.x() + 0.5D, (this.minY + this.maxY) / 2.0D,
                center2.z() + 0.5D);
        this.config.set("X", this.center.getBlockX());
        this.config.set("Y", this.getLowerY());
        this.config.set("Z", this.center.getBlockZ());
        this.config.set("radius", this.radius);
        this.config.set("height", this.maxY - this.minY);
    }

    @Override
    public FileConfiguration exportConfiguration() {
        final FileConfiguration file = this.getBasicSavedConfig();
        file.set("X", this.center.getBlockX());
        file.set("Y", this.getLowerY());
        file.set("Z", this.center.getBlockZ());
        file.set("radius", this.radius);
        file.set("height", this.getHeight() - 1);
        return file;
    }

    @Override
    public void save() {
        AsyncFileWriter.write(GalaxyMines.getMineFile(this),
                this.exportConfiguration().saveToString());
    }

    @Override
    public MineType getType() {
        return MineType.CIRCULAR;
    }

    @Override
    public boolean contains(final double x, final double y, final double z) {
        final boolean downGrade = y <= this.getUpperY() + 1 && y >= this.getUpperY();
        return this.area.contains(BlockVector3.at(x, downGrade ? (y - 1.0D) : y, z));
    }

    @Override
    public Location getCenter() {
        return this.center.toLocation(this.world);
    }

    @Override
    public int getVolume() {
        if (this.area == null) {
            return 0;
        }
        final Vector2 radius2 = this.area.getRadius();
        final double volume =
                Math.PI * Math.abs(radius2.x()) * Math.abs(radius2.z()) * this.getHeight();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, Math.round(volume)));
    }

    @Override
    public MineBounds getBounds() {
        return new MineBounds((int) Math.floor(this.center.getX() - this.radius),
                this.minY, (int) Math.floor(this.center.getZ() - this.radius),
                (int) Math.ceil(this.center.getX() + this.radius), this.maxY,
                (int) Math.ceil(this.center.getZ() + this.radius));
    }

    public Iterator<Block> iterator() {
        return new WorldEditIterator(this.area);
    }

    public int getArea() {
        return this.getVolume();
    }

    public int getHeight() {
        return this.maxY - this.minY + 1;
    }

    public double getRadius() {
        return this.radius;
    }

    public Vector getVectorCenter() {
        return this.center;
    }

    @Override
    public int getLowerY() {
        return this.minY;
    }

    @Override
    public int getUpperY() {
        return this.maxY;
    }

    @Override
    public void placeHolograms() {
        GalaxyMines.getInstance().debug("Creating holograms for " + this.name + "...");
        final Location l = this.getVectorCenter().toLocation(this.world);
        Location temp = l.clone();
        temp.setY(this.getUpperY());
        this.holograms.add(GalaxyMines.hologramAdapter.createHologram(temp));
        final double radiusX = this.area.getRadius().x();
        final double radiusZ = this.area.getRadius().z();
        temp = l.clone();
        temp.add(radiusX + 1.0D, 0.0D, 0.0D);
        this.holograms.add(GalaxyMines.hologramAdapter.createHologram(temp));
        temp = l.clone();
        temp.add(radiusX * -1.0D - 1.0D, 0.0D, 0.0D);
        this.holograms.add(GalaxyMines.hologramAdapter.createHologram(temp));
        temp = l.clone();
        temp.add(0.0D, 0.0D, radiusZ + 1.0D);
        this.holograms.add(GalaxyMines.hologramAdapter.createHologram(temp));
        temp = l.clone();
        temp.add(0.0D, 0.0D, radiusZ * -1.0D - 1.0D);
        this.holograms.add(GalaxyMines.hologramAdapter.createHologram(temp));
        GalaxyMines.getInstance().debug("Created " + this.holograms.size() + " holograms.");
        this.updateHolograms();
        this.hologramsReady = true;
    }
}
