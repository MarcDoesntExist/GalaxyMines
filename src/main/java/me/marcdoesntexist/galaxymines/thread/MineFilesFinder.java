package me.marcdoesntexist.galaxymines.thread;

import me.marcdoesntexist.galaxymines.GalaxyMines;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;





public final class MineFilesFinder {
    private final GalaxyMines plugin;

    public MineFilesFinder(final GalaxyMines plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Result> find() {
        final CompletableFuture<Result> future = new CompletableFuture<>();
        this.plugin.getServer().getAsyncScheduler().runNow(this.plugin, task -> {
            final long startedAt = System.currentTimeMillis();
            final List<FileConfiguration> files = new ArrayList<>();
            final List<String> notLoadedFiles = new ArrayList<>();
            final File[] mineFiles = GalaxyMines.minesFolder == null
                    ? null : GalaxyMines.minesFolder.listFiles();
            if (mineFiles == null) {
                future.complete(new Result(List.of(), List.of(),
                        System.currentTimeMillis() - startedAt));
                return;
            }
            for (final File file : mineFiles) {
                if (!file.isFile()) {
                    continue;
                }
                final String filename = file.getName();
                final int dot = filename.lastIndexOf('.');
                if (dot < 0 || !"yml".equalsIgnoreCase(filename.substring(dot + 1))) {
                    this.plugin.debug(filename + " isn't a .yml file! Skipping.", true);
                    continue;
                }
                try {
                    files.add(YamlConfiguration.loadConfiguration(file));
                } catch (RuntimeException ex) {
                    this.plugin.debug("Could not read " + filename
                            + " while searching for mine files.", true);
                    notLoadedFiles.add("<red>" + filename);
                }
            }
            future.complete(new Result(List.copyOf(files), List.copyOf(notLoadedFiles),
                    System.currentTimeMillis() - startedAt));
        });
        return future;
    }

    public record Result(List<FileConfiguration> files, List<String> notLoadedFiles,
                         long elapsedMillis) {
    }
}
