package com.arch.policy.book.application;

/** 与订单在同一本地事务中保存，用于可靠启动创建 Saga。 */
public final class OrderCreationOutboxMessage {
    private final String messageId;
    private final String orderNo;
    private final String promotionId;
    private boolean published;

    public OrderCreationOutboxMessage(String orderNo, String promotionId) {
        this.messageId = "START_ORDER_CREATION:" + orderNo;
        this.orderNo = orderNo;
        this.promotionId = promotionId;
    }

    public String getMessageId() { return messageId; }
    public String getOrderNo() { return orderNo; }
    public String getPromotionId() { return promotionId; }
    public boolean isPublished() { return published; }
    public void markPublished() { published = true; }
}
