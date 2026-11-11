package me.marcdoesntexist.galaxymines.api.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileUtils {
    public static void moveFileToDirectory(final File file, final File folder)
            throws SecurityException, IOException {
        Files.move(file.toPath(), folder.toPath().resolve(file.getName()));
    }
}
