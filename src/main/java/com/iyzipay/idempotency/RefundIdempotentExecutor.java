package com.iyzipay.idempotency;

import com.google.gson.Gson;
import com.iyzipay.Options;
import com.iyzipay.audit.AuditEventType;
import com.iyzipay.audit.DefaultRefundAuditLogger;
import com.iyzipay.audit.RefundAuditEvent;
import com.iyzipay.audit.RefundAuditLogger;
import com.iyzipay.model.Refund;
import com.iyzipay.request.CreateRefundRequest;
import com.iyzipay.request.CreateRefundV2Request;

public class RefundIdempotentExecutor {

    private static volatile RefundIdempotentExecutor instance;

    private final IdempotencyCache cache;
    private final IdempotencyConfig config;
    private final RefundAuditLogger auditLogger;
    private final Gson gson;

    private RefundIdempotentExecutor() {
        this(IdempotencyConfig.defaultConfig(),
                new InMemoryIdempotencyCache(IdempotencyConfig.DEFAULT_CACHE_TTL_MS),
                new DefaultRefundAuditLogger());
    }

    private RefundIdempotentExecutor(IdempotencyConfig config, IdempotencyCache cache, RefundAuditLogger auditLogger) {
        this.config = config;
        this.cache = cache;
        this.auditLogger = auditLogger;
        this.gson = new Gson();
    }

    public static RefundIdempotentExecutor getInstance() {
        if (instance == null) {
            synchronized (RefundIdempotentExecutor.class) {
                if (instance == null) {
                    instance = new RefundIdempotentExecutor();
                }
            }
        }
        return instance;
    }

    public static void configure(IdempotencyConfig config, IdempotencyCache cache, RefundAuditLogger auditLogger) {
        synchronized (RefundIdempotentExecutor.class) {
            instance = new RefundIdempotentExecutor(config, cache, auditLogger);
        }
    }

    public static void reset() {
        synchronized (RefundIdempotentExecutor.class) {
            instance = null;
        }
    }

    public Refund executeRefundV1(CreateRefundRequest request, Options options) {
        String idempotencyKey = IdempotencyKeyGenerator.generateForRefundV1(request);
        request.setIdempotencyKey(idempotencyKey);
        return execute(idempotencyKey, request, options, new RefundOperation() {
            @Override
            public Refund execute(Options opts) {
                return Refund.create(request, opts);
            }
        }, "v1");
    }

    public Refund executeRefundV2(CreateRefundV2Request request, Options options) {
        String idempotencyKey = IdempotencyKeyGenerator.generateForRefundV2(request);
        request.setIdempotencyKey(idempotencyKey);
        return execute(idempotencyKey, request, options, new RefundOperation() {
            @Override
            public Refund execute(Options opts) {
                return Refund.createV2(request, opts);
            }
        }, "v2");
    }

    private Refund execute(String idempotencyKey, Object request, Options options,
                           RefundOperation operation, String version) {
        if (!config.isEnabled()) {
            return operation.execute(options);
        }

        logAuditEvent(AuditEventType.REFUND_IDEMPOTENCY_KEY_GENERATED, idempotencyKey, request, 0, null);

        IdempotencyRecord existingRecord = cache.get(idempotencyKey);

        if (existingRecord != null && existingRecord.getStatus() == IdempotencyStatus.SUCCESS) {
            logAuditEvent(AuditEventType.REFUND_CACHE_HIT, idempotencyKey, request,
                    existingRecord.getRetryCount(), existingRecord.getStatus().name());
            return gson.fromJson(existingRecord.getResponseJson(), Refund.class);
        }

        if (existingRecord != null) {
            return handleRetry(idempotencyKey, request, options, operation, version, existingRecord);
        }

        return handleFirstAttempt(idempotencyKey, request, options, operation, version);
    }

    private Refund handleFirstAttempt(String idempotencyKey, Object request, Options options,
                                      RefundOperation operation, String version) {
        IdempotencyRecord newRecord = new IdempotencyRecord(idempotencyKey);
        IdempotencyRecord existing = cache.putIfAbsent(idempotencyKey, newRecord);

        if (existing != null) {
            return execute(idempotencyKey, request, options, operation, version);
        }

        logAuditEvent(AuditEventType.REFUND_REQUEST_INITIATED, idempotencyKey, request, 0, null);
        logAuditEvent(AuditEventType.REFUND_CACHE_MISS, idempotencyKey, request, 0, null);

        return doExecute(idempotencyKey, request, options, operation, newRecord);
    }

