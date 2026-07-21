package com.arch.policy.book.application;

import java.util.List;

/** 以至少一次语义发布 Outbox；OrderCreationSaga 必须使用 orderNo 幂等启动。 */
public final class OrderCreationOutboxPublisher {
    private final BookOrderStore store;
    private final OrderCreationSaga creationSaga;

    public OrderCreationOutboxPublisher(BookOrderStore store, OrderCreationSaga creationSaga) {
        this.store = store;
        this.creationSaga = creationSaga;
    }

    public void publish(OrderCreationOutboxMessage message) {
        if (message.isPublished()) return;
        creationSaga.start(message.getOrderNo(), message.getPromotionId());
        store.markCreationOutboxPublished(message.getMessageId());
    }

    public void publishPending(int limit) {
        List<OrderCreationOutboxMessage> messages = store.findUnpublishedCreationOutbox(limit);
        for (OrderCreationOutboxMessage message : messages) publish(message);
    }
}
