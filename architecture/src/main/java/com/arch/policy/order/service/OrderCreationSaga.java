package com.arch.policy.order.service;

import com.arch.policy.order.model.OrderRequest;
import com.arch.policy.order.model.TradeOrder;

/** 本地订单提交后的跨库、跨三方创建流程。 */
public interface OrderCreationSaga {
    void start(TradeOrder order, OrderRequest request);
}
