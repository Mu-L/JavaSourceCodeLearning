package com.arch.policy.book.infrastructure.repository;

import com.arch.policy.book.application.OrderWorkflowTask;
import com.arch.policy.book.application.OrderWorkflowTaskStore;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InMemoryOrderWorkflowTaskStore implements OrderWorkflowTaskStore {
    private final Map<String, OrderWorkflowTask> tasks = new LinkedHashMap<String, OrderWorkflowTask>();

    @Override public synchronized void saveIfAbsent(OrderWorkflowTask task) {
        if (!tasks.containsKey(task.getUniqueKey())) tasks.put(task.getUniqueKey(), task);
    }

    public synchronized int size() { return tasks.size(); }
}
