package com.iyzipay.idempotency;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryIdempotencyCache implements IdempotencyCache {

    private final ConcurrentMap<String, IdempotencyRecord> cache;
    private final long ttlMs;

    public InMemoryIdempotencyCache() {
        this(24 * 60 * 60 * 1000L);
    }

    public InMemoryIdempotencyCache(long ttlMs) {
        this.cache = new ConcurrentHashMap<String, IdempotencyRecord>();
        this.ttlMs = ttlMs;
    }

    @Override
    public IdempotencyRecord get(String idempotencyKey) {
        IdempotencyRecord record = cache.get(idempotencyKey);
        if (record != null && isExpired(record)) {
            cache.remove(idempotencyKey);
            return null;
        }
        return record;
    }

    @Override
    public IdempotencyRecord putIfAbsent(String idempotencyKey, IdempotencyRecord record) {
        return cache.putIfAbsent(idempotencyKey, record);
    }

    @Override
    public void put(String idempotencyKey, IdempotencyRecord record) {
        cache.put(idempotencyKey, record);
    }

    @Override
    public boolean remove(String idempotencyKey) {
        return cache.remove(idempotencyKey) != null;
    }

    @Override
    public boolean containsKey(String idempotencyKey) {
        IdempotencyRecord record = cache.get(idempotencyKey);
        if (record != null && isExpired(record)) {
            cache.remove(idempotencyKey);
            return false;
        }
        return record != null;
    }

    @Override
    public void clearExpired() {
        Iterator<IdempotencyRecord> iterator = cache.values().iterator();
        while (iterator.hasNext()) {
            IdempotencyRecord record = iterator.next();
            if (isExpired(record)) {
                iterator.remove();
            }
        }
    }

    private boolean isExpired(IdempotencyRecord record) {
        long lastActivity = Math.max(record.getLastAttemptAt(), record.getCompletedAt());
        if (lastActivity == 0) {
            lastActivity = record.getCreatedAt();
        }
        return System.currentTimeMillis() - lastActivity > ttlMs;
    }

    public int size() {
        return cache.size();
    }
}
