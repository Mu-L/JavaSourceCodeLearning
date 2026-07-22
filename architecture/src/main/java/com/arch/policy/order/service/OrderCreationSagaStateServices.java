package com.arch.policy.order.service;

import com.arch.policy.order.model.OrderRequest;
import com.arch.policy.order.model.OrderStatus;
import com.arch.policy.order.model.SupplierOrderStatus;
import com.arch.policy.order.model.ThirdOrderResult;
import com.arch.policy.order.model.ThirdOrderStatus;
import com.arch.policy.order.model.TradeOrder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/** Seata 状态节点调用的短事务服务；未知三方结果不会错误返还库存。 */
@Service("orderCreationSagaStateServices")
public final class OrderCreationSagaStateServices {
    @Resource
    private OrderTransactionService orderTransactionService;
    @Resource
    private InventoryTransactionService inventoryTransactionService;
    @Resource
    private SupplierOrderService supplierOrderService;
    @Resource
    private SupplierOrderTransactionService supplierOrderTransactionService;
    @Resource
    private OrderRecoveryService orderRecoveryService;

    public boolean deductStock(TradeOrder order, OrderRequest request) {
        boolean deducted = inventoryTransactionService.deduct(
                order.getTradeOrderSerialNo(), request);
        return deducted;
    }

    public ThirdOrderResult createThirdOrder(TradeOrder order, OrderRequest request) {
        supplierOrderTransactionService.markCreating(order);
        ThirdOrderResult result;
        try {
            result = supplierOrderService.createOrder(order.getTradeOrderSerialNo(), request);
        } catch (RuntimeException uncertainFailure) {
            // 网络异常只代表结果未知，不能当作三方明确失败并返还库存。
            result = new ThirdOrderResult(null, ThirdOrderStatus.UNKNOWN);
        }
        supplierOrderTransactionService.saveResult(order.getTradeOrderSerialNo(),
                result.getThirdPartyOrderNo(), statusOf(result.getStatus()));
        return result;
    }

    public boolean completeSuccess(TradeOrder order) {
        inventoryTransactionService.confirm(order.getTradeOrderSerialNo());
        orderTransactionService.updateStatus(order, OrderStatus.WAIT_PAY);
        return true;
    }

    public boolean completeFailure(TradeOrder order) {
        inventoryTransactionService.returnStock(order.getTradeOrderSerialNo());
        orderTransactionService.updateStatus(order, OrderStatus.CREATE_FAIL);
        return true;
    }

    public boolean completeStockFailure(TradeOrder order) {
        orderTransactionService.updateStatus(order, OrderStatus.CREATE_FAIL);
        return true;
    }

    public boolean waitForThirdResult(TradeOrder order) {
        orderRecoveryService.createThirdOrderQueryTask(order);
        return true;
    }

    private static SupplierOrderStatus statusOf(ThirdOrderStatus status) {
        if (status == ThirdOrderStatus.SUCCESS) return SupplierOrderStatus.SUCCESS;
        if (status == ThirdOrderStatus.FAILED) return SupplierOrderStatus.FAILED;
        return SupplierOrderStatus.UNKNOWN;
    }
}
