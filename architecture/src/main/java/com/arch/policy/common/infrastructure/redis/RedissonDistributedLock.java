package com.arch.policy.common.infrastructure.redis;

import com.arch.policy.common.lock.DistributedLock;
import com.arch.policy.common.lock.DistributedLockUnavailableException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class RedissonDistributedLock implements DistributedLock {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedissonDistributedLock.class);
    private final RedissonClient redisson;

    public RedissonDistributedLock(RedissonClient redisson) { this.redisson = redisson; }

    @Override public LockHandle tryAcquire(String key, long waitMillis) {
        RLock lock = redisson.getLock(key);
        try {
            // 不指定 leaseTime，交由 Redisson watchdog 在持锁线程存活期间自动续期。
            boolean acquired = lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
            return acquired ? new RedissonLockHandle(lock) : null;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return null;
        } catch (RedisException redisFailure) {
            throw new DistributedLockUnavailableException(
                    "Redisson lock unavailable, key=" + key, redisFailure);
        }
    }

    private static final class RedissonLockHandle implements LockHandle {
        private final RLock lock;

        private RedissonLockHandle(RLock lock) { this.lock = lock; }

        @Override public void close() {
            try {
                if (lock.isHeldByCurrentThread()) lock.unlock();
            } catch (RedisException redisFailure) {
                LOGGER.warn("Failed to release Redisson lock, key={}", lock.getName(), redisFailure);
            }
        }
    }
}
