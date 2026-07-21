package com.arch.policy.common.redis;

public interface RedisClient {
    <T> T get(String key);

    void set(String key, Object value, long ttlMillis);
}
