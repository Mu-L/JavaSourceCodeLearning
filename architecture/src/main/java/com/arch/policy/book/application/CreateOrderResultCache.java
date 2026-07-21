package com.arch.policy.book.application;

import com.arch.policy.common.book.BookOrderResponse;

public interface CreateOrderResultCache {
    BookOrderResponse get(String requestId);

    void put(String requestId, BookOrderResponse response, long ttlMillis);

    BookOrderResponse await(String requestId, long waitMillis);
}
