package com.arch.policy.book.application;

public interface PaymentGateway {
    void refund(String orderNo, String paymentNo);
}
