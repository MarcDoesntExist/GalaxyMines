package me.marcdoesntexist.galaxymines.bomb;

import java.util.ArrayList;
import java.util.List;


public final class BombShape {
    private BombShape() {
    }


    public static List<int[]> offsets(final int radius) {
        final int limit = Math.max(0, radius);
        final List<int[]> offsets = new ArrayList<>();
        final int squared = limit * limit;
        for (int x = -limit; x <= limit; ++x) {
            for (int y = -limit; y <= limit; ++y) {
                for (int z = -limit; z <= limit; ++z) {
                    if (x * x + y * y + z * z <= squared) {
                        offsets.add(new int[]{x, y, z});
                    }
                }
            }
        }
        return offsets;
    }
}
