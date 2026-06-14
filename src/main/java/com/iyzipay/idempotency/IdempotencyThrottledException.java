package com.iyzipay.idempotency;

public class IdempotencyThrottledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String idempotencyKey;
    private final int retryCount;

    public IdempotencyThrottledException(String message, String idempotencyKey, int retryCount) {
        super(message);
        this.idempotencyKey = idempotencyKey;
        this.retryCount = retryCount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
