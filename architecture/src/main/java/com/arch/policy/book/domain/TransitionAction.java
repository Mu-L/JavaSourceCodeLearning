package com.arch.policy.book.domain;

public interface TransitionAction {
    void execute(StateTransitionContext context);
}