    private Refund handleRetry(String idempotencyKey, Object request, Options options,
                               RefundOperation operation, String version, IdempotencyRecord record) {
        if (!record.canRetry(config.getMinRetryIntervalMs())) {
            logAuditEvent(AuditEventType.REFUND_THROTTLED, idempotencyKey, request,
                    record.getRetryCount(), record.getStatus().name());
            if (record.getStatus() == IdempotencyStatus.FAILED && record.getResponseJson() != null) {
                return gson.fromJson(record.getResponseJson(), Refund.class);
            }
            throw new IdempotencyThrottledException(
                    "Refund request throttled. Next retry available after " +
                            (config.getMinRetryIntervalMs() - (System.currentTimeMillis() - record.getLastAttemptAt())) + "ms",
                    idempotencyKey, record.getRetryCount());
        }

        if (record.getRetryCount() >= config.getMaxRetryCount()) {
            if (record.getRetryCount() >= config.getManualReviewThreshold()) {
                record.markManualReviewRequired("Exceeded manual review threshold: " +
                        record.getRetryCount() + " >= " + config.getManualReviewThreshold());
                cache.put(idempotencyKey, record);
                logAuditEvent(AuditEventType.REFUND_MANUAL_REVIEW_TRIGGERED, idempotencyKey, request,
                        record.getRetryCount(), record.getStatus().name());
                throw new ManualReviewRequiredException(
                        "Refund requires manual review after " + record.getRetryCount() + " retries",
                        idempotencyKey, record.getRetryCount());
            }
        }

        logAuditEvent(AuditEventType.REFUND_REQUEST_RETRIED, idempotencyKey, request,
                record.getRetryCount() + 1, record.getStatus().name());

        record.incrementRetry();
        cache.put(idempotencyKey, record);

        return doExecute(idempotencyKey, request, options, operation, record);
    }

    private Refund doExecute(String idempotencyKey, Object request, Options options,
                             RefundOperation operation, IdempotencyRecord record) {
        record.markProcessing();
        cache.put(idempotencyKey, record);

        try {
            Refund result = operation.execute(options);
            String responseJson = gson.toJson(result);

            if (result.getStatus() != null && "success".equalsIgnoreCase(result.getStatus())) {
                record.markSuccess(responseJson);
                logAuditEvent(AuditEventType.REFUND_SUCCESS, idempotencyKey, request,
                        record.getRetryCount(), "success");
            } else {
                record.markFailed(result.getErrorCode(), result.getErrorMessage());
                record.setResponseJson(responseJson);
                logAuditEvent(AuditEventType.REFUND_FAILED, idempotencyKey, request,
                        record.getRetryCount(), "failed");
            }

            cache.put(idempotencyKey, record);
            logAuditEvent(AuditEventType.REFUND_RESPONSE_CACHED, idempotencyKey, request,
                    record.getRetryCount(), record.getStatus().name());

            return result;

        } catch (RuntimeException e) {
            record.markFailed(null, e.getMessage());
            cache.put(idempotencyKey, record);
            logAuditEvent(AuditEventType.REFUND_FAILED, idempotencyKey, request,
                    record.getRetryCount(), "error");
            throw e;
        }
    }

    private void logAuditEvent(AuditEventType eventType, String idempotencyKey, Object request,
                               int retryCount, String status) {
        RefundAuditEvent event = new RefundAuditEvent(eventType);
        event.setIdempotencyKey(idempotencyKey);
        event.setRetryCount(retryCount);
        event.setStatus(status);

        if (request instanceof CreateRefundRequest) {
            CreateRefundRequest req = (CreateRefundRequest) request;
            event.setConversationId(req.getConversationId());
            event.setPaymentTransactionId(req.getPaymentTransactionId());
            event.setPrice(req.getPrice());
            event.setCurrency(req.getCurrency());
        } else if (request instanceof CreateRefundV2Request) {
            CreateRefundV2Request req = (CreateRefundV2Request) request;
            event.setConversationId(req.getConversationId());
            event.setPaymentId(req.getPaymentId());
            event.setPrice(req.getPrice());
        }

        event.setRequestJson(gson.toJson(request));
        auditLogger.log(event);
    }

    public IdempotencyRecord getRecord(String idempotencyKey) {
        return cache.get(idempotencyKey);
    }

    public IdempotencyCache getCache() {
        return cache;
    }

    public IdempotencyConfig getConfig() {
        return config;
    }

    public RefundAuditLogger getAuditLogger() {
        return auditLogger;
    }

    private interface RefundOperation {
        Refund execute(Options options);
    }
}
