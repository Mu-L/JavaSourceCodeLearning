package com.arch.policy.book.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class StateTransitionContext {
    private final String eventId;
    private final OrderEvent event;
    private final BookOrder order;
    private final String operator;
    private final Map<String, String> attributes;

    public StateTransitionContext(String eventId, OrderEvent event, BookOrder order,
                                  String operator, Map<String, String> attributes) {
        this.eventId = eventId;
        this.event = event;
        this.order = order;
        this.operator = operator;
        this.attributes = attributes == null ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, String>(attributes));
    }

    public String getEventId() { return eventId; }
    public OrderEvent getEvent() { return event; }
    public BookOrder getOrder() { return order; }
    public String getOperator() { return operator; }
    public Map<String, String> getAttributes() { return attributes; }
    public String attribute(String name) { return attributes.get(name); }
}
