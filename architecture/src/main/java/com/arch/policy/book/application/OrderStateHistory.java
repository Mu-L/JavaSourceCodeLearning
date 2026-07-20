package com.arch.policy.book.application;

import com.arch.policy.book.domain.OrderEvent;
import com.arch.policy.book.domain.OrderState;

public final class OrderStateHistory {
    private final String eventId;
    private final String orderNo;
    private final OrderState from;
    private final OrderEvent event;
    private final OrderState to;
    private final String operator;
    private final long occurredAtMillis;

    public OrderStateHistory(String eventId, String orderNo, OrderState from, OrderEvent event,
                             OrderState to, String operator, long occurredAtMillis) {
        this.eventId = eventId;
        this.orderNo = orderNo;
        this.from = from;
        this.event = event;
        this.to = to;
        this.operator = operator;
        this.occurredAtMillis = occurredAtMillis;
    }

    public String getEventId() { return eventId; }
    public String getOrderNo() { return orderNo; }
    public OrderState getFrom() { return from; }
    public OrderEvent getEvent() { return event; }
    public OrderState getTo() { return to; }
    public String getOperator() { return operator; }
    public long getOccurredAtMillis() { return occurredAtMillis; }
}
