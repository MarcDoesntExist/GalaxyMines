package me.marcdoesntexist.galaxymines.mines;

import org.bukkit.block.Block;

import java.util.Iterator;
import java.util.NoSuchElementException;


public final class MineWaveIterator implements Iterator<Block> {
    private final Mine mine;
    private final MineBounds bounds;
    private int y;
    private int x;
    private int z;
    private Block next;

    public MineWaveIterator(final Mine mine) {
        this.mine = mine;
        this.bounds = mine.getBounds();
        this.y = this.bounds.maxY();
        this.x = this.bounds.minX();
        this.z = this.bounds.minZ();
        this.advance();
    }

    @Override
    public boolean hasNext() {
        return this.next != null;
    }

    @Override
    public Block next() {
        if (this.next == null) {
            throw new NoSuchElementException();
        }
        final Block result = this.next;
        this.advance();
        return result;
    }

    private void advance() {
        this.next = null;
        while (this.y >= this.bounds.minY()) {
            while (this.z <= this.bounds.maxZ()) {
                while (this.x <= this.bounds.maxX()) {
                    final int currentX = this.x++;
                    if (this.mine.contains(currentX + 0.5D, this.y + 0.5D,
                            this.z + 0.5D)) {
                        this.next = this.mine.getWorld().getBlockAt(currentX, this.y, this.z);
                        return;
                    }
                }
                this.x = this.bounds.minX();
                ++this.z;
            }
            this.z = this.bounds.minZ();
            --this.y;
        }
    }
}
