package com.arch.policy.order.service;

import com.arch.policy.api.order.OrderRpcService;
import com.arch.policy.order.cache.OrderCacheUtil;
import com.arch.policy.order.cache.OrderLockUtil;
import com.arch.policy.order.model.OrderRequest;
import com.arch.policy.order.model.OrderResponse;
import com.arch.policy.order.model.OrderStatus;
import com.arch.policy.order.model.SupplierOrder;
import com.arch.policy.order.model.TradeOrder;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/** 创建本地子订单，并在本地事务提交后同步启动 Seata Saga。 */
@DubboService(version = "1.0.0", timeout = 3000)
public class OrderRpcServiceImpl implements OrderRpcService {
    @Resource
    private OrderCacheUtil orderCacheUtil;
    @Resource
    private OrderLockUtil orderLockUtil;
    @Resource
    private OrderTransactionService orderTransactionService;
    @Resource
    private OrderCreationSaga orderCreationSaga;
    @Resource
    private SupplierOrderTransactionService supplierOrderTransactionService;

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        validate(request);
        String businessKey = businessKey(request);

        OrderResponse cached = orderCacheUtil.getCache(businessKey);
        if (cached != null) return cached;

        if (!orderLockUtil.tryLock(businessKey)) {
            return processing(request, "相同下单请求正在处理中");
        }

        try {
            cached = orderCacheUtil.getCache(businessKey);
            if (cached != null) return cached;

            TradeOrder order = orderTransactionService.findByRequest(
                    request.getOrderSerialNo(), request.getSupplierId());
            if (order != null) {
                return cacheAndReturn(businessKey, order, "返回已存在的幂等订单");
            } else {
                // 这里只提交订单库本地事务，不写 Outbox，也不在事务中调用库存或三方。
                order = orderTransactionService.createOrder(request);
            }

            // 对同一幂等请求持锁启动，避免多个线程同时创建相同 businessKey 的 Saga。
            orderCreationSaga.start(order, request);
            return cacheAndReturn(businessKey, order, message(order.getStatus()));
        } finally {
            orderLockUtil.unlock(businessKey);
        }
    }

    private OrderResponse cacheAndReturn(String businessKey, TradeOrder order, String message) {
        OrderResponse result = response(order, message);
        orderCacheUtil.setCache(businessKey, result);
        return result;
    }

    private static String message(OrderStatus status) {
        if (status == OrderStatus.WAIT_PAY) return "下单成功，等待支付";
        if (status == OrderStatus.CREATE_FAIL) return "下单失败，库存已正确处理";
        return "Saga已启动，订单处理中";
    }

    private static OrderResponse processing(OrderRequest request, String message) {
        OrderResponse response = new OrderResponse();
        response.setOrderSerialNo(request.getOrderSerialNo());
        response.setStatus(OrderStatus.CREATE.name());
        response.setMessage(message);
        return response;
    }

    private OrderResponse response(TradeOrder order, String message) {
        SupplierOrder supplierOrder = supplierOrderTransactionService
                .findByTradeOrderSerialNo(order.getTradeOrderSerialNo());
        OrderResponse response = new OrderResponse();
        response.setOrderSerialNo(order.getOrderSerialNo());
        response.setTradeOrderSerialNo(order.getTradeOrderSerialNo());
        response.setThirdPartyOrderNo(supplierOrder == null
                ? null : supplierOrder.getThirdPartyOrderNo());
        response.setStatus(order.getStatus().name());
        response.setMessage(message);
        return response;
    }

    private static String businessKey(OrderRequest request) {
        return request.getOrderSerialNo() + ":" + request.getSupplierId();
    }

    private static void validate(OrderRequest request) {
        if (request == null) throw new IllegalArgumentException("orderRequest is required");
        if (isBlank(request.getOrderSerialNo())) {
            throw new IllegalArgumentException("orderSerialNo is required");
        }
        if (isBlank(request.getSupplierId())) {
            throw new IllegalArgumentException("supplierId is required");
        }
        if (isBlank(request.getSkuId())) throw new IllegalArgumentException("skuId is required");
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
