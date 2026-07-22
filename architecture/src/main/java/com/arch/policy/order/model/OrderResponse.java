package com.arch.policy.order.model;

import java.io.Serializable;

/**
 * @Author : haiyang.luo
 * @Date : 2026/7/22 15:45
 * @Description :
 */
public class OrderResponse implements Serializable {

    private static final long serialVersionUID = -655337401948649968L;

    private String orderSerialNo;
    private String tradeOrderSerialNo;
    private String thirdPartyOrderNo;
    private String status;
    private String message;

    public String getOrderSerialNo() { return orderSerialNo; }
    public void setOrderSerialNo(String orderSerialNo) { this.orderSerialNo = orderSerialNo; }
    public String getTradeOrderSerialNo() { return tradeOrderSerialNo; }
    public void setTradeOrderSerialNo(String tradeOrderSerialNo) { this.tradeOrderSerialNo = tradeOrderSerialNo; }
    public String getThirdPartyOrderNo() { return thirdPartyOrderNo; }
    public void setThirdPartyOrderNo(String thirdPartyOrderNo) { this.thirdPartyOrderNo = thirdPartyOrderNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
