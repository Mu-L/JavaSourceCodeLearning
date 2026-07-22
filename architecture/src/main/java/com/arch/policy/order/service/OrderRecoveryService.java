package com.arch.policy.order.service;

import com.arch.policy.order.model.OrderEvent;
import com.arch.policy.order.model.SupplierOrder;
import com.arch.policy.order.model.SupplierOrderStatus;
import com.arch.policy.order.model.ThirdOrderResult;
import com.arch.policy.order.model.ThirdOrderStatus;
import com.arch.policy.order.model.TradeOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/** 对三方超时、处理中等未知结果登记查询恢复任务。 */
@Service
public class OrderRecoveryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderRecoveryService.class);

    @Resource
    private SupplierOrderService supplierOrderService;
    @Resource
    private InventoryTransactionService inventoryTransactionService;
    @Resource
    private OrderTransactionService orderTransactionService;
    @Resource
    private SupplierOrderTransactionService supplierOrderTransactionService;

    public void createThirdOrderQueryTask(TradeOrder order) {
        SupplierOrder supplierOrder = supplierOrderTransactionService.findByTradeOrderSerialNo(
                order.getTradeOrderSerialNo());
        LOGGER.info("[订单库事务] 创建三方订单查询任务，保持库存已扣状态，不立即返还, "
                        + "tradeOrderSerialNo={}, thirdPartyOrderNo={}",
                order.getTradeOrderSerialNo(), thirdPartyOrderNo(supplierOrder));
    }

    /** 定时任务消费查询任务时执行；这里保留成普通方法突出恢复流程。 */
    public void reconcileThirdOrder(TradeOrder order) {
        SupplierOrder supplierOrder = supplierOrderTransactionService.findByTradeOrderSerialNo(
                order.getTradeOrderSerialNo());
        ThirdOrderResult result = supplierOrderService.queryOrder(order.getTradeOrderSerialNo(),
                thirdPartyOrderNo(supplierOrder), order.getSupplierId());

        if (result.getStatus() == ThirdOrderStatus.SUCCESS) {
            supplierOrderTransactionService.saveResult(order.getTradeOrderSerialNo(),
                    result.getThirdPartyOrderNo(), SupplierOrderStatus.SUCCESS);
            inventoryTransactionService.confirm(order.getTradeOrderSerialNo());
            orderTransactionService.fireEvent(order, OrderEvent.CREATE_SUCCEEDED);
            return;
        }

        if (result.getStatus() == ThirdOrderStatus.FAILED) {
            supplierOrderTransactionService.saveResult(order.getTradeOrderSerialNo(),
                    result.getThirdPartyOrderNo(), SupplierOrderStatus.FAILED);
            inventoryTransactionService.returnStock(order.getTradeOrderSerialNo());
            orderTransactionService.fireEvent(order, OrderEvent.CREATE_FAILED);
            return;
        }

        LOGGER.info("[恢复任务] 三方状态仍未知，保留已扣库存并等待下次查询, tradeOrderSerialNo={}",
                order.getTradeOrderSerialNo());
    }

    private static String thirdPartyOrderNo(SupplierOrder supplierOrder) {
        return supplierOrder == null ? null : supplierOrder.getThirdPartyOrderNo();
    }
}
