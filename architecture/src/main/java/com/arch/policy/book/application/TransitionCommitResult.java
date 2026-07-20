package com.arch.policy.book.application;

import com.arch.policy.book.domain.BookOrder;

public final class TransitionCommitResult {
    private final BookOrder order;
    private final boolean duplicateEvent;

    private TransitionCommitResult(BookOrder order, boolean duplicateEvent) {
        this.order = order;
        this.duplicateEvent = duplicateEvent;
    }

    public static TransitionCommitResult committed(BookOrder order) {
        return new TransitionCommitResult(order, false);
    }

    public static TransitionCommitResult duplicate(BookOrder order) {
        return new TransitionCommitResult(order, true);
    }

    public BookOrder getOrder() { return order; }
    public boolean isDuplicateEvent() { return duplicateEvent; }
}
