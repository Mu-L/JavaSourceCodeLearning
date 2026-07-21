package com.arch.policy.book.infrastructure.rpc;

import com.arch.policy.api.book.BookOrderRpcService;
import com.arch.policy.book.application.BookOrderApplicationService;
import com.arch.policy.book.application.OrderEventApplicationService;
import com.arch.policy.common.book.BookOrderRequest;
import com.arch.policy.common.book.BookOrderResponse;
import com.arch.policy.common.book.OrderEventRequest;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "1.0.0", timeout = 3000)
public final class DubboBookOrderService implements BookOrderRpcService {
    private final BookOrderApplicationService applicationService;
    private final OrderEventApplicationService eventService;

    public DubboBookOrderService(BookOrderApplicationService applicationService,
                                 OrderEventApplicationService eventService) {
        this.applicationService = applicationService;
        this.eventService = eventService;
    }

    @Override public BookOrderResponse createOrder(BookOrderRequest request) {
        return applicationService.createOrder(request);
    }

    @Override public BookOrderResponse fireEvent(OrderEventRequest request) {
        return eventService.fireEvent(request);
    }
}
