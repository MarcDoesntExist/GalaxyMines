package me.marcdoesntexist.galaxymines.holograms;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.mines.HologramCompatible;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.mines.MineManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Collection;









public final class HologramChunkListener implements Listener {





    static int recreateInChunk(final Chunk chunk, final Collection<? extends Mine> mines) {
        if (chunk == null || mines == null) {
            return 0;
        }
        int recreated = 0;
        for (final Mine mine : mines) {
            if (!(mine instanceof HologramCompatible)) {
                continue;
            }
            for (final CompatibleHologram hologram
                    : ((HologramCompatible) mine).getHolograms()) {
                if (hologram == null || hologram.isSpawned()) {
                    continue;
                }
                final Location location = hologram.getLocation();
                if (location == null || location.getWorld() == null
                        || !location.getWorld().equals(chunk.getWorld())
                        || (location.getBlockX() >> 4) != chunk.getX()
                        || (location.getBlockZ() >> 4) != chunk.getZ()) {
                    continue;
                }
                try {
                    hologram.recreate();
                    ++recreated;
                } catch (RuntimeException ex) {
                    if (GalaxyMines.instance != null) {
                        GalaxyMines.instance.printException(
                                "Could not recreate a hologram for mine " + mine.getName(), ex);
                    }
                }
            }
        }
        return recreated;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(final ChunkLoadEvent event) {
        recreateInChunk(event.getChunk(), MineManager.getMines());
    }
}
