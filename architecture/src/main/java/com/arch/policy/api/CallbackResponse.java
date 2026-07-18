package com.arch.policy.api;

import java.io.Serializable;

public final class CallbackResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean success;
    public CallbackResponse(boolean success) { this.success = success; }
    public boolean isSuccess() { return success; }
}
