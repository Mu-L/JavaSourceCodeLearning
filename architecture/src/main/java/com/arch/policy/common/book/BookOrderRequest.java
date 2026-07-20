package com.arch.policy.common.book;

import java.io.Serializable;
import java.math.BigDecimal;

public final class BookOrderRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String customerId;
    private String productId;
    private int quantity;
    private BigDecimal amount;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
