package com.arch.policy.book.application;

import com.arch.policy.book.domain.OrderEvent;
import com.arch.policy.book.domain.OrderState;

public final class OrderOutboxMessage {
    private final String eventId;
    private final String orderNo;
    private final OrderEvent event;
    private final OrderState state;
    private final long orderVersion;
    private boolean published;

    public OrderOutboxMessage(String eventId, String orderNo, OrderEvent event,
                              OrderState state, long orderVersion) {
        this.eventId = eventId;
        this.orderNo = orderNo;
        this.event = event;
        this.state = state;
        this.orderVersion = orderVersion;
    }

    public String getEventId() { return eventId; }
    public String getOrderNo() { return orderNo; }
    public OrderEvent getEvent() { return event; }
    public OrderState getState() { return state; }
    public long getOrderVersion() { return orderVersion; }
    public boolean isPublished() { return published; }
    public void markPublished() { published = true; }
}
