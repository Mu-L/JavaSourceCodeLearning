package com.arch.policy.order.service;

import com.arch.policy.order.model.OrderRequest;
import com.arch.policy.order.model.InventoryTransaction;
import com.arch.policy.order.model.InventoryTransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 只负责库存库事务：正常扣库存并记录幂等库存流水，不使用预占库存。 */
@Service
public class InventoryTransactionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryTransactionService.class);
    private final ConcurrentMap<String, InventoryTransaction> transactions =
            new ConcurrentHashMap<String, InventoryTransaction>();

    public boolean deduct(String tradeOrderSerialNo, OrderRequest request) {
        LOGGER.info("[库存库事务-开始] 查询库存流水, tradeOrderSerialNo={}, skuId={}",
                tradeOrderSerialNo, request.getSkuId());
        LOGGER.info("[库存库] 原子扣减库存: stock = stock - {}, 条件 stock >= {}, skuId={}",
                request.getQuantity(), request.getQuantity(), request.getSkuId());
        InventoryTransaction created = new InventoryTransaction(tradeOrderSerialNo,
                request.getSkuId(), request.getQuantity(), InventoryTransactionStatus.DEDUCTED);
        InventoryTransaction existing = transactions.putIfAbsent(tradeOrderSerialNo, created);
        InventoryTransaction transaction = existing == null ? created : existing;
        LOGGER.info("[库存库] 幂等写库存流水, tradeOrderSerialNo={}, status={}",
                tradeOrderSerialNo, transaction.getStatus());
        LOGGER.info("[库存库事务-提交] 库存扣减和DEDUCTED流水同时提交");
        return transaction.getStatus() == InventoryTransactionStatus.DEDUCTED
                || transaction.getStatus() == InventoryTransactionStatus.CONFIRMED;
    }

    public void confirm(String tradeOrderSerialNo) {
        InventoryTransaction transaction = required(tradeOrderSerialNo);
        synchronized (transaction) {
            if (transaction.getStatus() == InventoryTransactionStatus.DEDUCTED) {
                transaction.setStatus(InventoryTransactionStatus.CONFIRMED);
            } else if (transaction.getStatus() != InventoryTransactionStatus.CONFIRMED) {
                throw new IllegalStateException("inventory cannot be confirmed from status: "
                        + transaction.getStatus());
            }
        }
        LOGGER.info("[库存库事务] 库存流水 {} -> {}, tradeOrderSerialNo={}",
                InventoryTransactionStatus.DEDUCTED, InventoryTransactionStatus.CONFIRMED,
                tradeOrderSerialNo);
    }

    public void returnStock(String tradeOrderSerialNo) {
        InventoryTransaction transaction = required(tradeOrderSerialNo);
        synchronized (transaction) {
            if (transaction.getStatus() == InventoryTransactionStatus.DEDUCTED) {
                transaction.setStatus(InventoryTransactionStatus.RETURNED);
            } else if (transaction.getStatus() != InventoryTransactionStatus.RETURNED) {
                throw new IllegalStateException("inventory cannot be returned from status: "
                        + transaction.getStatus());
            }
        }
        LOGGER.info("[库存库事务-开始] 库存流水仅允许 {} -> {}, tradeOrderSerialNo={}",
                InventoryTransactionStatus.DEDUCTED, InventoryTransactionStatus.RETURNED,
                tradeOrderSerialNo);
        LOGGER.info("[库存库] 只有流水状态更新成功才执行 stock = stock + quantity，防止重复返还");
        LOGGER.info("[库存库事务-提交] 库存返还和RETURNED流水同时提交");
    }

    public InventoryTransaction findByTradeOrderSerialNo(String tradeOrderSerialNo) {
        return transactions.get(tradeOrderSerialNo);
    }

    private InventoryTransaction required(String tradeOrderSerialNo) {
        InventoryTransaction transaction = transactions.get(tradeOrderSerialNo);
        if (transaction == null) {
            throw new IllegalStateException("inventory transaction not found: "
                    + tradeOrderSerialNo);
        }
        return transaction;
    }
}
