package com.arch.policy.order.model;

import java.io.Serializable;

/**
 * @Author : haiyang.luo
 * @Date : 2026/7/22 15:46
 * @Description :
 */
public class OrderRequest implements Serializable {

    private static final long serialVersionUID = 6809741055680415850L;

    /**
     * 主订单
     */
    private String orderSerialNo;

    /**
     * 供应商ID
     */
    private String supplierId;

    /**
     * 商品ID
     */
    private String skuId;

    /**
     * 下单数量
     */
    private int quantity;

    public String getOrderSerialNo() {
        return orderSerialNo;
    }

    public void setOrderSerialNo(String orderSerialNo) {
        this.orderSerialNo = orderSerialNo;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
