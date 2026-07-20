package com.arch.policy.book.infrastructure.repository;

import com.arch.policy.book.application.BookOrderStore;
import com.arch.policy.book.application.OrderOutboxMessage;
import com.arch.policy.book.application.OrderStateHistory;
import com.arch.policy.book.application.TransitionCommitResult;
import com.arch.policy.book.domain.BookOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryBookOrderStore implements BookOrderStore {
    private final Map<String, BookOrder> orders = new HashMap<String, BookOrder>();
    private final Map<String, String> requestIndexes = new HashMap<String, String>();
    private final Map<String, BookOrder> eventResults = new HashMap<String, BookOrder>();
    private final List<OrderStateHistory> histories = new ArrayList<OrderStateHistory>();
    private final List<OrderOutboxMessage> outboxMessages = new ArrayList<OrderOutboxMessage>();

    @Override public synchronized BookOrder findByOrderNo(String orderNo) {
        return copy(orders.get(orderNo));
    }

    @Override public synchronized BookOrder findByRequestId(String requestId) {
        String orderNo = requestIndexes.get(requestId);
        return orderNo == null ? null : copy(orders.get(orderNo));
    }

    @Override public synchronized BookOrder findByEventId(String eventId) {
        return copy(eventResults.get(eventId));
    }

    @Override public synchronized TransitionCommitResult create(
            BookOrder order, OrderStateHistory history, OrderOutboxMessage outboxMessage) {
        String existingOrderNo = requestIndexes.get(order.getRequestId());
        if (existingOrderNo != null) {
            return TransitionCommitResult.duplicate(copy(orders.get(existingOrderNo)));
        }
        persist(order, history, outboxMessage);
        requestIndexes.put(order.getRequestId(), order.getOrderNo());
        return TransitionCommitResult.committed(order.copy());
    }

    @Override public synchronized TransitionCommitResult transit(
            BookOrder order, long expectedVersion, OrderStateHistory history,
            OrderOutboxMessage outboxMessage) {
        BookOrder processed = eventResults.get(history.getEventId());
        if (processed != null) return TransitionCommitResult.duplicate(processed.copy());

        BookOrder current = orders.get(order.getOrderNo());
        if (current == null || current.getVersion() != expectedVersion
                || order.getVersion() != expectedVersion + 1) {
            throw new IllegalStateException("concurrent order update: " + order.getOrderNo());
        }
        persist(order, history, outboxMessage);
        return TransitionCommitResult.committed(order.copy());
    }

    public synchronized List<OrderStateHistory> historiesOf(String orderNo) {
        List<OrderStateHistory> result = new ArrayList<OrderStateHistory>();
        for (OrderStateHistory history : histories) {
            if (orderNo.equals(history.getOrderNo())) result.add(history);
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized List<OrderOutboxMessage> unpublishedOutboxMessages() {
        List<OrderOutboxMessage> result = new ArrayList<OrderOutboxMessage>();
        for (OrderOutboxMessage message : outboxMessages) {
            if (!message.isPublished()) result.add(message);
        }
        return Collections.unmodifiableList(result);
    }

    private void persist(BookOrder order, OrderStateHistory history, OrderOutboxMessage outboxMessage) {
        BookOrder snapshot = order.copy();
        orders.put(order.getOrderNo(), snapshot);
        eventResults.put(history.getEventId(), snapshot.copy());
        histories.add(history);
        outboxMessages.add(outboxMessage);
    }

    private static BookOrder copy(BookOrder order) { return order == null ? null : order.copy(); }
}
