package com.arch.policy.common.lock;

public final class DistributedLockUnavailableException extends RuntimeException {
    public DistributedLockUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
