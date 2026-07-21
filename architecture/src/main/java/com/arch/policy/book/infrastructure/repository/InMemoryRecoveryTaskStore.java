package com.arch.policy.book.infrastructure.repository;

import com.arch.policy.book.application.RecoveryTask;
import com.arch.policy.book.application.RecoveryTaskStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryRecoveryTaskStore implements RecoveryTaskStore {
    private final Map<String, RecoveryTask> tasks = new LinkedHashMap<String, RecoveryTask>();

    @Override public synchronized RecoveryTask saveIfAbsent(RecoveryTask task) {
        RecoveryTask existing = tasks.get(task.getUniqueKey());
        if (existing != null) return existing.copy();
        tasks.put(task.getUniqueKey(), task.copy());
        return task.copy();
    }

    @Override public synchronized List<RecoveryTask> findExecutable(long nowMillis, int limit) {
        List<RecoveryTask> result = new ArrayList<RecoveryTask>();
        for (RecoveryTask task : tasks.values()) {
            if (result.size() >= limit) break;
            if ((task.getStatus() == RecoveryTask.RecoveryStatus.PENDING
                    || task.getStatus() == RecoveryTask.RecoveryStatus.RETRYING)
                    && task.getNextAttemptAtMillis() <= nowMillis) result.add(task.copy());
        }
        return result;
    }

    @Override public synchronized void save(RecoveryTask task) {
        if (!tasks.containsKey(task.getUniqueKey())) {
            throw new IllegalArgumentException("recovery task not found: " + task.getUniqueKey());
        }
        tasks.put(task.getUniqueKey(), task.copy());
    }

    public synchronized int size() { return tasks.size(); }

    public synchronized RecoveryTask find(String uniqueKey) {
        RecoveryTask task = tasks.get(uniqueKey);
        return task == null ? null : task.copy();
    }
}
