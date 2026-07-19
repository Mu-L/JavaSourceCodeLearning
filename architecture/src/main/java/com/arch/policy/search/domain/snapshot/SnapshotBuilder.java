package com.arch.policy.search.domain.snapshot;

import com.arch.policy.common.model.MessagePosition;

import org.rocksdb.RocksDBException;

import java.nio.file.Path;

import static com.arch.policy.search.domain.snapshot.SnapshotPorts.*;

/** Builds a candidate without exposing it to queries. */
public final class SnapshotBuilder {
    private final FullPolicyLoader fullLoader;
    private final IncrementalReplayer replayer;
    private final SnapshotValidator validator;
    private final SnapshotDirectory directories;

    public SnapshotBuilder(FullPolicyLoader fullLoader, IncrementalReplayer replayer,
                           SnapshotValidator validator, SnapshotDirectory directories) {
        this.fullLoader = fullLoader;
        this.replayer = replayer;
        this.validator = validator;
        this.directories = directories;
    }

    public PolicySnapshot build(String version) throws Exception {
        Path directory = directories.create(version);
        PolicySnapshot candidate = null;
        try {
            candidate = open(version, directory);
            MessagePosition position = fullLoader.loadInto(candidate);
            candidate.advanceTo(position);
            catchUp(candidate);
            validator.validate(candidate, candidate.getPosition());
            return candidate;
        } catch (Exception failure) {
            if (candidate != null) candidate.close();
            directories.delete(directory);
            throw failure;
        }
    }

    private void catchUp(PolicySnapshot candidate) throws Exception {
        while (true) {
            MessagePosition target = replayer.latestPosition();
            if (candidate.getPosition().compareTo(target) >= 0) return;
            MessagePosition replayed = replayer.replayInto(candidate, candidate.getPosition(), target);
            if (replayed.compareTo(candidate.getPosition()) <= 0 || replayed.compareTo(target) > 0) {
                throw new IllegalStateException("incremental replay made invalid progress");
            }
            candidate.advanceTo(replayed);
        }
    }

    private PolicySnapshot open(String version, Path directory) throws RocksDBException {
        return new PolicySnapshot(version, directory);
    }
}
