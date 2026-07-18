package com.arch.policy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.arch.policy.SnapshotPorts.SnapshotDirectory;

/** Keeps every version in an isolated directory and removes abandoned candidates. */
public final class FileSystemSnapshotDirectory implements SnapshotDirectory {
    private final Path root;

    public FileSystemSnapshotDirectory(Path root) { this.root = root; }

    @Override public Path create(String version) throws IOException {
        Path directory = root.resolve(safeVersion(version));
        delete(directory);
        return Files.createDirectories(directory);
    }

    @Override public void delete(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (IOException failure) { throw new DeleteFailure(failure); }
            });
        } catch (DeleteFailure failure) {
            throw (IOException) failure.getCause();
        }
    }

    private static String safeVersion(String version) {
        if (version == null || !version.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("invalid snapshot version: " + version);
        }
        return version;
    }

    private static final class DeleteFailure extends RuntimeException {
        private DeleteFailure(IOException cause) { super(cause); }
    }
}
