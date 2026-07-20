package com.arch.policy.book.application;

import com.arch.policy.book.domain.DefaultOrderTransitions;
import com.arch.policy.book.domain.OrderStateMachine;
import com.arch.policy.book.infrastructure.repository.InMemoryBookOrderStore;
import com.arch.policy.book.infrastructure.repository.InMemoryFailedPostActionStore;
import com.arch.policy.common.book.BookOrderRequest;
import com.arch.policy.common.book.BookOrderResponse;
import com.arch.policy.common.book.OrderEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookOrderApplicationServiceTest {
    private InMemoryBookOrderStore store;
    private BookOrderApplicationService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryBookOrderStore();
        OrderStateMachine stateMachine = new OrderStateMachine(DefaultOrderTransitions.definitions());
        service = new BookOrderApplicationService(store, stateMachine,
                new RetryablePostTransitionExecutor(new InMemoryFailedPostActionStore()));
    }

    @Test
    void createsOrderIdempotentlyAndCommitsHistoryWithOutbox() {
        BookOrderRequest request = request("request-1");

        BookOrderResponse first = service.createOrder(request);
        BookOrderResponse repeated = service.createOrder(request);

        assertEquals("WAIT_PAY", first.getState());
        assertEquals(1L, first.getVersion());
        assertEquals(first.getOrderNo(), repeated.getOrderNo());
        assertEquals(1, store.historiesOf(first.getOrderNo()).size());
        assertEquals(1, store.unpublishedOutboxMessages().size());
    }

    @Test
    void drivesOrderByBusinessEventsInsteadOfTargetStates() {
        BookOrderResponse created = service.createOrder(request("request-2"));

        BookOrderResponse booking = fire(created, "pay-1", "PAY_SUCCEEDED");
        BookOrderResponse booked = fire(booking, "book-1", "BOOK_SUCCEEDED");

        assertEquals("BOOKED", booked.getState());
        assertEquals(3L, booked.getVersion());
        assertEquals(3, store.historiesOf(booked.getOrderNo()).size());
        assertEquals(3, store.unpublishedOutboxMessages().size());
    }

    @Test
    void returnsOriginalResultForDuplicateEventEvenWithStaleVersion() {
        BookOrderResponse created = service.createOrder(request("request-3"));
        BookOrderResponse booking = fire(created, "pay-2", "PAY_SUCCEEDED");

        BookOrderResponse duplicate = fire(created, "pay-2", "PAY_SUCCEEDED");

        assertEquals(booking.getState(), duplicate.getState());
        assertEquals(booking.getVersion(), duplicate.getVersion());
        assertEquals(2, store.historiesOf(created.getOrderNo()).size());
    }

    @Test
    void rejectsUnsupportedEventAndConcurrentUpdate() {
        BookOrderResponse created = service.createOrder(request("request-4"));
        assertThrows(IllegalStateException.class,
                () -> fire(created, "book-too-early", "BOOK_SUCCEEDED"));

        fire(created, "pay-3", "PAY_SUCCEEDED");
        assertThrows(IllegalStateException.class,
                () -> fire(created, "cancel-stale", "CANCEL"));
    }

    private BookOrderResponse fire(BookOrderResponse current, String eventId, String event) {
        OrderEventRequest request = new OrderEventRequest();
        request.setEventId(eventId);
        request.setOrderNo(current.getOrderNo());
        request.setExpectedVersion(current.getVersion());
        request.setEvent(event);
        request.setOperator("test-operator");
        if ("PAY_SUCCEEDED".equals(event)) {
            request.setAttributes(Collections.singletonMap("paymentNo", "payment-1"));
        } else if ("BOOK_SUCCEEDED".equals(event)) {
            request.setAttributes(Collections.singletonMap("bookingReference", "PNR001"));
        }
        return service.fireEvent(request);
    }

    private static BookOrderRequest request(String requestId) {
        BookOrderRequest request = new BookOrderRequest();
        request.setRequestId(requestId);
        request.setCustomerId("customer-1");
        request.setProductId("policy-1");
        request.setQuantity(1);
        request.setAmount(new BigDecimal("100.00"));
        return request;
    }
}
