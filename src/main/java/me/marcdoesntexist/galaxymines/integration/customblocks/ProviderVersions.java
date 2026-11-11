package me.marcdoesntexist.galaxymines.integration.customblocks;

import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public final class ProviderVersions {


    public static final List<Requirement> TESTED_MINIMUMS = List.of(
            new Requirement("Nexo", "1.29.1"),
            new Requirement("Oraxen", "1.220.1"),
            new Requirement("ItemsAdder", "4.0.18"));

    private ProviderVersions() {
    }


    public static void verify(final Plugin owner) {
        if (owner == null) {
            return;
        }
        for (final Requirement requirement : TESTED_MINIMUMS) {
            final Plugin provider =
                    owner.getServer().getPluginManager().getPlugin(requirement.pluginName());
            if (provider == null || !provider.isEnabled()) {
                continue;
            }
            final String installed = provider.getDescription().getVersion();
            if (isOlder(installed, requirement.minimum())) {
                owner.getLogger().warning(warningMessage(requirement, installed));
            }
        }
    }


    public static List<Status> status(final Plugin owner) {
        final List<Status> statuses = new ArrayList<>();
        for (final Requirement requirement : TESTED_MINIMUMS) {
            final Plugin provider = owner == null ? null
                    : owner.getServer().getPluginManager().getPlugin(requirement.pluginName());
            final String namespace = requirement.pluginName().toLowerCase(Locale.ROOT);
            if (provider == null) {
                statuses.add(new Status(namespace, requirement.pluginName(), "",
                        requirement.minimum(), false, false, false));
                continue;
            }
            final String installed = provider.getDescription().getVersion();
            statuses.add(new Status(namespace, requirement.pluginName(), installed,
                    requirement.minimum(), true, provider.isEnabled(),
                    isOlder(installed, requirement.minimum())));
        }
        return List.copyOf(statuses);
    }


    public static String warningMessage(final Requirement requirement,
                                        final String installed) {
        return requirement.pluginName() + " " + installed
                + " is older than the tested minimum " + requirement.minimum()
                + "; custom block and item lookups may fail. Update the provider or verify it "
                + "before reporting an issue.";
    }


    public static boolean isOlder(final String installed, final String minimum) {
        final int[] left = parse(installed);
        final int[] right = parse(minimum);
        return left != null && right != null && compare(left, right) < 0;
    }


    public static int compare(final String left, final String right) {
        final int[] first = parse(left);
        final int[] second = parse(right);
        if (first == null || second == null) {
            return 0;
        }
        return compare(first, second);
    }

    private static int compare(final int[] left, final int[] right) {
        final int length = Math.max(left.length, right.length);
        for (int index = 0; index < length; ++index) {
            final int first = index < left.length ? left[index] : 0;
            final int second = index < right.length ? right[index] : 0;
            if (first != second) {
                return Integer.compare(first, second);
            }
        }
        return 0;
    }

    private static int[] parse(final String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        while (!value.isEmpty() && (value.charAt(0) == 'v' || value.charAt(0) == 'V')) {
            value = value.substring(1);
        }
        value = value.substring(0, firstSuffix(value));
        if (value.isEmpty()) {
            return null;
        }
        final String[] parts = value.split("\\.");
        final int[] numbers = new int[parts.length];
        boolean anyNumber = false;
        for (int index = 0; index < parts.length; ++index) {
            numbers[index] = leadingNumber(parts[index]);
            anyNumber |= numbers[index] > 0;
        }
        return anyNumber ? numbers : null;
    }

    private static int firstSuffix(final String value) {
        for (int index = 0; index < value.length(); ++index) {
            final char character = value.charAt(index);
            if (character == '-' || character == '+') {
                return index;
            }
        }
        return value.length();
    }

    private static int leadingNumber(final String part) {
        int index = 0;
        while (index < part.length() && Character.isDigit(part.charAt(index))) {
            ++index;
        }
        if (index == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(part.substring(0, index));
        } catch (final NumberFormatException ignored) {
            return 0;
        }
    }

    public record Status(String namespace, String pluginName, String installedVersion,
                         String minimum, boolean available, boolean enabled, boolean outdated) {
    }

    public record Requirement(String pluginName, String minimum) {
        public Requirement {
            pluginName = pluginName == null ? "" : pluginName.trim();
            minimum = minimum == null ? "" : minimum.trim();
        }
    }
}
