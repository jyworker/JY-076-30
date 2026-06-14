package com.iyzipay.idempotency;

public class IdempotencyConfig {

    public static final long DEFAULT_MIN_RETRY_INTERVAL_MS = 1000L;
    public static final int DEFAULT_MAX_RETRY_COUNT = 5;
    public static final int DEFAULT_MANUAL_REVIEW_THRESHOLD = 3;
    public static final long DEFAULT_CACHE_TTL_MS = 24 * 60 * 60 * 1000L;

    private long minRetryIntervalMs;
    private int maxRetryCount;
    private int manualReviewThreshold;
    private long cacheTtlMs;
    private boolean enabled;

    public IdempotencyConfig() {
        this.minRetryIntervalMs = DEFAULT_MIN_RETRY_INTERVAL_MS;
        this.maxRetryCount = DEFAULT_MAX_RETRY_COUNT;
        this.manualReviewThreshold = DEFAULT_MANUAL_REVIEW_THRESHOLD;
        this.cacheTtlMs = DEFAULT_CACHE_TTL_MS;
        this.enabled = true;
    }

    public static IdempotencyConfig defaultConfig() {
        return new IdempotencyConfig();
    }

    public static IdempotencyConfig disabled() {
        IdempotencyConfig config = new IdempotencyConfig();
        config.setEnabled(false);
        return config;
    }

    public long getMinRetryIntervalMs() {
        return minRetryIntervalMs;
    }

    public IdempotencyConfig setMinRetryIntervalMs(long minRetryIntervalMs) {
        this.minRetryIntervalMs = minRetryIntervalMs;
        return this;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public IdempotencyConfig setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }

    public int getManualReviewThreshold() {
        return manualReviewThreshold;
    }

    public IdempotencyConfig setManualReviewThreshold(int manualReviewThreshold) {
        this.manualReviewThreshold = manualReviewThreshold;
        return this;
    }

    public long getCacheTtlMs() {
        return cacheTtlMs;
    }

    public IdempotencyConfig setCacheTtlMs(long cacheTtlMs) {
        this.cacheTtlMs = cacheTtlMs;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public IdempotencyConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
