package com.arch.policy.order.model;

/** 订单库中的供应商订单关联记录，与 TradeOrder 通过子订单号关联。 */
public class SupplierOrder {
    private final String tradeOrderSerialNo;
    private final String supplierId;
    private final String requestNo;
    private String thirdPartyOrderNo;
    private SupplierOrderStatus status;

    public SupplierOrder(String tradeOrderSerialNo, String supplierId, String requestNo,
                         SupplierOrderStatus status) {
        this.tradeOrderSerialNo = tradeOrderSerialNo;
        this.supplierId = supplierId;
        this.requestNo = requestNo;
        this.status = status;
    }

    public String getTradeOrderSerialNo() { return tradeOrderSerialNo; }
    public String getSupplierId() { return supplierId; }
    public String getRequestNo() { return requestNo; }
    public String getThirdPartyOrderNo() { return thirdPartyOrderNo; }
    public void setThirdPartyOrderNo(String thirdPartyOrderNo) { this.thirdPartyOrderNo = thirdPartyOrderNo; }
    public SupplierOrderStatus getStatus() { return status; }
    public void setStatus(SupplierOrderStatus status) { this.status = status; }
}
