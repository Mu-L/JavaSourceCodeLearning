package com.arch.policy.book.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OrderTransition {
    private final OrderState from;
    private final OrderEvent event;
    private final OrderState to;
    private final List<TransitionGuard> guards;
    private final List<TransitionAction> preActions;
    private final List<TransitionAction> postActions;

    private OrderTransition(Builder builder) {
        from = builder.from;
        event = builder.event;
        to = builder.to;
        guards = immutableCopy(builder.guards);
        preActions = immutableCopy(builder.preActions);
        postActions = immutableCopy(builder.postActions);
    }

    public static Builder from(OrderState from, OrderEvent event, OrderState to) {
        return new Builder(from, event, to);
    }

    public OrderState getFrom() { return from; }
    public OrderEvent getEvent() { return event; }
    public OrderState getTo() { return to; }
    public List<TransitionGuard> getGuards() { return guards; }
    public List<TransitionAction> getPreActions() { return preActions; }
    public List<TransitionAction> getPostActions() { return postActions; }

    private static <T> List<T> immutableCopy(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }

    public static final class Builder {
        private final OrderState from;
        private final OrderEvent event;
        private final OrderState to;
        private final List<TransitionGuard> guards = new ArrayList<TransitionGuard>();
        private final List<TransitionAction> preActions = new ArrayList<TransitionAction>();
        private final List<TransitionAction> postActions = new ArrayList<TransitionAction>();

        private Builder(OrderState from, OrderEvent event, OrderState to) {
            if (from == null || event == null || to == null) {
                throw new IllegalArgumentException("from, event and to are required");
            }
            this.from = from;
            this.event = event;
            this.to = to;
        }

        public Builder guard(TransitionGuard guard) { guards.add(guard); return this; }
        public Builder before(TransitionAction action) { preActions.add(action); return this; }
        public Builder after(TransitionAction action) { postActions.add(action); return this; }
        public OrderTransition build() { return new OrderTransition(this); }
    }
}
