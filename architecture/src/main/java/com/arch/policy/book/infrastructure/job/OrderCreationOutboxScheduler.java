package com.arch.policy.book.infrastructure.job;

import com.arch.policy.book.application.OrderCreationOutboxPublisher;
import org.springframework.scheduling.annotation.Scheduled;

public final class OrderCreationOutboxScheduler {
    private final OrderCreationOutboxPublisher publisher;

    public OrderCreationOutboxScheduler(OrderCreationOutboxPublisher publisher) {
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${book.creation.outbox-delay-ms:3000}")
    public void publish() { publisher.publishPending(100); }
}
