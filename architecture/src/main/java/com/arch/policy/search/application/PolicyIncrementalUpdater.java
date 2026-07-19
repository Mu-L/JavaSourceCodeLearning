package com.arch.policy.search.application;

import com.arch.policy.common.model.PolicyChange;
import com.arch.policy.search.domain.snapshot.ActiveSnapshotRegistry;

public final class PolicyIncrementalUpdater {
    private final ActiveSnapshotRegistry registry;

    public PolicyIncrementalUpdater(ActiveSnapshotRegistry registry) { this.registry = registry; }

    public boolean apply(PolicyChange change) throws Exception {
        try (ActiveSnapshotRegistry.SnapshotLease lease = registry.acquire()) {
            return lease.snapshot().apply(change);
        }
    }
}
