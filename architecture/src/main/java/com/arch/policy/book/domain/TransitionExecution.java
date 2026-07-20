package com.arch.policy.book.domain;

public final class TransitionExecution {
    private final OrderTransition transition;
    private final StateTransitionContext context;
    private final long previousVersion;

    public TransitionExecution(OrderTransition transition, StateTransitionContext context,
                               long previousVersion) {
        this.transition = transition;
        this.context = context;
        this.previousVersion = previousVersion;
    }

    public OrderTransition getTransition() { return transition; }
    public StateTransitionContext getContext() { return context; }
    public long getPreviousVersion() { return previousVersion; }
}
