package com.arch.policy.book.infrastructure.job;

import com.arch.policy.book.application.RecoveryTaskProcessor;
import org.springframework.scheduling.annotation.Scheduled;

public final class OrderRecoveryScheduler {
    private final RecoveryTaskProcessor processor;

    public OrderRecoveryScheduler(RecoveryTaskProcessor processor) { this.processor = processor; }

    @Scheduled(fixedDelayString = "${book.recovery.scan-delay-ms:5000}")
    public void scan() { processor.processBatch(System.currentTimeMillis(), 100); }
}
