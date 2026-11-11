package me.marcdoesntexist.galaxymines.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Iterator;
import java.util.Objects;

public class WorldEditIterator implements Iterator<Block> {
    private final Iterator<BlockVector3> bviterator;
    private final World world;

    public WorldEditIterator(final Region area) {
        this.bviterator = area.iterator();
        this.world = BukkitAdapter.adapt(Objects.requireNonNull(area.getWorld(), "region world"));
    }

    @Override
    public boolean hasNext() {
        return this.bviterator.hasNext();
    }

    @Override
    public Block next() {
        final BlockVector3 next = this.bviterator.next();
        return new Location2(this.world, next).getBlock();
    }


    private static final class Location2 {
        private final World world;
        private final BlockVector3 vector;

        private Location2(final World world, final BlockVector3 vector) {
            this.world = world;
            this.vector = vector;
        }

        private Block getBlock() {
            return this.world.getBlockAt(this.vector.x(), this.vector.y(), this.vector.z());
        }
    }
}
