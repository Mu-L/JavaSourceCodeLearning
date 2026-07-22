package com.arch.policy.api.order;

import com.arch.policy.order.model.OrderRequest;
import com.arch.policy.order.model.OrderResponse;

/**
 * @Author : haiyang.luo
 * @Date : 2026/7/22 15:44
 * @Description :
 */
public interface OrderRpcService {

    OrderResponse createOrder(OrderRequest orderRequest);
}
