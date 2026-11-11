package me.marcdoesntexist.galaxymines.events.mine;

import me.marcdoesntexist.galaxymines.mines.Mine;
import org.bukkit.event.Cancellable;

public class CancellableMineEvent<E extends CancellableMineEvent> extends MineEvent<E>
        implements Cancellable {

    protected boolean cancelled;

    public CancellableMineEvent(Mine mine) {
        super(mine);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
