package com.arch.policy.order.model;

import org.springframework.stereotype.Component;

/** 集中维护订单业务状态迁移规则，禁止应用服务直接指定目标状态。 */
@Component
public final class OrderStateMachine {

    public OrderStatus fire(TradeOrder order, OrderEvent event) {
        if (order == null) throw new IllegalArgumentException("order is required");
        if (event == null) throw new IllegalArgumentException("order event is required");

        OrderStatus current = order.getStatus();
        OrderStatus target = target(current, event);
        order.applyStatus(target);
        return target;
    }

    private static OrderStatus target(OrderStatus current, OrderEvent event) {
        // Saga和消息可能至少投递一次；相同事件到达目标状态后按幂等成功处理。
        if (current == targetOf(event)) return current;

        switch (current) {
            case CREATE:
                if (event == OrderEvent.CREATE_SUCCEEDED) return OrderStatus.WAIT_PAY;
                if (event == OrderEvent.CREATE_FAILED) return OrderStatus.CREATE_FAIL;
                break;
            case WAIT_PAY:
                if (event == OrderEvent.PAY_SUCCEEDED) return OrderStatus.BOOKING;
                if (event == OrderEvent.PAY_FAILED) return OrderStatus.PAY_FAIL;
                break;
            case BOOKING:
                if (event == OrderEvent.BOOK_SUCCEEDED) return OrderStatus.BOOKED;
                if (event == OrderEvent.BOOK_FAILED) return OrderStatus.BOOK_FAIL;
                break;
            default:
                break;
        }
        throw new IllegalStateException("invalid order transition: " + current + " + " + event);
    }

    private static OrderStatus targetOf(OrderEvent event) {
        switch (event) {
            case CREATE_SUCCEEDED: return OrderStatus.WAIT_PAY;
            case CREATE_FAILED: return OrderStatus.CREATE_FAIL;
            case PAY_SUCCEEDED: return OrderStatus.BOOKING;
            case PAY_FAILED: return OrderStatus.PAY_FAIL;
            case BOOK_SUCCEEDED: return OrderStatus.BOOKED;
            case BOOK_FAILED: return OrderStatus.BOOK_FAIL;
            default: throw new IllegalArgumentException("unsupported order event: " + event);
        }
    }
}
