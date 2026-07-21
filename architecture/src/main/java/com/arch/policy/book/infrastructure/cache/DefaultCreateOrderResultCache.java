package com.arch.policy.book.infrastructure.cache;

import com.arch.policy.book.application.CreateOrderResultCache;
import com.arch.policy.common.book.BookOrderResponse;
import com.arch.policy.common.redis.RedisClient;
import com.arch.policy.common.redis.RedisClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.LockSupport;

public final class DefaultCreateOrderResultCache implements CreateOrderResultCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCreateOrderResultCache.class);
    private static final String KEY_PREFIX = "book:create:result:";
    private static final long POLL_INTERVAL_NANOS = 20_000_000L;
    private final RedisClient redisClient;

    public DefaultCreateOrderResultCache(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override public BookOrderResponse get(String requestId) {
        try {
            return redisClient.get(key(requestId));
        } catch (RedisClientException redisFailure) {
            LOGGER.warn("Create-order result cache unavailable, requestId={}", requestId,
                    redisFailure);
            return null;
        }
    }

    @Override public void put(String requestId, BookOrderResponse response, long ttlMillis) {
        try {
            redisClient.set(key(requestId), response, ttlMillis);
        } catch (RedisClientException redisFailure) {
            // 缓存写失败不回滚已经提交的订单，后续请求仍可在锁内查询数据库回填。
            LOGGER.warn("Failed to cache create-order result, requestId={}", requestId,
                    redisFailure);
        }
    }

    @Override public BookOrderResponse await(String requestId, long waitMillis) {
        long deadlineNanos = System.nanoTime() + waitMillis * 1_000_000L;
        do {
            BookOrderResponse response = get(requestId);
            if (response != null) return response;
            if (Thread.currentThread().isInterrupted()) return null;
            LockSupport.parkNanos(POLL_INTERVAL_NANOS);
        } while (System.nanoTime() < deadlineNanos);
        return get(requestId);
    }

    private static String key(String requestId) { return KEY_PREFIX + requestId; }
}
