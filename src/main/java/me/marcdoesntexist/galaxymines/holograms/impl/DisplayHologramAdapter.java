package me.marcdoesntexist.galaxymines.holograms.impl;

import me.marcdoesntexist.galaxymines.holograms.CompatibleHologram;
import me.marcdoesntexist.galaxymines.holograms.HologramAdapter;
import org.bukkit.Location;




public class DisplayHologramAdapter implements HologramAdapter {
    @Override
    public CompatibleHologram createHologram(final Location location) {
        return new DisplayHologram(location);
    }
}
