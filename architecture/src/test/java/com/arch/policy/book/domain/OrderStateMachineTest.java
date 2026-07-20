package com.arch.policy.book.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStateMachineTest {
    @Test
    void resolvesTargetByCurrentStateAndBusinessEvent() {
        OrderStateMachine stateMachine = new OrderStateMachine(DefaultOrderTransitions.definitions());

        assertEquals(OrderState.WAIT_PAY,
                stateMachine.targetOf(OrderState.CREATE, OrderEvent.CREATE_SUCCEEDED));
        assertEquals(OrderState.BOOKING,
                stateMachine.targetOf(OrderState.WAIT_PAY, OrderEvent.PAY_SUCCEEDED));
        assertEquals(OrderState.BOOKED,
                stateMachine.targetOf(OrderState.VALIDATE_FAIL, OrderEvent.VALIDATE_SUCCEEDED));
        assertNull(stateMachine.targetOf(OrderState.WAIT_PAY, OrderEvent.BOOK_SUCCEEDED));
    }

    @Test
    void executesGuardAndPreActionBeforeMutationAndPostActionAfterCommit() {
        final AtomicInteger calls = new AtomicInteger();
        TransitionGuard guard = context -> calls.compareAndSet(0, 1);
        TransitionAction preAction = context -> {
            assertEquals(OrderState.CREATE, context.getOrder().getState());
            calls.compareAndSet(1, 2);
        };
        TransitionAction postAction = context -> {
            assertEquals(OrderState.WAIT_PAY, context.getOrder().getState());
            calls.compareAndSet(2, 3);
        };
        OrderTransition transition = OrderTransition
                .from(OrderState.CREATE, OrderEvent.CREATE_SUCCEEDED, OrderState.WAIT_PAY)
                .guard(guard).before(preAction).after(postAction).build();
        OrderStateMachine stateMachine = new OrderStateMachine(Arrays.asList(transition));
        BookOrder order = order();

        TransitionExecution execution = stateMachine.fire(context(order, OrderEvent.CREATE_SUCCEEDED));
        assertEquals(2, calls.get());
        assertEquals(OrderState.WAIT_PAY, order.getState());

        stateMachine.afterCommit(execution);
        assertEquals(3, calls.get());
    }

    @Test
    void guardFailurePreventsStateMutation() {
        OrderTransition transition = OrderTransition
                .from(OrderState.CREATE, OrderEvent.CREATE_SUCCEEDED, OrderState.WAIT_PAY)
                .guard(context -> { throw new IllegalStateException("risk rejected"); })
                .build();
        OrderStateMachine stateMachine = new OrderStateMachine(Arrays.asList(transition));
        BookOrder order = order();

        assertThrows(IllegalStateException.class,
                () -> stateMachine.fire(context(order, OrderEvent.CREATE_SUCCEEDED)));
        assertEquals(OrderState.CREATE, order.getState());
        assertEquals(0L, order.getVersion());
    }

    @Test
    void rejectsDuplicateTransitionRegistration() {
        OrderTransition first = OrderTransition
                .from(OrderState.CREATE, OrderEvent.CREATE_SUCCEEDED, OrderState.WAIT_PAY).build();
        OrderTransition duplicate = OrderTransition
                .from(OrderState.CREATE, OrderEvent.CREATE_SUCCEEDED, OrderState.CREATE_FAIL).build();

        assertThrows(IllegalStateException.class,
                () -> new OrderStateMachine(Arrays.asList(first, duplicate)));
    }

    private static BookOrder order() {
        return BookOrder.create("order-1", "request-1", "customer-1", "product-1",
                1, BigDecimal.ONE);
    }

    private static StateTransitionContext context(BookOrder order, OrderEvent event) {
        return new StateTransitionContext("event-1", event, order, "tester",
                Collections.<String, String>emptyMap());
    }
}
