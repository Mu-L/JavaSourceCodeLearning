package com.arch.policy.book.application;

import com.arch.policy.book.domain.BookOrder;

import java.util.List;

public interface BookOrderStore {
    BookOrder findByOrderNo(String orderNo);

    BookOrder findByRequestId(String requestId);

    BookOrder findByEventId(String eventId);

    LocalCreateResult createOrder(BookOrder order, OrderCreationOutboxMessage outboxMessage);

    List<OrderCreationOutboxMessage> findUnpublishedCreationOutbox(int limit);

    void markCreationOutboxPublished(String messageId);

    TransitionCommitResult transit(BookOrder order, long expectedVersion,
                                   OrderStateHistory history, OrderOutboxMessage outboxMessage);
}
