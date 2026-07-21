package com.arch.policy.common.lock;

public interface DistributedLock {
    /**
     * 尝试获取指定 key 对应的分布式锁。
     *
     * @return 锁句柄；等待超时返回 null
     */
    LockHandle tryAcquire(String key, long waitMillis);

    interface LockHandle extends AutoCloseable {
        @Override void close();
    }
}
