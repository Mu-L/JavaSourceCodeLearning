package com.arch.policy.book.domain;

public final class RequiredAttributeGuard implements TransitionGuard {
    private final String attributeName;

    public RequiredAttributeGuard(String attributeName) {
        this.attributeName = attributeName;
    }

    @Override public void check(StateTransitionContext context) {
        String value = context.attribute(attributeName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(attributeName + " is required for event " + context.getEvent());
        }
    }
}
