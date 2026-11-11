package me.marcdoesntexist.galaxymines.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


















public final class ConfigMigration {


    public static final int CURRENT_VERSION = 5;
    private static final String VERSION_KEY = "config-version";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");



    private static final Map<String, String> VERSION_2_KEYS = new LinkedHashMap<>();



    private static final Map<String, String> VERSION_3_KEYS = new LinkedHashMap<>();



    private static final Map<String, String> VERSION_4_KEYS = new LinkedHashMap<>();



    private static final Map<String, String> VERSION_5_KEYS = new LinkedHashMap<>();

    static {
        VERSION_2_KEYS.put("settings.reset.globalMillisecondsPerTick", "6.0");
        VERSION_2_KEYS.put("settings.infinite.maxRespawnsPerTick", "128");
        VERSION_2_KEYS.put("settings.timings.enabled", "true");
        VERSION_2_KEYS.put("settings.timings.sampleSize", "512");
        VERSION_3_KEYS.put("settings.reset.commands.enabled", "true");
        VERSION_4_KEYS.put("economy.sellPriority", "PROVIDER_FIRST");
        VERSION_5_KEYS.put("integrations.multipliers.enabled", "true");
        VERSION_5_KEYS.put("integrations.multipliers.drop-cap", "3.0");
        VERSION_5_KEYS.put("integrations.multipliers.sell-cap", "3.0");
        VERSION_5_KEYS.put("integrations.multipliers.disabled", "[ ]");
        VERSION_5_KEYS.put("settings.holograms.orphan-cleanup-minutes", "0");
        VERSION_5_KEYS.put("economy.shop-enabled", "false");
    }

    private ConfigMigration() {
    }




    public static FileConfiguration apply(final File file, final FileConfiguration config,
                                          final Logger logger) {
        if (file == null || config == null) {
            return config;
        }
        final int version = Math.max(1, config.getInt(VERSION_KEY, 1));
        if (version >= CURRENT_VERSION) {
            return config;
        }
        try {
            backup(file, logger);
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (version < 2) {
                text = applyVersion2(text, config);
            }
            if (version < 3) {
                text = applyVersion3(text, config);
            }
            if (version < 4) {
                text = applyVersion4(text, config);
            }
            if (version < 5) {
                text = applyVersion5(text, config);
            }
            text = YamlTextPatcher.setTopLevelValue(text, VERSION_KEY,
                    Integer.toString(CURRENT_VERSION));
            Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
            if (logger != null) {
                logger.info("Migrated config.yml from version " + version + " to "
                        + CURRENT_VERSION + "; the previous file is kept next to it.");
            }
            return YamlConfiguration.loadConfiguration(file);
        } catch (IOException | RuntimeException ex) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Could not migrate config.yml; using in-memory "
                        + "defaults for the new keys. The file was not modified beyond the "
                        + "backup.", ex);
            }
            return config;
        }
    }



    static String applyVersion2(final String text, final FileConfiguration config) {
        String result = text;
        for (final Map.Entry<String, String> entry : VERSION_2_KEYS.entrySet()) {
            if (config.contains(entry.getKey())) {
                continue;
            }
            result = YamlTextPatcher.insertMissingLeaf(result, entry.getKey(), entry.getValue());
        }
        return result;
    }



    static String applyVersion5(final String text, final FileConfiguration config) {
        String result = text;
        for (final Map.Entry<String, String> entry : VERSION_5_KEYS.entrySet()) {
            if (config.contains(entry.getKey())) {
                continue;
            }
            result = YamlTextPatcher.insertMissingLeaf(result, entry.getKey(), entry.getValue());
        }
        return result;
    }



    static String applyVersion4(final String text, final FileConfiguration config) {
        String result = text;
        for (final Map.Entry<String, String> entry : VERSION_4_KEYS.entrySet()) {
            if (config.contains(entry.getKey())) {
                continue;
            }
            result = YamlTextPatcher.insertMissingLeaf(result, entry.getKey(), entry.getValue());
        }
        return result;
    }



    static String applyVersion3(final String text, final FileConfiguration config) {
        String result = text;
        for (final Map.Entry<String, String> entry : VERSION_3_KEYS.entrySet()) {
            if (config.contains(entry.getKey())) {
                continue;
            }
            result = YamlTextPatcher.insertMissingLeaf(result, entry.getKey(), entry.getValue());
        }
        return result;
    }



    static Path backup(final File file, final Logger logger) throws IOException {
        final Path source = file.toPath();
        final Path backup = source.resolveSibling(
                file.getName() + ".bak-" + LocalDateTime.now().format(STAMP));
        Files.copy(source, backup, StandardCopyOption.COPY_ATTRIBUTES);
        if (logger != null) {
            logger.info("config.yml backup created: " + backup.getFileName());
        }
        return backup;
    }
}
