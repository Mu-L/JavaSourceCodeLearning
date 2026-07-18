package com.arch.policy.application;

import com.arch.policy.ActiveSnapshotRegistry;
import com.arch.policy.PolicyChange;

public final class PolicyIncrementalUpdater {
    private final ActiveSnapshotRegistry registry;

    public PolicyIncrementalUpdater(ActiveSnapshotRegistry registry) { this.registry = registry; }

    public boolean apply(PolicyChange change) throws Exception {
        try (ActiveSnapshotRegistry.SnapshotLease lease = registry.acquire()) {
            return lease.snapshot().apply(change);
        }
    }
}
