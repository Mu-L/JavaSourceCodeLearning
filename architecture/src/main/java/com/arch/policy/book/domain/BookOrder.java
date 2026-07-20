package com.arch.policy.book.domain;

import java.math.BigDecimal;

public final class BookOrder {
    private final String orderNo;
    private final String requestId;
    private final String customerId;
    private final String productId;
    private final int quantity;
    private final BigDecimal amount;
    private OrderState state;
    private long version;

    private BookOrder(String orderNo, String requestId, String customerId, String productId,
                      int quantity, BigDecimal amount, OrderState state, long version) {
        this.orderNo = orderNo;
        this.requestId = requestId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        this.state = state;
        this.version = version;
    }

    public static BookOrder create(String orderNo, String requestId, String customerId,
                                   String productId, int quantity, BigDecimal amount) {
        return new BookOrder(orderNo, requestId, customerId, productId, quantity, amount,
                OrderState.CREATE, 0L);
    }

    public BookOrder copy() {
        return new BookOrder(orderNo, requestId, customerId, productId, quantity, amount, state, version);
    }

    void applyState(OrderState target) {
        state = target;
        version++;
    }

    public String getOrderNo() { return orderNo; }
    public String getRequestId() { return requestId; }
    public String getCustomerId() { return customerId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getAmount() { return amount; }
    public OrderState getState() { return state; }
    public long getVersion() { return version; }
}
