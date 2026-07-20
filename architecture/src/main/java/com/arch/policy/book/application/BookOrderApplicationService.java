package com.arch.policy.book.application;

import com.arch.policy.book.domain.BookOrder;
import com.arch.policy.book.domain.OrderEvent;
import com.arch.policy.book.domain.OrderState;
import com.arch.policy.book.domain.OrderStateMachine;
import com.arch.policy.book.domain.StateTransitionContext;
import com.arch.policy.book.domain.TransitionExecution;
import com.arch.policy.common.book.BookOrderRequest;
import com.arch.policy.common.book.BookOrderResponse;
import com.arch.policy.common.book.OrderEventRequest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

public final class BookOrderApplicationService {
    private final BookOrderStore store;
    private final OrderStateMachine stateMachine;
    private final PostTransitionExecutor postTransitionExecutor;

    public BookOrderApplicationService(BookOrderStore store, OrderStateMachine stateMachine,
                                       PostTransitionExecutor postTransitionExecutor) {
        this.store = store;
        this.stateMachine = stateMachine;
        this.postTransitionExecutor = postTransitionExecutor;
    }

    public BookOrderResponse createOrder(BookOrderRequest request) {
        validate(request);
        BookOrder existing = store.findByRequestId(request.getRequestId());
        if (existing != null) return response(existing);

        BookOrder order = BookOrder.create(newOrderNo(), request.getRequestId(), request.getCustomerId(),
                request.getProductId(), request.getQuantity(), request.getAmount());
        TransitionExecution execution = stateMachine.fire(new StateTransitionContext(
                "CREATE:" + request.getRequestId(), OrderEvent.CREATE_SUCCEEDED, order,
                request.getCustomerId(), Collections.<String, String>emptyMap()));
        TransitionCommitResult result = store.create(order, history(execution), outbox(execution));
        runPostActionsIfCommitted(result, execution);
        return response(result.getOrder());
    }

    public BookOrderResponse fireEvent(OrderEventRequest request) {
        validate(request);
        BookOrder processed = store.findByEventId(request.getEventId());
        if (processed != null) return response(processed);
        BookOrder order = store.findByOrderNo(request.getOrderNo());
        if (order == null) throw new IllegalArgumentException("order not found: " + request.getOrderNo());
        if (order.getVersion() != request.getExpectedVersion()) {
            throw new IllegalStateException("stale order version: " + request.getExpectedVersion());
        }
        OrderEvent event = parseEvent(request.getEvent());
        TransitionExecution execution = stateMachine.fire(new StateTransitionContext(
                request.getEventId(), event, order, request.getOperator(), request.getAttributes()));
        TransitionCommitResult result = store.transit(order, execution.getPreviousVersion(),
                history(execution), outbox(execution));
        runPostActionsIfCommitted(result, execution);
        return response(result.getOrder());
    }

    private void runPostActionsIfCommitted(TransitionCommitResult result, TransitionExecution execution) {
        if (!result.isDuplicateEvent()) postTransitionExecutor.execute(stateMachine, execution);
    }

    private static OrderStateHistory history(TransitionExecution execution) {
        return new OrderStateHistory(execution.getContext().getEventId(),
                execution.getContext().getOrder().getOrderNo(), execution.getTransition().getFrom(),
                execution.getTransition().getEvent(), execution.getTransition().getTo(),
                execution.getContext().getOperator(), System.currentTimeMillis());
    }

    private static OrderOutboxMessage outbox(TransitionExecution execution) {
        BookOrder order = execution.getContext().getOrder();
        return new OrderOutboxMessage(execution.getContext().getEventId(), order.getOrderNo(),
                execution.getContext().getEvent(), order.getState(), order.getVersion());
    }

    private static void validate(BookOrderRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        if (isBlank(request.getRequestId())) throw new IllegalArgumentException("requestId is required");
        if (isBlank(request.getCustomerId())) throw new IllegalArgumentException("customerId is required");
        if (isBlank(request.getProductId())) throw new IllegalArgumentException("productId is required");
        if (request.getQuantity() <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }

    private static void validate(OrderEventRequest request) {
        if (request == null || isBlank(request.getEventId()) || isBlank(request.getOrderNo())
                || isBlank(request.getEvent()) || isBlank(request.getOperator())) {
            throw new IllegalArgumentException("eventId, orderNo, event and operator are required");
        }
    }

    private static OrderEvent parseEvent(String event) {
        try { return OrderEvent.valueOf(event.trim().toUpperCase()); }
        catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("unknown order event: " + event, failure);
        }
    }

    private static String newOrderNo() {
        return "BO" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private static BookOrderResponse response(BookOrder order) {
        return new BookOrderResponse(order.getOrderNo(), order.getState().name(), order.getVersion());
    }
}
