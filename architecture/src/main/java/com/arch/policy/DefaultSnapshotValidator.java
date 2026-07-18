package com.arch.policy;

import static com.arch.policy.SnapshotPorts.SnapshotValidator;

/** Baseline invariants; domain-specific checks can be supplied through SnapshotValidator. */
public final class DefaultSnapshotValidator implements SnapshotValidator {
    @Override public void validate(PolicySnapshot candidate, MessagePosition expectedPosition) throws Exception {
        if (!candidate.getPosition().equals(expectedPosition)) {
            throw new IllegalStateException("message position mismatch");
        }
        if (candidate.policyCount() != candidate.indexedPolicyCount()) {
            throw new IllegalStateException("RocksDB and bitmap policy counts differ");
        }
    }
}
