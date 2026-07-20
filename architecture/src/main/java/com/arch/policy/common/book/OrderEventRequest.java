package com.arch.policy.common.book;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class OrderEventRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String eventId;
    private String orderNo;
    private String event;
    private String operator;
    private long expectedVersion;
    private Map<String, String> attributes = new HashMap<String, String>();

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public long getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(long expectedVersion) { this.expectedVersion = expectedVersion; }
    public Map<String, String> getAttributes() { return Collections.unmodifiableMap(attributes); }
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes == null
                ? new HashMap<String, String>() : new HashMap<String, String>(attributes);
    }
}
