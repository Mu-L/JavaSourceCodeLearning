package com.arch.policy.book.application;

import com.arch.policy.book.domain.BookOrder;
import com.arch.policy.book.domain.OrderEvent;
import com.arch.policy.book.domain.OrderState;
import com.arch.policy.book.domain.OrderStateMachine;
import com.arch.policy.book.domain.OrderTransition;
import com.arch.policy.book.domain.StateTransitionContext;
import com.arch.policy.book.domain.TransitionExecution;
import com.arch.policy.book.infrastructure.repository.InMemoryFailedPostActionStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetryablePostTransitionExecutorTest {
    @Test
    void recordsPostActionFailureForRetryWithoutRevertingCommittedState() {
        OrderTransition transition = OrderTransition
                .from(OrderState.CREATE, OrderEvent.CREATE_SUCCEEDED, OrderState.WAIT_PAY)
                .after(context -> { throw new IllegalStateException("notification unavailable"); })
                .build();
        OrderStateMachine stateMachine = new OrderStateMachine(Arrays.asList(transition));
        BookOrder order = BookOrder.create("order-1", "request-1", "customer-1", "product-1",
                1, BigDecimal.ONE);
        TransitionExecution execution = stateMachine.fire(new StateTransitionContext(
                "event-1", OrderEvent.CREATE_SUCCEEDED, order, "tester",
                Collections.<String, String>emptyMap()));
        InMemoryFailedPostActionStore failureStore = new InMemoryFailedPostActionStore();

        new RetryablePostTransitionExecutor(failureStore).execute(stateMachine, execution);

        assertEquals(OrderState.WAIT_PAY, order.getState());
        assertEquals(1, failureStore.all().size());
        assertEquals("event-1", failureStore.all().get(0).getEventId());
    }
}
