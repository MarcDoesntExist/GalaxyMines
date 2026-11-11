package me.marcdoesntexist.galaxymines.luckyblocks;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;


public record LuckyBlockTable(String name, int rolls, List<Reward> rewards) {
    public LuckyBlockTable {
        name = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        rolls = Math.max(1, Math.min(10, rolls));
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
    }

    public boolean isEmpty() {
        return this.rewards.isEmpty();
    }

    public enum Type {
        ITEM,
        COMMAND,
        MESSAGE,
        NOTHING
    }


    public record Reward(Type type, Material material, int minAmount, int maxAmount,
                         double chance, String command, String message, String providerId) {
        public Reward {
            type = type == null ? Type.NOTHING : type;
            minAmount = Math.max(1, Math.min(64, minAmount));
            maxAmount = Math.max(minAmount, Math.min(64, maxAmount));
            chance = Double.isFinite(chance)
                    ? Math.max(0.0D, Math.min(100.0D, chance))
                    : 0.0D;
            command = command == null ? "" : command.trim();
            message = message == null ? "" : message;
            providerId = providerId == null || providerId.isBlank()
                    ? null : providerId.trim().toLowerCase(Locale.ROOT);
            if (type == Type.ITEM && providerId == null
                    && (material == null || isAir(material))) {
                type = Type.NOTHING;
            }
            if (type == Type.COMMAND && command.isEmpty()) {
                type = Type.NOTHING;
            }
            if (type == Type.MESSAGE && message.isBlank()) {
                type = Type.NOTHING;
            }
        }

        private static boolean isAir(final Material material) {
            return material == Material.AIR || material == Material.CAVE_AIR
                    || material == Material.VOID_AIR;
        }

        public String label() {
            return switch (this.type) {
                case ITEM -> this.material == null
                        ? "item" : this.material.name().toLowerCase(Locale.ROOT);
                case COMMAND -> "command";
                case MESSAGE -> "message";
                case NOTHING -> "nothing";
            };
        }
    }
}
