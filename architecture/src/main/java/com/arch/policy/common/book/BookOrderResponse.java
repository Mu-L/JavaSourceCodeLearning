package com.arch.policy.common.book;

import java.io.Serializable;

public final class BookOrderResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String orderNo;
    private final String state;
    private final long version;

    public BookOrderResponse(String orderNo, String state, long version) {
        this.orderNo = orderNo;
        this.state = state;
        this.version = version;
    }

    public String getOrderNo() { return orderNo; }
    public String getState() { return state; }
    public long getVersion() { return version; }
}
