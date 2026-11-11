package me.marcdoesntexist.galaxymines.mines;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;


public final class MineChunkTickets {
    private static final Handle NONE = new Handle() {
        @Override
        public void release() {
        }

        @Override
        public int chunkCount() {
            return 0;
        }
    };

    private MineChunkTickets() {
    }


    public static Handle none() {
        return NONE;
    }


    public static Handle acquire(final Plugin plugin, final World world, final MineBounds bounds,
                                 final int maxChunks) {
        if (plugin == null || world == null || bounds == null) {
            return NONE;
        }
        final long chunkCount = (long) (bounds.maxChunkX() - bounds.minChunkX() + 1)
                * (bounds.maxChunkZ() - bounds.minChunkZ() + 1);
        if (chunkCount <= 0 || chunkCount > Math.max(1, maxChunks)) {
            return NONE;
        }
        final int count = (int) chunkCount;
        int acquired = 0;
        try {
            for (int chunkX = bounds.minChunkX(); chunkX <= bounds.maxChunkX(); ++chunkX) {
                for (int chunkZ = bounds.minChunkZ(); chunkZ <= bounds.maxChunkZ(); ++chunkZ) {
                    if (world.addPluginChunkTicket(chunkX, chunkZ, plugin)) {
                        ++acquired;
                    }
                }
            }
        } catch (RuntimeException ex) {
            release(plugin, world, bounds);
            return NONE;
        }
        if (acquired == 0) {
            return NONE;
        }
        final int ticketCount = acquired;
        return new Handle() {
            private boolean released;

            @Override
            public void release() {
                if (this.released) {
                    return;
                }
                this.released = true;
                MineChunkTickets.release(plugin, world, bounds);
            }

            @Override
            public int chunkCount() {
                return ticketCount;
            }
        };
    }

    private static void release(final Plugin plugin, final World world, final MineBounds bounds) {
        for (int chunkX = bounds.minChunkX(); chunkX <= bounds.maxChunkX(); ++chunkX) {
            for (int chunkZ = bounds.minChunkZ(); chunkZ <= bounds.maxChunkZ(); ++chunkZ) {
                try {
                    world.removePluginChunkTicket(chunkX, chunkZ, plugin);
                } catch (RuntimeException ignored) {
                }
            }
        }
    }


    public interface Handle {
        void release();

        int chunkCount();
    }
}
