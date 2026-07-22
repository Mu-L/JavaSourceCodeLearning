package com.arch.policy.order.model;

/** 库存库流水状态；库存库是该状态的权威来源。 */
public enum InventoryTransactionStatus {
    /** 正在执行库存扣减，最终结果尚未确定。 */
    DEDUCTING,

    /** 库存已经扣减，等待订单创建结果决定确认或返还。 */
    DEDUCTED,

    /** 三方订单创建成功，库存扣减已经最终确认。 */
    CONFIRMED,

    /** 正在执行库存返还，最终结果尚未确定。 */
    RETURNING,

    /** 库存已经幂等返还。 */
    RETURNED,

    /** 可用库存不足，未执行库存扣减。 */
    INSUFFICIENT,

    /** 库存操作超时或异常，需要查询库存流水确认结果。 */
    UNKNOWN
}
