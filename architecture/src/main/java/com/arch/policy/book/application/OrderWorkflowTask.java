package com.arch.policy.book.application;

public final class OrderWorkflowTask {
    private final String uniqueKey;
    private final String orderNo;
    private final TaskType type;

    public OrderWorkflowTask(String uniqueKey, String orderNo, TaskType type) {
        this.uniqueKey = uniqueKey;
        this.orderNo = orderNo;
        this.type = type;
    }

    public String getUniqueKey() { return uniqueKey; }
    public String getOrderNo() { return orderNo; }
    public TaskType getType() { return type; }

    public enum TaskType { WAIT_PAYMENT, MANUAL_GDS_VALIDATION }
}
