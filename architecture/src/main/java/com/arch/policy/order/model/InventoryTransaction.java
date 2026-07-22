package com.arch.policy.order.model;

/** 库存库中的幂等扣减流水。 */
public class InventoryTransaction {
    private final String tradeOrderSerialNo;
    private final String skuId;
    private final int quantity;
    private InventoryTransactionStatus status;

    public InventoryTransaction(String tradeOrderSerialNo, String skuId, int quantity,
                                InventoryTransactionStatus status) {
        this.tradeOrderSerialNo = tradeOrderSerialNo;
        this.skuId = skuId;
        this.quantity = quantity;
        this.status = status;
    }

    public String getTradeOrderSerialNo() { return tradeOrderSerialNo; }
    public String getSkuId() { return skuId; }
    public int getQuantity() { return quantity; }
    public InventoryTransactionStatus getStatus() { return status; }
    public void setStatus(InventoryTransactionStatus status) { this.status = status; }
}
