package com.arch.policy.book.application;

import com.arch.policy.book.domain.BookOrder;

public final class LocalCreateResult {
    private final BookOrder order;
    private final boolean duplicateRequest;

    private LocalCreateResult(BookOrder order, boolean duplicateRequest) {
        this.order = order;
        this.duplicateRequest = duplicateRequest;
    }

    public static LocalCreateResult created(BookOrder order) {
        return new LocalCreateResult(order, false);
    }

    public static LocalCreateResult duplicate(BookOrder order) {
        return new LocalCreateResult(order, true);
    }

    public BookOrder getOrder() { return order; }
    public boolean isDuplicateRequest() { return duplicateRequest; }
}
