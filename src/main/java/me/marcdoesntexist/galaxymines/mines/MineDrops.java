package me.marcdoesntexist.galaxymines.mines;

import java.util.Locale;





public record MineDrops(boolean autoPickup, boolean autoSmelt, boolean autoBlock, boolean autoSell,
                        double dropMultiplier, double sellMultiplier,
                        FullInventoryPolicy fullInventoryPolicy) {

    public static final MineDrops VANILLA = new MineDrops(false, false, false, false,
            1.0D, 1.0D, FullInventoryPolicy.DROP);

    public MineDrops {
        if (!Double.isFinite(dropMultiplier) || dropMultiplier < 0.0D) {
            dropMultiplier = 1.0D;
        }
        if (!Double.isFinite(sellMultiplier) || sellMultiplier < 0.0D) {
            sellMultiplier = 1.0D;
        }
        if (fullInventoryPolicy == null) {
            fullInventoryPolicy = FullInventoryPolicy.DROP;
        }
    }

    public static FullInventoryPolicy parsePolicy(final String raw) {
        if (raw == null || raw.isBlank()) {
            return FullInventoryPolicy.DROP;
        }
        try {
            return FullInventoryPolicy.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return FullInventoryPolicy.DROP;
        }
    }




    public boolean requiresCustomHandling() {
        return autoPickup || autoSmelt || autoBlock || autoSell
                || dropMultiplier != 1.0D || fullInventoryPolicy != FullInventoryPolicy.DROP;
    }

    public enum FullInventoryPolicy {
        DROP,
        VOID,
        CANCEL_BREAK
    }
}
