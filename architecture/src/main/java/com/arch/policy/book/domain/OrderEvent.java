package com.arch.policy.book.domain;

public enum OrderEvent {
    CREATE_SUCCEEDED,
    CREATE_FAILED,
    PAY_SUCCEEDED,
    BOOK_SUCCEEDED,
    BOOK_FAILED,
    START_VALIDATE,
    VALIDATE_SUCCEEDED,
    VALIDATE_FAILED,
    CANCEL,
    REFUND_SUCCEEDED,
    DELETE
}
