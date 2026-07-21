package com.arch.policy.book.application;

public interface PromotionStockGateway {
    void returnStock(String orderNo, String promotionId);
}
