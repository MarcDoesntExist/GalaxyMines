package me.marcdoesntexist.galaxymines.utils;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.entity.Player;


public final class WorldEditHook {

    private WorldEditHook() {
    }

    public static Region getSelection(final Player player) {
        if (player == null) {
            return null;
        }
        try {
            final LocalSession session =
                    WorldEdit.getInstance().getSessionManager().findByName(player.getName());
            if (session == null) {
                return null;
            }
            final Region region = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            if (region == null || region.getVolume() <= 0) {
                return null;
            }
            return region;
        } catch (final IncompleteRegionException | RuntimeException ignored) {
            return null;
        }
    }
}
