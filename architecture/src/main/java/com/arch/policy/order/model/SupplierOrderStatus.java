package com.arch.policy.order.model;

/** 本地记录的供应商订单协作状态。 */
public enum SupplierOrderStatus {
    /** 供应商订单关联记录已经创建，尚未调用三方接口。 */
    INIT,

    /** 正在使用本地子订单号作为幂等号调用三方创建接口。 */
    CREATING,

    /** 三方已经受理请求，但订单仍处于处理中。 */
    PENDING,

    /** 三方已经明确确认订单创建成功。 */
    SUCCESS,

    /** 三方已经明确确认订单创建失败。 */
    FAILED,

    /** 调用超时或响应不明确，需要主动查询三方最终状态。 */
    UNKNOWN,

    /** 正在调用三方订单取消接口。 */
    CANCELING,

    /** 三方已经明确确认订单取消成功。 */
    CANCELED
}
