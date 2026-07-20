package com.arch.policy.book.domain;

import java.util.ArrayList;
import java.util.List;

public final class DefaultOrderTransitions {
    private DefaultOrderTransitions() {}

    public static List<OrderTransition> definitions() {
        List<OrderTransition> transitions = new ArrayList<OrderTransition>();
        add(transitions, OrderState.CREATE, OrderEvent.CREATE_SUCCEEDED, OrderState.WAIT_PAY);
        add(transitions, OrderState.CREATE, OrderEvent.CREATE_FAILED, OrderState.CREATE_FAIL);
        add(transitions, OrderState.CREATE, OrderEvent.CANCEL, OrderState.CANCEL);
        transitions.add(OrderTransition
                .from(OrderState.WAIT_PAY, OrderEvent.PAY_SUCCEEDED, OrderState.BOOKING)
                .guard(new RequiredAttributeGuard("paymentNo")).build());
        add(transitions, OrderState.WAIT_PAY, OrderEvent.CANCEL, OrderState.CANCEL);
        transitions.add(OrderTransition
                .from(OrderState.BOOKING, OrderEvent.BOOK_SUCCEEDED, OrderState.BOOKED)
                .guard(new RequiredAttributeGuard("bookingReference")).build());
        transitions.add(OrderTransition
                .from(OrderState.BOOKING, OrderEvent.BOOK_FAILED, OrderState.BOOK_FAIL)
                .guard(new RequiredAttributeGuard("failureCode")).build());
        add(transitions, OrderState.BOOKING, OrderEvent.START_VALIDATE, OrderState.VALIDATING);
        add(transitions, OrderState.BOOK_FAIL, OrderEvent.START_VALIDATE, OrderState.VALIDATING);
        add(transitions, OrderState.BOOK_FAIL, OrderEvent.CANCEL, OrderState.CANCEL);
        add(transitions, OrderState.BOOK_FAIL, OrderEvent.DELETE, OrderState.DELETED);
        add(transitions, OrderState.BOOKED, OrderEvent.START_VALIDATE, OrderState.VALIDATING);
        add(transitions, OrderState.BOOKED, OrderEvent.CANCEL, OrderState.CANCEL);
        transitions.add(OrderTransition
                .from(OrderState.BOOKED, OrderEvent.REFUND_SUCCEEDED, OrderState.REFUNDED)
                .guard(new RequiredAttributeGuard("refundNo")).build());
        add(transitions, OrderState.VALIDATING, OrderEvent.VALIDATE_SUCCEEDED, OrderState.BOOKED);
        add(transitions, OrderState.VALIDATING, OrderEvent.VALIDATE_FAILED, OrderState.VALIDATE_FAIL);
        add(transitions, OrderState.VALIDATE_FAIL, OrderEvent.VALIDATE_SUCCEEDED, OrderState.BOOKED);
        add(transitions, OrderState.VALIDATE_FAIL, OrderEvent.CANCEL, OrderState.CANCEL);
        add(transitions, OrderState.CREATE_FAIL, OrderEvent.CANCEL, OrderState.CANCEL);
        add(transitions, OrderState.CREATE_FAIL, OrderEvent.DELETE, OrderState.DELETED);
        add(transitions, OrderState.CANCEL, OrderEvent.DELETE, OrderState.DELETED);
        add(transitions, OrderState.REFUNDED, OrderEvent.DELETE, OrderState.DELETED);
        return transitions;
    }

    private static void add(List<OrderTransition> transitions, OrderState from,
                            OrderEvent event, OrderState to) {
        transitions.add(OrderTransition.from(from, event, to).build());
    }
}
