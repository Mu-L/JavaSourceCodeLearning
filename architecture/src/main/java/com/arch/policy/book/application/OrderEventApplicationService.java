package com.arch.policy.book.application;

import com.arch.policy.book.domain.BookOrder;
import com.arch.policy.book.domain.OrderEvent;
import com.arch.policy.book.domain.OrderStateMachine;
import com.arch.policy.book.domain.StateTransitionContext;
import com.arch.policy.book.domain.TransitionExecution;
import com.arch.policy.common.book.BookOrderResponse;
import com.arch.policy.common.book.OrderEventRequest;

public final class OrderEventApplicationService {
    private static final long CREATE_RESULT_TTL_MILLIS = 600_000L;
    private final BookOrderStore store;
    private final OrderStateMachine stateMachine;
    private final PostTransitionExecutor postTransitionExecutor;
    private final CompensationTaskService compensationTaskService;
    private final CreateOrderResultCache resultCache;

    public OrderEventApplicationService(BookOrderStore store, OrderStateMachine stateMachine,
                                        PostTransitionExecutor postTransitionExecutor,
                                        CompensationTaskService compensationTaskService,
                                        CreateOrderResultCache resultCache) {
        this.store = store;
        this.stateMachine = stateMachine;
        this.postTransitionExecutor = postTransitionExecutor;
        this.compensationTaskService = compensationTaskService;
        this.resultCache = resultCache;
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
        if (!result.isDuplicateEvent()) {
            enqueueCompensation(execution);
            postTransitionExecutor.execute(stateMachine, execution);
        }
        BookOrderResponse response = response(result.getOrder());
        resultCache.put(result.getOrder().getRequestId(), response, CREATE_RESULT_TTL_MILLIS);
        return response;
    }

    private void enqueueCompensation(TransitionExecution execution) {
        StateTransitionContext context = execution.getContext();
        if (context.getEvent() == OrderEvent.CANCEL && !isBlank(context.attribute("pnr"))) {
            compensationTaskService.cancelPnr(context.getOrder().getOrderNo(),
                    value(context.attribute("childOrderNo")), context.attribute("pnr"));
        }
        if (context.getEvent() == OrderEvent.BOOK_FAILED && !isBlank(context.attribute("paymentNo"))) {
            compensationTaskService.refundPayment(context.getOrder().getOrderNo(),
                    context.attribute("paymentNo"));
        }
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

    private static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private static String value(String value) { return value == null ? "" : value; }
    private static BookOrderResponse response(BookOrder order) {
        return new BookOrderResponse(order.getOrderNo(), order.getState().name(), order.getVersion());
    }
}
