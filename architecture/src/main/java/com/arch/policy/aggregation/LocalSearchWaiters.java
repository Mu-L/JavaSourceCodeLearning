package com.arch.policy.aggregation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class LocalSearchWaiters {
    private final ConcurrentMap<String, Waiter> waiters = new ConcurrentHashMap<String, Waiter>();

    public Waiter register(String searchKey) {
        Waiter waiter = new Waiter();
        Waiter existing = waiters.putIfAbsent(searchKey, waiter);
        if (existing != null) throw new IllegalStateException("duplicate local search: " + searchKey);
        return waiter;
    }

    public void remove(String searchKey, Waiter waiter) { waiters.remove(searchKey, waiter); }

    public void signal(String searchKey) {
        Waiter waiter = waiters.get(searchKey);
        if (waiter != null) waiter.signal();
    }

    public static final class Waiter {
        private final CountDownLatch completed = new CountDownLatch(1);
        public void await(long millis) throws InterruptedException {
            completed.await(millis, TimeUnit.MILLISECONDS);
        }
        private void signal() { completed.countDown(); }
    }
}
