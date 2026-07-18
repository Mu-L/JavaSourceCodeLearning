package com.arch.policy.redis;

import com.arch.policy.aggregation.LocalSearchWaiters;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

public final class SearchFinishedSubscriber implements MessageListener {
    private final LocalSearchWaiters waiters;
    public SearchFinishedSubscriber(LocalSearchWaiters waiters) { this.waiters = waiters; }
    @Override public void onMessage(Message message, byte[] pattern) {
        waiters.signal(new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
