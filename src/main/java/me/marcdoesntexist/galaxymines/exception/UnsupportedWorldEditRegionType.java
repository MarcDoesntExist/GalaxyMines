package me.marcdoesntexist.galaxymines.exception;

import com.sk89q.worldedit.regions.Region;

public class UnsupportedWorldEditRegionType extends RuntimeException {
    private final transient Region region;

    public UnsupportedWorldEditRegionType(final Region region) {
        super("Unsupported WorldEdit region type: "
                + (region == null ? "null" : region.getClass().getSimpleName()));
        this.region = region;
    }

    public Region getRegion() {
        return this.region;
    }
}
