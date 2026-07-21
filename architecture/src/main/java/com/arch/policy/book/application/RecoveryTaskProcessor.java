package com.arch.policy.book.application;

import java.util.List;

public final class RecoveryTaskProcessor {
    private static final int MAX_ATTEMPTS = 5;
    private final RecoveryTaskStore taskStore;
    private final PromotionStockGateway promotionStockGateway;
    private final GdsBookingGateway gdsGateway;
    private final PaymentGateway paymentGateway;
    private final OrderReconciler reconciliationService;

    public RecoveryTaskProcessor(RecoveryTaskStore taskStore,
                                 PromotionStockGateway promotionStockGateway,
                                 GdsBookingGateway gdsGateway,
                                 PaymentGateway paymentGateway,
                                 OrderReconciler reconciliationService) {
        this.taskStore = taskStore;
        this.promotionStockGateway = promotionStockGateway;
        this.gdsGateway = gdsGateway;
        this.paymentGateway = paymentGateway;
        this.reconciliationService = reconciliationService;
    }

    public void processBatch(long nowMillis, int limit) {
        List<RecoveryTask> tasks = taskStore.findExecutable(nowMillis, limit);
        for (RecoveryTask task : tasks) process(task, nowMillis);
    }

    private void process(RecoveryTask task, long nowMillis) {
        try {
            execute(task);
            task.markSucceeded();
        } catch (RuntimeException failure) {
            if (task.getAttempts() + 1 >= MAX_ATTEMPTS) {
                task.markManualRequired(failure.getMessage());
            } else {
                task.markRetry(nowMillis + retryDelayMillis(task.getAttempts()), failure.getMessage());
            }
        }
        taskStore.save(task);
    }

    private void execute(RecoveryTask task) {
        switch (task.getType()) {
            case RETURN_PROMOTION_STOCK:
                promotionStockGateway.returnStock(task.getOrderNo(), task.getParameters().get("promotionId"));
                return;
            case CANCEL_PNR:
                gdsGateway.cancelPnr(task.getOrderNo(), task.getParameters().get("childOrderNo"),
                        task.getParameters().get("pnr"));
                return;
            case REFUND_PAYMENT:
                paymentGateway.refund(task.getOrderNo(), task.getParameters().get("paymentNo"));
                return;
            case VALIDATE_GDS_BOOKING:
                reconciliationService.reconcile(task.getOrderNo());
                return;
            default:
                throw new IllegalStateException("unsupported recovery type: " + task.getType());
        }
    }

    private static long retryDelayMillis(int attempts) {
        return Math.min(60_000L, 1_000L << Math.min(attempts, 6));
    }
}
