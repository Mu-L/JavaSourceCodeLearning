package com.arch.policy.order.service;

import com.arch.policy.order.model.SupplierOrder;
import com.arch.policy.order.model.SupplierOrderStatus;
import com.arch.policy.order.model.TradeOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 只负责订单库中的供应商订单关联记录；生产实现应替换为数据库事务。 */
@Service
public class SupplierOrderTransactionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupplierOrderTransactionService.class);
    private final ConcurrentMap<String, SupplierOrder> orders =
            new ConcurrentHashMap<String, SupplierOrder>();

    public SupplierOrder markCreating(TradeOrder order) {
        SupplierOrder created = new SupplierOrder(order.getTradeOrderSerialNo(),
                order.getSupplierId(), order.getTradeOrderSerialNo(), SupplierOrderStatus.CREATING);
        SupplierOrder existing = orders.putIfAbsent(order.getTradeOrderSerialNo(), created);
        SupplierOrder supplierOrder = existing == null ? created : existing;
        LOGGER.info("[订单库事务] 幂等创建供应商订单记录, tradeOrderSerialNo={}, status={}",
                order.getTradeOrderSerialNo(), supplierOrder.getStatus());
        return supplierOrder;
    }

    public void saveResult(String tradeOrderSerialNo, String thirdPartyOrderNo,
                           SupplierOrderStatus status) {
        SupplierOrder supplierOrder = required(tradeOrderSerialNo);
        synchronized (supplierOrder) {
            supplierOrder.setThirdPartyOrderNo(thirdPartyOrderNo);
            supplierOrder.setStatus(status);
        }
        LOGGER.info("[订单库事务] 保存供应商订单结果, tradeOrderSerialNo={}, thirdPartyOrderNo={}, status={}",
                tradeOrderSerialNo, thirdPartyOrderNo, status);
    }

    public SupplierOrder findByTradeOrderSerialNo(String tradeOrderSerialNo) {
        return orders.get(tradeOrderSerialNo);
    }

    private SupplierOrder required(String tradeOrderSerialNo) {
        SupplierOrder supplierOrder = orders.get(tradeOrderSerialNo);
        if (supplierOrder == null) {
            throw new IllegalStateException("supplier order not found: " + tradeOrderSerialNo);
        }
        return supplierOrder;
    }
}
