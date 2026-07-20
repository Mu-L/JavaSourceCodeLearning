package com.arch.policy.book.infrastructure.repository;

import com.arch.policy.book.application.RetryablePostTransitionExecutor.FailedPostActionStore;
import com.arch.policy.book.domain.TransitionExecution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InMemoryFailedPostActionStore implements FailedPostActionStore {
    private final List<FailedPostAction> failures = new ArrayList<FailedPostAction>();

    @Override public synchronized void record(TransitionExecution execution, RuntimeException failure) {
        failures.add(new FailedPostAction(execution.getContext().getEventId(),
                execution.getContext().getOrder().getOrderNo(), failure.getMessage()));
    }

    public synchronized List<FailedPostAction> all() {
        return Collections.unmodifiableList(new ArrayList<FailedPostAction>(failures));
    }

    public static final class FailedPostAction {
        private final String eventId;
        private final String orderNo;
        private final String reason;

        public FailedPostAction(String eventId, String orderNo, String reason) {
            this.eventId = eventId;
            this.orderNo = orderNo;
            this.reason = reason;
        }

        public String getEventId() { return eventId; }
        public String getOrderNo() { return orderNo; }
        public String getReason() { return reason; }
    }
}
