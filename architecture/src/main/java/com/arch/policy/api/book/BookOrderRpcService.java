package com.arch.policy.api.book;

import com.arch.policy.common.book.BookOrderRequest;
import com.arch.policy.common.book.BookOrderResponse;
import com.arch.policy.common.book.OrderEventRequest;

public interface BookOrderRpcService {

    BookOrderResponse createOrder(BookOrderRequest request);

    BookOrderResponse fireEvent(OrderEventRequest request);
}
