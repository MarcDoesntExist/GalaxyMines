package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.utils.RandomCollection;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.EnumMap;
import java.util.Map;


public final class MineResetPalette {
    private final RandomCollection<ResetBlock> blocks;
    private final double blockPercentage;
    private final BlockData air;

    public MineResetPalette(final Mine mine) {
        this.blocks = new RandomCollection<>();
        final Map<Material, Double> composition = mine.getComposition();
        final double total = composition == null ? 0.0D
                : CompositionMath.total(composition.values());
        this.blockPercentage = Math.min(100.0D, Math.max(0.0D, total));
        this.air = Material.AIR.createBlockData();
        final Map<Material, BlockData> cache = new EnumMap<>(Material.class);
        if (composition != null) {
            for (final Map.Entry<Material, Double> entry : composition.entrySet()) {
                final double percentage = entry.getValue() == null ? 0.0D : entry.getValue();
                if (percentage <= 0.0D) {
                    continue;
                }
                final Material material = entry.getKey();
                if (material == null || material.isAir()) {
                    continue;
                }
                final BlockData data = cache.computeIfAbsent(material, Material::createBlockData);
                this.blocks.add(percentage, new ResetBlock(data));
            }
        }
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty();
    }

    public ResetBlock nextBlock() {
        return this.blocks.next();
    }

    public BlockData air() {
        return this.air;
    }


    public double blockPercentage() {
        return this.blockPercentage;
    }


    public static final class ResetBlock {
        private final BlockData data;

        private ResetBlock(final BlockData data) {
            this.data = data;
        }

        public BlockData data() {
            return this.data;
        }

        public void apply(final Block block) {
            block.setBlockData(this.data, false);
        }
    }
}
