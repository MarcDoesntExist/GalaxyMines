package me.marcdoesntexist.galaxymines.events.mine;

import me.marcdoesntexist.galaxymines.api.events.CustomEvent;
import me.marcdoesntexist.galaxymines.mines.Mine;

public class MineEvent<E extends CustomEvent> extends CustomEvent {
    protected final Mine mine;

    public MineEvent(Mine mine) {
        this.mine = mine;
    }

    public Mine getMine() {
        return mine;
    }
}
