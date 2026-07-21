package com.arch.policy.book.application;

public interface OrderWorkflowTaskStore {
    void saveIfAbsent(OrderWorkflowTask task);
}
