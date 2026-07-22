package com.arch.policy.order.model;

/** 本地子订单的最小快照。 */
public class TradeOrder {
    private final String orderSerialNo;
    private final String tradeOrderSerialNo;
    private final String supplierId;
    private OrderStatus status;

    public TradeOrder(String orderSerialNo, String tradeOrderSerialNo, String supplierId,
                      OrderStatus status) {
        this.orderSerialNo = orderSerialNo;
        this.tradeOrderSerialNo = tradeOrderSerialNo;
        this.supplierId = supplierId;
        this.status = status;
    }

    public String getOrderSerialNo() { return orderSerialNo; }
    public String getTradeOrderSerialNo() { return tradeOrderSerialNo; }
    public String getSupplierId() { return supplierId; }
    public OrderStatus getStatus() { return status; }

    /** 仅供订单状态机应用已经校验通过的目标状态。 */
    void applyStatus(OrderStatus status) { this.status = status; }
}
