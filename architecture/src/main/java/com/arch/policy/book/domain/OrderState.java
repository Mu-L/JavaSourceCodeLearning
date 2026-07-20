package com.arch.policy.book.domain;

public enum OrderState {
    CREATE(0, "创单"),
    WAIT_PAY(1, "待支付"),
    CREATE_FAIL(2, "创单失败"),
    CANCEL(3, "已取消"),
    BOOKING(4, "预定中"),
    BOOKED(5, "已预定"),
    BOOK_FAIL(6, "预定失败"),
    DELETED(7, "已删除"),
    REFUNDED(8, "已退订"),
    VALIDATING(10, "验真中"),
    VALIDATE_FAIL(11, "验真失败");

    private final int code;
    private final String description;

    OrderState(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
