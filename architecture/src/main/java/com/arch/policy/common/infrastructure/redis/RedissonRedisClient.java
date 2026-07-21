package com.arch.policy.common.infrastructure.redis;

import com.arch.policy.common.redis.RedisClient;
import com.arch.policy.common.redis.RedisClientException;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;

import java.util.concurrent.TimeUnit;

public final class RedissonRedisClient implements RedisClient {
    private final RedissonClient redisson;

    public RedissonRedisClient(RedissonClient redisson) { this.redisson = redisson; }

    @Override public <T> T get(String key) {
        try {
            return redisson.<T>getBucket(key).get();
        } catch (RedisException redisFailure) {
            throw new RedisClientException("Failed to read Redis key=" + key, redisFailure);
        }
    }

    @Override public void set(String key, Object value, long ttlMillis) {
        try {
            redisson.getBucket(key).set(value, ttlMillis, TimeUnit.MILLISECONDS);
        } catch (RedisException redisFailure) {
            throw new RedisClientException("Failed to write Redis key=" + key, redisFailure);
        }
    }
}
