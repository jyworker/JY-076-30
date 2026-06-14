package com.iyzipay.idempotency;

public enum IdempotencyStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    MANUAL_REVIEW_REQUIRED
}
