package me.marcdoesntexist.galaxymines.luckyblocks;

import java.util.Locale;
import java.util.Map;





public record MineLuckyBlocks(String mineName, boolean replaceDrops,
                              Map<String, String> blocks) {
    public MineLuckyBlocks {
        mineName = mineName == null ? "" : mineName.trim().toLowerCase(Locale.ROOT);
        blocks = blocks == null ? Map.of() : Map.copyOf(blocks);
    }
}
