package com.arch.policy.common.redis;

public final class RedisClientException extends RuntimeException {
    public RedisClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
