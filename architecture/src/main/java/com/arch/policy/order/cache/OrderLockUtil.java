package com.arch.policy.order.cache;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class OrderLockUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderLockUtil.class);

    @Resource
    private RedissonClient redissonClient;

    public boolean tryLock(String orderSerialNo) {
        RLock lock = redissonClient.getLock("order:create:lock:" + orderSerialNo);
        try {
            // 不设置固定过期时间，使用Redisson watchdog自动续期；只锁本地建单，不锁三方调用。
            return lock.tryLock(300L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        } catch (RedisException redisFailure) {
            LOGGER.warn("Create-order lock unavailable, orderSerialNo={}", orderSerialNo,
                    redisFailure);
            // Redis故障时继续执行，由订单库唯一索引保证最终幂等。
            return true;
        }
    }

    public void unlock(String orderSerialNo) {
        RLock lock = redissonClient.getLock("order:create:lock:" + orderSerialNo);
        try {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        } catch (RedisException redisFailure) {
            LOGGER.warn("Release create-order lock failed, orderSerialNo={}", orderSerialNo,
                    redisFailure);
        }
    }
}
