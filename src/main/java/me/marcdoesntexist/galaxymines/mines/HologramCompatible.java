package me.marcdoesntexist.galaxymines.mines;

import me.marcdoesntexist.galaxymines.holograms.CompatibleHologram;

import java.util.List;

public interface HologramCompatible {
    void setupHolograms();

    void showHolograms();

    void hideHolograms();

    void updateHolograms();

    void softHologramUpdate();

    boolean isHologramsVisible();

    boolean useCustomHologramText();

    List<CompatibleHologram> getHolograms();

    List<String> getHologramsLines();

    void setHologramsLines(final List<String> p0);
}
