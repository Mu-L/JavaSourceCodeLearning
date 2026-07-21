package com.arch.policy.book.application;

import java.util.HashMap;
import java.util.Map;

public final class CompensationTaskService {
    private final RecoveryTaskStore taskStore;

    public CompensationTaskService(RecoveryTaskStore taskStore) { this.taskStore = taskStore; }

    public void returnPromotionStock(String orderNo, String promotionId) {
        if (isBlank(promotionId)) return;
        enqueue("PROMOTION:" + orderNo + ":" + promotionId,
                RecoveryTask.RecoveryType.RETURN_PROMOTION_STOCK, orderNo,
                params("promotionId", promotionId));
    }

    public void cancelPnr(String orderNo, String childOrderNo, String pnr) {
        if (isBlank(pnr)) return;
        Map<String, String> parameters = params("childOrderNo", childOrderNo);
        parameters.put("pnr", pnr);
        enqueue("CANCEL_PNR:" + orderNo + ":" + childOrderNo + ":" + pnr,
                RecoveryTask.RecoveryType.CANCEL_PNR, orderNo, parameters);
    }

    public void refundPayment(String orderNo, String paymentNo) {
        if (isBlank(paymentNo)) return;
        enqueue("REFUND:" + orderNo + ":" + paymentNo,
                RecoveryTask.RecoveryType.REFUND_PAYMENT, orderNo,
                params("paymentNo", paymentNo));
    }

    public void validateGdsBooking(String orderNo) {
        enqueue("VALIDATE_GDS:" + orderNo,
                RecoveryTask.RecoveryType.VALIDATE_GDS_BOOKING, orderNo,
                new HashMap<String, String>());
    }

    private void enqueue(String uniqueKey, RecoveryTask.RecoveryType type, String orderNo,
                         Map<String, String> parameters) {
        taskStore.saveIfAbsent(new RecoveryTask(uniqueKey, type, orderNo, parameters));
    }

    private static Map<String, String> params(String name, String value) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(name, value == null ? "" : value);
        return params;
    }

    private static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
