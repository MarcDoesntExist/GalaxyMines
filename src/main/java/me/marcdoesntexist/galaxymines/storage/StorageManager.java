package me.marcdoesntexist.galaxymines.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import me.marcdoesntexist.galaxymines.GalaxyMines;
import me.marcdoesntexist.galaxymines.mines.Mine;
import me.marcdoesntexist.galaxymines.utils.AsyncFileWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class StorageManager {
    private static final long STATS_CACHE_NANOS = 1_000_000_000L;
    private static final Map<String, CachedStats> STATS_CACHE = new ConcurrentHashMap<>();

    private static File getStorageFile(final Mine m) {
        final String safeName = m.getName().replaceAll("[^A-Za-z0-9._-]", "_");
        return new File(GalaxyMines.storageFolder, safeName + ".mines");
    }

    public static void addReset(final Mine m) throws IOException, JsonParseException {
        final File minestoragefile = getStorageFile(m);
        final JsonObject json = getMineJson(m);
        final long brokenblocks = getLong(json, "brokenblocks");
        final long resets = getLong(json, "timesreseted");
        json.addProperty("brokenblocks", brokenblocks + m.getMinedBlocks());
        json.addProperty("timesreseted", resets + 1);
        final String updatedString = Base64.getEncoder()
                .encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
        AsyncFileWriter.write(minestoragefile, updatedString);
        STATS_CACHE.remove(m.getName());
    }

    public static JsonObject getMineJson(final Mine m) throws IOException, JsonParseException {
        File minestoragefile = getStorageFile(m);
        if (!minestoragefile.exists()) {
            minestoragefile = createNewMineFile(m);
        }
        try {
            return parse(readBase64(minestoragefile));
        } catch (RuntimeException ex) {
            GalaxyMines.instance.printException(
                    "Storage file for mine " + m.getName() + " is corrupted: recreating it.", ex);
            if (minestoragefile.exists() && !minestoragefile.delete()) {
                GalaxyMines.instance.debug(
                        "Could not delete corrupted file " + minestoragefile, true);
            }
            final File recreated = createNewMineFile(m);
            return parse(readBase64(recreated));
        }
    }

    private static JsonObject parse(final String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static String readBase64(final File file) throws IOException {
        final StringBuilder filetext = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                filetext.append(line);
            }
        }
        return new String(Base64.getDecoder().decode(filetext.toString().trim()),
                StandardCharsets.UTF_8);
    }

    private static File createNewMineFile(final Mine m) throws IOException {
        final JsonObject json = new JsonObject();
        json.addProperty("mine", m.getName());
        json.addProperty("brokenblocks", 0);
        json.addProperty("timesreseted", 0);
        final File storagefile = getStorageFile(m);
        final File parent = storagefile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create folder " + parent);
        }
        try (final Writer writer = new OutputStreamWriter(new FileOutputStream(storagefile),
                StandardCharsets.UTF_8)) {
            writer.write(Base64.getEncoder()
                    .encodeToString(json.toString().getBytes(StandardCharsets.UTF_8)));
        }
        return storagefile;
    }

    private static long getLong(final JsonObject json, final String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return 0L;
        }
        try {
            return json.get(key).getAsLong();
        } catch (final RuntimeException ex) {
            return 0L;
        }
    }


    public static MineStats getStats(final Mine m) {
        final String key = m.getName();
        final long now = System.nanoTime();
        final CachedStats cached = STATS_CACHE.get(key);
        if (cached != null && now - cached.loadedAtNanos() < STATS_CACHE_NANOS) {
            return new MineStats(cached.brokenBlocks(), cached.resets());
        }
        try {
            final JsonObject json = getMineJson(m);
            final MineStats stats = new MineStats(getLong(json, "brokenblocks"),
                    getLong(json, "timesreseted"));
            STATS_CACHE.put(key, new CachedStats(stats.brokenBlocks(), stats.resets(), now));
            return stats;
        } catch (Exception e) {
            GalaxyMines.instance.printException(
                    "An error occurred while loading data for mine " + m.getName() + "!", e);
            return new MineStats(0L, 0L);
        }
    }

    public static long getTotalBrokenBlocks(final Mine m) {
        return getStats(m).brokenBlocks();
    }

    public static long getTotalResets(final Mine m) {
        return getStats(m).resets();
    }

    public record MineStats(long brokenBlocks, long resets) {
    }

    private record CachedStats(long brokenBlocks, long resets, long loadedAtNanos) {
    }
}
