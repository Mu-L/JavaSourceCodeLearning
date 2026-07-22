package com.arch.policy.order.model;

/** 三方订单结果；超时和处理中统一按 UNKNOWN 处理，不能直接返还库存。 */
public enum ThirdOrderStatus {
    /** 三方接口明确返回业务处理成功。 */
    SUCCESS,

    /** 三方接口明确返回业务处理失败，且不会转为成功。 */
    FAILED,

    /** 三方处理中、调用超时或响应无法确认最终业务结果。 */
    UNKNOWN
}
