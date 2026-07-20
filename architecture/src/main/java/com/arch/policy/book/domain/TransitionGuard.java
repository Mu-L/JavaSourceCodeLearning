package com.arch.policy.book.domain;

public interface TransitionGuard {
    void check(StateTransitionContext context);
}
