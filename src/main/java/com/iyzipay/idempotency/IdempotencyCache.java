package com.iyzipay.idempotency;

public interface IdempotencyCache {

    IdempotencyRecord get(String idempotencyKey);

    IdempotencyRecord putIfAbsent(String idempotencyKey, IdempotencyRecord record);

    void put(String idempotencyKey, IdempotencyRecord record);

    boolean remove(String idempotencyKey);

    boolean containsKey(String idempotencyKey);

    void clearExpired();
}
