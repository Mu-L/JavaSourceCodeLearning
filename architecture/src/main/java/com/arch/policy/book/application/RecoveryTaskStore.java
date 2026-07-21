package com.arch.policy.book.application;

import java.util.List;

public interface RecoveryTaskStore {
    RecoveryTask saveIfAbsent(RecoveryTask task);

    List<RecoveryTask> findExecutable(long nowMillis, int limit);

    void save(RecoveryTask task);
}
