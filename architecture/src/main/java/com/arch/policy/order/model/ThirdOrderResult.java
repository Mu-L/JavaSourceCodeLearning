package com.arch.policy.order.model;

public class ThirdOrderResult {
    private final String thirdPartyOrderNo;
    private final ThirdOrderStatus status;

    public ThirdOrderResult(String thirdPartyOrderNo, ThirdOrderStatus status) {
        this.thirdPartyOrderNo = thirdPartyOrderNo;
        this.status = status;
    }

    public String getThirdPartyOrderNo() { return thirdPartyOrderNo; }
    public ThirdOrderStatus getStatus() { return status; }
}
