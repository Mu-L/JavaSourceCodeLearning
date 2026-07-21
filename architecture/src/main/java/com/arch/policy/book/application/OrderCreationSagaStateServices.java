package com.arch.policy.book.application;

import com.arch.policy.book.domain.BookOrder;
import com.arch.policy.common.book.OrderEventRequest;

import java.util.Collections;

public final class OrderCreationSagaStateServices {
    private final BookOrderStore store;
    private final GdsBookingGateway gdsGateway;
    private final OrderEventApplicationService eventService;
    private final CompensationTaskService compensationService;
    private final OrderWorkflowTaskStore workflowTaskStore;

    public OrderCreationSagaStateServices(BookOrderStore store, GdsBookingGateway gdsGateway,
                                          OrderEventApplicationService eventService,
                                          CompensationTaskService compensationService,
                                          OrderWorkflowTaskStore workflowTaskStore) {
        this.store = store;
        this.gdsGateway = gdsGateway;
        this.eventService = eventService;
        this.compensationService = compensationService;
        this.workflowTaskStore = workflowTaskStore;
    }

    public GdsBookingResult reservePnr(String orderNo) { return gdsGateway.createPnr(orderNo); }

    public boolean markCreateSuccess(String orderNo, String pnr) {
        fire(orderNo, orderNo + ":CREATE_SUCCESS", "CREATE_SUCCEEDED", "gds-booking-saga",
                Collections.singletonMap("bookingReference", value(pnr)));
        workflowTaskStore.saveIfAbsent(new OrderWorkflowTask("WAIT_PAYMENT:" + orderNo,
                orderNo, OrderWorkflowTask.TaskType.WAIT_PAYMENT));
        return true;
    }

    public boolean markCreateFailure(String orderNo, String errorCode, String promotionId) {
        fire(orderNo, orderNo + ":CREATE_FAIL", "CREATE_FAILED", "gds-booking-saga",
                Collections.singletonMap("failureCode", value(errorCode)));
        compensationService.returnPromotionStock(orderNo, value(promotionId));
        return true;
    }

    public boolean markCreateUnknown(String orderNo, String errorCode) {
        fire(orderNo, orderNo + ":CREATE_UNKNOWN", "START_VALIDATE", "gds-booking-saga",
                Collections.singletonMap("failureCode", value(errorCode)));
        workflowTaskStore.saveIfAbsent(new OrderWorkflowTask("MANUAL_GDS_VALIDATION:" + orderNo,
                orderNo, OrderWorkflowTask.TaskType.MANUAL_GDS_VALIDATION));
        compensationService.validateGdsBooking(orderNo);
        return true;
    }

    public boolean enqueueCancelPnr(String orderNo, String childOrderNo, String pnr) {
        compensationService.cancelPnr(orderNo, childOrderNo, pnr);
        return true;
    }

    public void markGdsValidationSuccess(String orderNo, String pnr) {
        fire(orderNo, orderNo + ":RECONCILE_SUCCESS", "GDS_BOOKING_CONFIRMED",
                "gds-reconciliation-job", Collections.singletonMap("bookingReference", value(pnr)));
        workflowTaskStore.saveIfAbsent(new OrderWorkflowTask("WAIT_PAYMENT:" + orderNo,
                orderNo, OrderWorkflowTask.TaskType.WAIT_PAYMENT));
    }

    public void markGdsValidationFailure(String orderNo, String errorCode) {
        BookOrder order = store.findByOrderNo(orderNo);
        fire(orderNo, orderNo + ":RECONCILE_FAIL", "GDS_BOOKING_REJECTED",
                "gds-reconciliation-job", Collections.singletonMap("failureCode", value(errorCode)));
        compensationService.returnPromotionStock(orderNo, value(order.getPromotionId()));
    }

    private void fire(String orderNo, String eventId, String event, String operator,
                      java.util.Map<String, String> attributes) {
        BookOrder order = store.findByOrderNo(orderNo);
        OrderEventRequest request = new OrderEventRequest();
        request.setOrderNo(orderNo);
        request.setEventId(eventId);
        request.setEvent(event);
        request.setOperator(operator);
        request.setExpectedVersion(order.getVersion());
        request.setAttributes(attributes);
        eventService.fireEvent(request);
    }

    private static String value(String value) { return value == null ? "" : value; }

}
