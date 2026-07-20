package com.arch.policy.book.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OrderStateMachine {
    private final Map<TransitionKey, OrderTransition> transitions;

    public OrderStateMachine(List<OrderTransition> definitions) {
        Map<TransitionKey, OrderTransition> registry = new HashMap<TransitionKey, OrderTransition>();
        for (OrderTransition transition : definitions) {
            TransitionKey key = new TransitionKey(transition.getFrom(), transition.getEvent());
            if (registry.put(key, transition) != null) {
                throw new IllegalStateException("duplicate order transition: " + key);
            }
        }
        transitions = Collections.unmodifiableMap(registry);
    }

    public TransitionExecution fire(StateTransitionContext context) {
        OrderTransition transition = transitions.get(
                new TransitionKey(context.getOrder().getState(), context.getEvent()));
        if (transition == null) {
            throw new IllegalStateException("unsupported order event " + context.getEvent()
                    + " in state " + context.getOrder().getState());
        }
        executeGuards(transition, context);
        executeActions(transition.getPreActions(), context);
        long previousVersion = context.getOrder().getVersion();
        context.getOrder().applyState(transition.getTo());
        return new TransitionExecution(transition, context, previousVersion);
    }

    public void afterCommit(TransitionExecution execution) {
        executeActions(execution.getTransition().getPostActions(), execution.getContext());
    }

    public OrderState targetOf(OrderState state, OrderEvent event) {
        OrderTransition transition = transitions.get(new TransitionKey(state, event));
        return transition == null ? null : transition.getTo();
    }

    private static void executeGuards(OrderTransition transition, StateTransitionContext context) {
        for (TransitionGuard guard : transition.getGuards()) guard.check(context);
    }

    private static void executeActions(List<TransitionAction> actions, StateTransitionContext context) {
        for (TransitionAction action : actions) action.execute(context);
    }

    private static final class TransitionKey {
        private final OrderState state;
        private final OrderEvent event;

        private TransitionKey(OrderState state, OrderEvent event) {
            this.state = state;
            this.event = event;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof TransitionKey)) return false;
            TransitionKey that = (TransitionKey) other;
            return state == that.state && event == that.event;
        }

        @Override public int hashCode() { return 31 * state.hashCode() + event.hashCode(); }
        @Override public String toString() { return state + " + " + event; }
    }
}
