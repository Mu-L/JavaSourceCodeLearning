package com.arch.policy.book.application;

import com.arch.policy.book.domain.OrderStateMachine;
import com.arch.policy.book.domain.TransitionExecution;

public interface PostTransitionExecutor {
    void execute(OrderStateMachine stateMachine, TransitionExecution execution);
}
