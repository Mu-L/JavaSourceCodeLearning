package com.arch.policy.search.domain.snapshot;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Coordinates startup retry and side-by-side runtime refresh. */
public final class PolicySnapshotService implements AutoCloseable {
    private final SnapshotBuilder builder;
    private final ActiveSnapshotRegistry registry;
    private final ScheduledExecutorService executor;
    private final long retryDelayMillis;
    private final AtomicBoolean building = new AtomicBoolean();
    private volatile boolean stopped;

    public PolicySnapshotService(SnapshotBuilder builder, ActiveSnapshotRegistry registry,
                                 ScheduledExecutorService executor, long retryDelayMillis) {
        this.builder = builder;
        this.registry = registry;
        this.executor = executor;
        this.retryDelayMillis = retryDelayMillis;
    }

    /** Keeps readiness false and retries until the first valid snapshot is activated. */
    public void start(String version) { submitBuild(version, true); }

    /** Returns false when another candidate is already being built. */
    public boolean refresh(String version) { return submitBuild(version, false); }

    public boolean isReady() { return registry.isReady(); }
    public boolean isBuilding() { return building.get(); }

    /** Used by application startup: Spring startup does not finish before a snapshot is ready. */
    public void initializeBlocking(String version) throws InterruptedException {
        if (!building.compareAndSet(false, true)) throw new IllegalStateException("snapshot build already running");
        try {
            while (!stopped && !registry.isReady()) {
                try {
                    registry.activate(builder.build(version));
                } catch (Exception failure) {
                    Thread.sleep(retryDelayMillis);
                }
            }
        } finally {
            building.set(false);
        }
    }

    private boolean submitBuild(final String version, final boolean retryOnFailure) {
        if (stopped || !building.compareAndSet(false, true)) return false;
        executor.execute(new Runnable() {
            @Override public void run() { buildAndActivate(version, retryOnFailure); }
        });
        return true;
    }

    private void buildAndActivate(final String version, final boolean retryOnFailure) {
        try {
            PolicySnapshot candidate = builder.build(version);
            if (stopped) candidate.closeAndDelete();
            else registry.activate(candidate);
        } catch (Exception ignored) {
            if (retryOnFailure && !stopped) {
                executor.schedule(new Runnable() {
                    @Override public void run() { buildAndActivate(version, true); }
                }, retryDelayMillis, TimeUnit.MILLISECONDS);
                return;
            }
        } finally {
            // Startup retry owns the build slot until it succeeds or the service stops.
            if (!retryOnFailure || registry.isReady() || stopped) building.set(false);
        }
    }

    @Override public void close() {
        stopped = true;
        registry.close();
        executor.shutdownNow();
    }
}
