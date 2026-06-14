package com.iyzipay.idempotency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IdempotencyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private String idempotencyKey;
    private IdempotencyStatus status;
    private String responseJson;
    private int retryCount;
    private long createdAt;
    private long lastAttemptAt;
    private long completedAt;
    private List<Long> retryTimestamps;
    private String errorMessage;
    private String errorCode;
    private boolean requiresManualReview;

    public IdempotencyRecord() {
        this.retryTimestamps = new ArrayList<Long>();
        this.createdAt = System.currentTimeMillis();
        this.status = IdempotencyStatus.PENDING;
    }

    public IdempotencyRecord(String idempotencyKey) {
        this();
        this.idempotencyKey = idempotencyKey;
    }

    public synchronized void incrementRetry() {
        this.retryCount++;
        this.lastAttemptAt = System.currentTimeMillis();
        this.retryTimestamps.add(this.lastAttemptAt);
    }

    public synchronized void markProcessing() {
        this.status = IdempotencyStatus.PROCESSING;
        this.lastAttemptAt = System.currentTimeMillis();
    }

    public synchronized void markSuccess(String responseJson) {
        this.status = IdempotencyStatus.SUCCESS;
        this.responseJson = responseJson;
        this.completedAt = System.currentTimeMillis();
    }

    public synchronized void markFailed(String errorCode, String errorMessage) {
        this.status = IdempotencyStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.completedAt = System.currentTimeMillis();
    }

    public synchronized void markManualReviewRequired(String reason) {
        this.status = IdempotencyStatus.MANUAL_REVIEW_REQUIRED;
        this.requiresManualReview = true;
        this.errorMessage = reason;
        this.completedAt = System.currentTimeMillis();
    }

    public synchronized boolean canRetry(long minRetryIntervalMs) {
        if (status == IdempotencyStatus.SUCCESS || status == IdempotencyStatus.MANUAL_REVIEW_REQUIRED) {
            return false;
        }
        if (lastAttemptAt == 0) {
            return true;
        }
        return System.currentTimeMillis() - lastAttemptAt >= minRetryIntervalMs;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public void setStatus(IdempotencyStatus status) {
        this.status = status;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(long lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public List<Long> getRetryTimestamps() {
        return retryTimestamps;
    }

    public void setRetryTimestamps(List<Long> retryTimestamps) {
        this.retryTimestamps = retryTimestamps;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public boolean isRequiresManualReview() {
        return requiresManualReview;
    }

    public void setRequiresManualReview(boolean requiresManualReview) {
        this.requiresManualReview = requiresManualReview;
    }
}
