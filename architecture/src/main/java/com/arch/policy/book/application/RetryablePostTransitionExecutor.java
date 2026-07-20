package com.arch.policy.book.application;

import com.arch.policy.book.domain.OrderStateMachine;
import com.arch.policy.book.domain.TransitionExecution;

public final class RetryablePostTransitionExecutor implements PostTransitionExecutor {
    private final FailedPostActionStore failedActionStore;

    public RetryablePostTransitionExecutor(FailedPostActionStore failedActionStore) {
        this.failedActionStore = failedActionStore;
    }

    @Override public void execute(OrderStateMachine stateMachine, TransitionExecution execution) {
        try {
            stateMachine.afterCommit(execution);
        } catch (RuntimeException failure) {
            failedActionStore.record(execution, failure);
        }
    }

    public interface FailedPostActionStore {
        void record(TransitionExecution execution, RuntimeException failure);
    }
}
