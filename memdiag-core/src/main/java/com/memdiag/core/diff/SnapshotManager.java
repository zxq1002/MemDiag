package com.memdiag.core.diff;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Manager for saving, loading, and listing heap snapshots.
 * Provides persistent storage for Snapshot objects using Java serialization.
 */
public class SnapshotManager {

    private static final String SNAPSHOT_DIR = ".memdiag/snapshots";
    private static final String SNAPSHOT_EXT = ".snapshot";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private final Path snapshotDir;

    public SnapshotManager() {
        this(Paths.get(System.getProperty("user.home"), SNAPSHOT_DIR));
    }

    public SnapshotManager(Path snapshotDir) {
        this.snapshotDir = snapshotDir;
        ensureSnapshotDir();
    }

    private void ensureSnapshotDir() {
        try {
            Files.createDirectories(snapshotDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create snapshot directory: " + snapshotDir, e);
        }
    }

    /**
     * Save a snapshot to persistent storage.
     *
     * @param snapshot The snapshot to save
     * @return The path where the snapshot was saved
     */
    public Path saveSnapshot(Snapshot snapshot) {
        String filename = generateFilename(snapshot);
        Path snapshotPath = snapshotDir.resolve(filename);

        try (FileOutputStream fos = new FileOutputStream(snapshotPath.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(snapshot);
            return snapshotPath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save snapshot to: " + snapshotPath, e);
        }
    }

    /**
     * Load a snapshot from persistent storage.
     *
     * @param path The path to the snapshot file
     * @return The loaded snapshot
     */
    public Snapshot loadSnapshot(Path path) {
        try (FileInputStream fis = new FileInputStream(path.toFile());
             BufferedInputStream bis = new BufferedInputStream(fis);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Snapshot) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to load snapshot from: " + path, e);
        }
    }

    /**
     * Load a snapshot by ID or filename.
     *
     * @param idOrFilename The snapshot ID or full filename
     * @return The loaded snapshot, or null if not found
     */
    public Snapshot loadSnapshot(String idOrFilename) {
        Path path = findSnapshot(idOrFilename);
        if (path == null) {
            return null;
        }
        return loadSnapshot(path);
    }

    /**
     * Find a snapshot file by ID or filename.
     *
     * @param idOrFilename The snapshot ID or full filename
     * @return The path to the snapshot, or null if not found
     */
    public Path findSnapshot(String idOrFilename) {
        // First, try as a full path
        Path directPath = Paths.get(idOrFilename);
        if (Files.exists(directPath)) {
            return directPath;
        }

        // Try in snapshot directory with exact filename
        Path inDir = snapshotDir.resolve(idOrFilename);
        if (Files.exists(inDir)) {
            return inDir;
        }

        // Try with extension
        Path withExt = snapshotDir.resolve(idOrFilename + SNAPSHOT_EXT);
        if (Files.exists(withExt)) {
            return withExt;
        }

        // Try matching by ID prefix
        try (Stream<Path> paths = Files.list(snapshotDir)) {
            return paths
                .filter(p -> p.getFileName().toString().startsWith(idOrFilename))
                .filter(p -> p.getFileName().toString().endsWith(SNAPSHOT_EXT))
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * List all available snapshots.
     *
     * @return List of snapshot metadata
     */
    public List<SnapshotInfo> listSnapshots() {
        List<SnapshotInfo> snapshots = new ArrayList<>();

        try (Stream<Path> paths = Files.list(snapshotDir)) {
            paths
                .filter(p -> p.getFileName().toString().endsWith(SNAPSHOT_EXT))
                .forEach(path -> {
                    try {
                        SnapshotInfo info = new SnapshotInfo();
                        info.path = path;
                        info.filename = path.getFileName().toString();
                        info.size = Files.size(path);
                        info.lastModified = Files.getLastModifiedTime(path).toInstant();

                        // Try to extract ID from filename
                        String filename = info.filename;
                        if (filename.startsWith("snapshot-")) {
                            int endIdx = filename.indexOf('-', 9);
                            if (endIdx > 0) {
                                info.id = filename.substring(9, endIdx);
                            }
                        }

                        snapshots.add(info);
                    } catch (IOException e) {
                        // Skip this snapshot
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list snapshots", e);
        }

        // Sort by timestamp, newest first
        snapshots.sort(Comparator.comparing((SnapshotInfo s) -> s.lastModified).reversed());
        return snapshots;
    }

    /**
     * Delete a snapshot.
     *
     * @param path The path to the snapshot
     * @return true if deleted
     */
    public boolean deleteSnapshot(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Delete all snapshots.
     *
     * @return Number of snapshots deleted
     */
    public int deleteAllSnapshots() {
        int count = 0;
        try (Stream<Path> paths = Files.list(snapshotDir)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (path.getFileName().toString().endsWith(SNAPSHOT_EXT)) {
                    if (deleteSnapshot(path)) {
                        count++;
                    }
                }
            }
        } catch (IOException e) {
            // Ignore
        }
        return count;
    }

    private String generateFilename(Snapshot snapshot) {
        String id = snapshot.getId() != null ? snapshot.getId() : UUID.randomUUID().toString().substring(0, 8);
        String timestamp = TIMESTAMP_FORMATTER.format(snapshot.getTimestamp() != null ? snapshot.getTimestamp() : Instant.now());
        return "snapshot-" + id + "-" + timestamp + SNAPSHOT_EXT;
    }

    /**
     * Metadata about a saved snapshot.
     */
    public static class SnapshotInfo {
        public Path path;
        public String filename;
        public String id;
        public long size;
        public Instant lastModified;

        @Override
        public String toString() {
            return filename;
        }
    }
}
