package com.arch.policy.book.application;

import com.arch.policy.book.domain.BookOrder;

public interface BookOrderStore {
    BookOrder findByOrderNo(String orderNo);

    BookOrder findByRequestId(String requestId);

    BookOrder findByEventId(String eventId);

    TransitionCommitResult create(BookOrder order, OrderStateHistory history,
                                  OrderOutboxMessage outboxMessage);

    TransitionCommitResult transit(BookOrder order, long expectedVersion,
                                   OrderStateHistory history, OrderOutboxMessage outboxMessage);
}
