package com.arch.policy.book.application;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RecoveryTask {
    private final String uniqueKey;
    private final RecoveryType type;
    private final String orderNo;
    private final Map<String, String> parameters;
    private RecoveryStatus status;
    private int attempts;
    private long nextAttemptAtMillis;
    private String lastError;

    public RecoveryTask(String uniqueKey, RecoveryType type, String orderNo,
                        Map<String, String> parameters) {
        this.uniqueKey = uniqueKey;
        this.type = type;
        this.orderNo = orderNo;
        this.parameters = parameters == null ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, String>(parameters));
        this.status = RecoveryStatus.PENDING;
    }

    public RecoveryTask copy() {
        RecoveryTask copy = new RecoveryTask(uniqueKey, type, orderNo, parameters);
        copy.status = status;
        copy.attempts = attempts;
        copy.nextAttemptAtMillis = nextAttemptAtMillis;
        copy.lastError = lastError;
        return copy;
    }

    public void markSucceeded() { status = RecoveryStatus.SUCCEEDED; lastError = null; }
    public void markRetry(long nextAttemptAtMillis, String error) {
        attempts++;
        status = RecoveryStatus.RETRYING;
        this.nextAttemptAtMillis = nextAttemptAtMillis;
        lastError = error;
    }
    public void markManualRequired(String error) {
        attempts++;
        status = RecoveryStatus.MANUAL_REQUIRED;
        lastError = error;
    }

    public String getUniqueKey() { return uniqueKey; }
    public RecoveryType getType() { return type; }
    public String getOrderNo() { return orderNo; }
    public Map<String, String> getParameters() { return parameters; }
    public RecoveryStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public long getNextAttemptAtMillis() { return nextAttemptAtMillis; }
    public String getLastError() { return lastError; }

    public enum RecoveryType {
        RETURN_PROMOTION_STOCK,
        CANCEL_PNR,
        REFUND_PAYMENT,
        VALIDATE_GDS_BOOKING
    }

    public enum RecoveryStatus { PENDING, RETRYING, SUCCEEDED, MANUAL_REQUIRED }
}
