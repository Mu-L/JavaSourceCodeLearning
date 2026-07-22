package com.arch.policy.order.model;

/** 驱动订单业务状态迁移的业务事件。 */
public enum OrderEvent {
    /** 库存和三方创建流程均已成功，订单进入待支付。 */
    CREATE_SUCCEEDED,

    /** 库存不足或三方创建明确失败，订单创建失败。 */
    CREATE_FAILED,

    /** 支付成功，订单开始执行供应商预订。 */
    PAY_SUCCEEDED,

    /** 支付明确失败。 */
    PAY_FAILED,

    /** 供应商明确返回预订成功。 */
    BOOK_SUCCEEDED,

    /** 供应商明确返回预订失败。 */
    BOOK_FAILED
}
