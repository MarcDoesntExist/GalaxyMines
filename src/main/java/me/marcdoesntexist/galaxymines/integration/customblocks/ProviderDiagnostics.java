package me.marcdoesntexist.galaxymines.integration.customblocks;

import me.marcdoesntexist.galaxymines.events.MineEventDefinition;
import me.marcdoesntexist.galaxymines.luckyblocks.LuckyBlockTable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


public final class ProviderDiagnostics {
    private ProviderDiagnostics() {
    }


    public static List<String> unresolvedEvents(
            final Map<String, MineEventDefinition> definitions,
            final Predicate<String> resolvable) {
        final Set<String> unresolved = new LinkedHashSet<>();
        if (definitions == null || resolvable == null) {
            return List.of();
        }
        for (final Map.Entry<String, MineEventDefinition> entry : definitions.entrySet()) {
            for (final MineEventDefinition.RareDrop drop
                    : entry.getValue().actions().rareDrops()) {
                final String providerId = drop.providerId();
                if (providerId != null && !resolvable.test(providerId)) {
                    unresolved.add(entry.getKey() + " -> " + providerId);
                }
            }
        }
        return List.copyOf(unresolved);
    }


    public static List<String> unresolvedLuckyBlocks(
            final Map<String, LuckyBlockTable> tables,
            final Predicate<String> resolvable) {
        final Set<String> unresolved = new LinkedHashSet<>();
        if (tables == null || resolvable == null) {
            return List.of();
        }
        for (final Map.Entry<String, LuckyBlockTable> entry : tables.entrySet()) {
            for (final LuckyBlockTable.Reward reward : entry.getValue().rewards()) {
                final String providerId = reward.providerId();
                if (providerId != null && !resolvable.test(providerId)) {
                    unresolved.add(entry.getKey() + " -> " + providerId);
                }
            }
        }
        return List.copyOf(unresolved);
    }
}
