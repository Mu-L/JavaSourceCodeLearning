package com.arch.policy.search.domain.snapshot;

import com.arch.policy.common.model.MessagePosition;
import com.arch.policy.common.model.PolicyChange;
import com.arch.policy.common.model.PolicyRecord;

import org.roaringbitmap.RoaringBitmap;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** A version-isolated RocksDB detail store and its matching bitmap index. */
public final class PolicySnapshot implements AutoCloseable {
    static { RocksDB.loadLibrary(); }

    private final String version;
    private final Path directory;
    private final Options options;
    private final RocksDB database;
    private final Map<String, RoaringBitmap> bitmapIndex = new HashMap<String, RoaringBitmap>();
    private final RoaringBitmap allPolicyIds = new RoaringBitmap();
    private final Map<Integer, Set<String>> termsByPolicy = new HashMap<Integer, Set<String>>();
    private MessagePosition position = MessagePosition.BEGINNING;
    private boolean closed;

    public PolicySnapshot(String version, Path directory) throws RocksDBException {
        this.version = version;
        this.directory = directory;
        this.options = new Options().setCreateIfMissing(true);
        this.database = RocksDB.open(options, directory.toString());
    }

    public synchronized void upsert(PolicyRecord policy) throws RocksDBException {
        ensureOpen();
        removeFromIndex(policy.getId());
        database.put(key(policy.getId()), policy.getDetail());
        allPolicyIds.add(policy.getId());
        termsByPolicy.put(policy.getId(), policy.getIndexTerms());
        for (String term : policy.getIndexTerms()) {
            RoaringBitmap bitmap = bitmapIndex.get(term);
            if (bitmap == null) {
                bitmap = new RoaringBitmap();
                bitmapIndex.put(term, bitmap);
            }
            bitmap.add(policy.getId());
        }
    }

    public synchronized void delete(int policyId) throws RocksDBException {
        ensureOpen();
        database.delete(key(policyId));
        allPolicyIds.remove(policyId);
        removeFromIndex(policyId);
    }

    public synchronized byte[] findDetail(int policyId) throws RocksDBException {
        ensureOpen();
        return database.get(key(policyId));
    }

    public synchronized RoaringBitmap findPolicyIds(String term) {
        ensureOpen();
        RoaringBitmap bitmap = bitmapIndex.get(term);
        return bitmap == null ? new RoaringBitmap() : bitmap.clone();
    }

    public synchronized RoaringBitmap allPolicyIds() {
        ensureOpen();
        return allPolicyIds.clone();
    }

    /** Applies an ordered event and advances its position in the same monitor. */
    public synchronized boolean apply(PolicyChange change) throws RocksDBException {
        ensureOpen();
        if (change.getPosition().compareTo(position) <= 0) return false;
        if (change.getType() == PolicyChange.Type.DELETE) delete(change.getPolicyId());
        else upsert(change.getPolicy());
        advanceTo(change.getPosition());
        return true;
    }

    public synchronized long policyCount() throws RocksDBException {
        ensureOpen();
        long count = 0;
        try (RocksIterator iterator = database.newIterator()) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) count++;
            iterator.status();
        }
        return count;
    }

    public synchronized Map<String, Integer> bitmapCardinalities() {
        ensureOpen();
        Map<String, Integer> result = new HashMap<String, Integer>();
        for (Map.Entry<String, RoaringBitmap> entry : bitmapIndex.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getCardinality());
        }
        return Collections.unmodifiableMap(result);
    }

    public synchronized int indexedPolicyCount() {
        ensureOpen();
        RoaringBitmap all = new RoaringBitmap();
        for (RoaringBitmap bitmap : bitmapIndex.values()) all.or(bitmap);
        return all.getCardinality();
    }

    public String getVersion() { return version; }
    public Path getDirectory() { return directory; }
    public synchronized MessagePosition getPosition() { return position; }
    public synchronized void advanceTo(MessagePosition newPosition) {
        ensureOpen();
        if (newPosition.compareTo(position) < 0) throw new IllegalArgumentException("position cannot move backwards");
        position = newPosition;
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        database.close();
        options.close();
        bitmapIndex.clear();
        allPolicyIds.clear();
        termsByPolicy.clear();
    }

    public void closeAndDelete() {
        close();
        try {
            if (!Files.exists(directory)) return;
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (IOException failure) { throw new DeleteFailure(failure); }
                });
            }
        } catch (IOException failure) {
            throw new IllegalStateException("cannot delete snapshot " + directory, failure);
        } catch (DeleteFailure failure) {
            throw new IllegalStateException("cannot delete snapshot " + directory, failure.getCause());
        }
    }

    private static final class DeleteFailure extends RuntimeException {
        private DeleteFailure(IOException cause) { super(cause); }
    }

    private void removeFromIndex(int policyId) {
        Set<String> oldTerms = termsByPolicy.remove(policyId);
        if (oldTerms == null) return;
        for (String term : oldTerms) {
            RoaringBitmap bitmap = bitmapIndex.get(term);
            bitmap.remove(policyId);
            if (bitmap.isEmpty()) bitmapIndex.remove(term);
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("snapshot is closed: " + version);
    }

    private static byte[] key(int id) { return ByteBuffer.allocate(4).putInt(id).array(); }
}
