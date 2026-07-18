package com.arch.policy;

import java.nio.file.Path;

public final class SnapshotPorts {
    private SnapshotPorts() {}

    public interface FullPolicyLoader {
        /** Returns the message position corresponding to the full-data cut. */
        MessagePosition loadInto(PolicySnapshot target) throws Exception;
    }

    public interface IncrementalReplayer {
        /** Replays (position, latest] and returns the actual replayed position. */
        MessagePosition replayInto(PolicySnapshot target, MessagePosition position,
                                   MessagePosition latest) throws Exception;
        MessagePosition latestPosition() throws Exception;
    }

    public interface SnapshotValidator {
        void validate(PolicySnapshot candidate, MessagePosition expectedPosition) throws Exception;
    }

    public interface SnapshotDirectory {
        Path create(String version) throws Exception;
        void delete(Path directory) throws Exception;
    }
}
