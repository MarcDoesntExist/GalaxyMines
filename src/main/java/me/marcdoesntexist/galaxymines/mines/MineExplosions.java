package me.marcdoesntexist.galaxymines.mines;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockExplodeEvent;

import java.util.function.Consumer;





public final class MineExplosions {
    private MineExplosions() {
    }

    public static void visitBlocks(final BlockExplodeEvent event, final Consumer<Block> visitor) {
        if (event.isCancelled()) {
            return;
        }
        visitor.accept(event.getBlock());
        for (final Block block : event.blockList()) {
            visitor.accept(block);
        }
    }




    public static Player sourcePlayer(final Entity entity) {
        Entity current = entity;
        if (!(current instanceof TNTPrimed)) {
            return null;
        }
        for (int depth = 0; depth < 16 && current instanceof TNTPrimed tnt; ++depth) {
            final Entity source = tnt.getSource();
            if (source == null || source == current) {
                return null;
            }
            current = source;
        }
        return current instanceof Player player ? player : null;
    }
}
