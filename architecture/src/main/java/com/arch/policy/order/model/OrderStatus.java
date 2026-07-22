package com.arch.policy.order.model;

/** 只表达订单业务生命周期，不承载库存、三方或 Saga 技术状态。 */
public enum OrderStatus {
    /** 本地订单已经创建，创建流程尚未得出最终业务结果。 */
    CREATE,

    /** 订单创建成功，正在等待用户完成支付。 */
    WAIT_PAY,

    /** 支付完成，正在向供应商执行预订。 */
    BOOKING,

    /** 供应商已经明确返回预订成功。 */
    BOOKED,

    /** 供应商明确返回预订失败。 */
    BOOK_FAIL,

    /** 订单支付失败。 */
    PAY_FAIL,

    /** 订单创建失败，例如库存不足或三方创建明确失败。 */
    CREATE_FAIL
}
