package com.arch.policy.search.domain.snapshot;

import java.util.concurrent.atomic.AtomicBoolean;

/** Atomic activation plus draining of queries that still hold the old version. */
public final class ActiveSnapshotRegistry implements AutoCloseable {
    private Entry active;

    public synchronized boolean isReady() { return active != null; }

    public synchronized SnapshotLease acquire() {
        if (active == null) throw new IllegalStateException("policy snapshot is not ready");
        active.references++;
        return new SnapshotLease(active);
    }

    public synchronized void activate(PolicySnapshot snapshot) {
        Entry previous = active;
        active = new Entry(snapshot);
        if (previous != null) retire(previous);
    }

    @Override public synchronized void close() {
        Entry previous = active;
        active = null;
        if (previous != null) retire(previous);
    }

    private void release(Entry entry) {
        synchronized (this) {
            entry.references--;
            closeWhenDrained(entry);
        }
    }

    private void retire(Entry entry) {
        entry.retired = true;
        closeWhenDrained(entry);
    }

    private void closeWhenDrained(Entry entry) {
        if (entry.retired && entry.references == 0) entry.snapshot.closeAndDelete();
    }

    private static final class Entry {
        private final PolicySnapshot snapshot;
        private int references;
        private boolean retired;
        private Entry(PolicySnapshot snapshot) { this.snapshot = snapshot; }
    }

    public final class SnapshotLease implements AutoCloseable {
        private final Entry entry;
        private final AtomicBoolean released = new AtomicBoolean();
        private SnapshotLease(Entry entry) { this.entry = entry; }
        public PolicySnapshot snapshot() { return entry.snapshot; }
        @Override public void close() {
            if (released.compareAndSet(false, true)) release(entry);
        }
    }
}
