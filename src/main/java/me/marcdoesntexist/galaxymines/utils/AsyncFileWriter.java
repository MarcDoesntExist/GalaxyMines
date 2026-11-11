package me.marcdoesntexist.galaxymines.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;





public final class AsyncFileWriter {
    private static final Map<Path, String> PENDING = new ConcurrentHashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "GalaxyMines-IO");
        thread.setDaemon(true);
        return thread;
    });
    private static final int MAX_MOVE_ATTEMPTS = 3;
    private static final long BASE_RETRY_DELAY_MILLIS = 50L;
    private static final long MAX_RETRY_DELAY_MILLIS = 400L;
    private static final MoveStrategy DEFAULT_MOVE_STRATEGY = (source, target, atomic) -> {
        if (atomic) {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    };
    private static final Sleeper DEFAULT_SLEEPER = Thread::sleep;
    private static volatile Logger logger = Logger.getLogger("GalaxyMines");

    private AsyncFileWriter() {
    }

    public static void init(final Logger pluginLogger) {
        AsyncFileWriter.logger = pluginLogger;
    }

    public static void write(final File file, final String content) {
        write(file, content, null);
    }




    public static void write(final File file, final String content, final Runnable onComplete) {
        final Path target = file.toPath().toAbsolutePath().normalize();
        PENDING.put(target, content);
        EXECUTOR.execute(() -> {
            final String latest = PENDING.remove(target);
            if (latest != null) {
                try {
                    writeAtomically(target, latest);
                } catch (IOException ex) {
                    AsyncFileWriter.logger.log(Level.SEVERE, "Could not save " + target, ex);
                }
            }
            if (onComplete != null) {
                try {
                    onComplete.run();
                } catch (RuntimeException ex) {
                    AsyncFileWriter.logger.log(Level.SEVERE,
                            "A file-write callback failed for " + target, ex);
                }
            }
        });
    }

    private static void writeAtomically(final Path target, final String content)
            throws IOException {
        writeAtomically(target, content, DEFAULT_MOVE_STRATEGY, DEFAULT_SLEEPER);
    }





    static void writeAtomically(final Path target, final String content,
                                final MoveStrategy moveStrategy, final Sleeper sleeper)
            throws IOException {
        final Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        final Path temp = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            moveWithRetry(temp, target, moveStrategy, sleeper);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void moveWithRetry(final Path temp, final Path target,
                                      final MoveStrategy moveStrategy, final Sleeper sleeper)
            throws IOException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_MOVE_ATTEMPTS; ++attempt) {
            try {
                moveStrategy.move(temp, target, true);
                return;
            } catch (AtomicMoveNotSupportedException ex) {
                moveStrategy.move(temp, target, false);
                return;
            } catch (FileSystemException ex) {
                if (!isRetryable(ex)) {
                    throw ex;
                }
                lastFailure = ex;
                if (attempt < MAX_MOVE_ATTEMPTS) {
                    sleepBeforeRetry(attempt, sleeper, ex);
                }
            }
        }
        try {
            moveStrategy.move(temp, target, false);
        } catch (IOException fallbackFailure) {
            if (lastFailure != null) {
                fallbackFailure.addSuppressed(lastFailure);
            }
            throw fallbackFailure;
        }
    }

    private static boolean isRetryable(final FileSystemException ex) {
        if (ex instanceof AccessDeniedException) {
            return true;
        }
        final String reason = ex.getReason();
        if (reason == null) {
            return false;
        }
        final String normalized = reason.toLowerCase(Locale.ROOT);
        return normalized.contains("access is denied")
                || normalized.contains("being used by another process")
                || normalized.contains("cannot access the file");
    }

    private static void sleepBeforeRetry(final int attempt, final Sleeper sleeper,
                                         final IOException failure) throws IOException {
        final long delay = Math.min(MAX_RETRY_DELAY_MILLIS,
                BASE_RETRY_DELAY_MILLIS * (1L << (attempt - 1)));
        try {
            sleeper.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            final IOException interrupted = new IOException(
                    "Interrupted while retrying a file move to " + failure.getMessage(), ex);
            interrupted.addSuppressed(failure);
            throw interrupted;
        }
    }





    public static void delete(final File file) {
        final Path target = file.toPath().toAbsolutePath().normalize();
        PENDING.remove(target);
        EXECUTOR.execute(() -> {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ex) {
                AsyncFileWriter.logger.log(Level.WARNING, "Could not delete " + target, ex);
            }
        });
    }



    public static void flush() {
        try {
            EXECUTOR.submit(() -> {
            }).get(10L, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            AsyncFileWriter.logger.log(Level.SEVERE, "Could not complete saves", ex);
        }
    }

    @FunctionalInterface
    interface MoveStrategy {
        void move(Path source, Path target, boolean atomic) throws IOException;
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
