package com.arch.policy.order.cache;

import com.arch.policy.order.model.OrderResponse;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author : haiyang.luo
 * @Date : 2026/7/22 15:58
 * @Description :
 */
@Component
public class OrderCacheUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCacheUtil.class);

    @Autowired
    private RedissonClient redissonClient;

    public OrderResponse getCache(String orderSerialNo) {
        try {
            return redissonClient.<OrderResponse>getBucket(cacheKey(orderSerialNo)).get();
        } catch (RedisException redisFailure) {
            LOGGER.warn("Create-order result cache unavailable, orderSerialNo={}", orderSerialNo,
                    redisFailure);
            return null;
        }
    }

    public void setCache(String orderSerialNo, OrderResponse response) {
        try {
            redissonClient.getBucket(cacheKey(orderSerialNo)).set(response, 600_000L,
                    java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (RedisException redisFailure) {
            LOGGER.warn("Create-order result cache unavailable, orderSerialNo={}", orderSerialNo,
                    redisFailure);
        }
    }

    private String cacheKey(String orderSerialNo) {
        return "order:create:result:" + orderSerialNo;
    }
}
