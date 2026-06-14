package com.iyzipay.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultRefundAuditLogger implements RefundAuditLogger {

    private final List<RefundAuditEvent> events;

    public DefaultRefundAuditLogger() {
        this.events = new CopyOnWriteArrayList<RefundAuditEvent>();
    }

    @Override
    public void log(RefundAuditEvent event) {
        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        events.add(event);
    }

    @Override
    public List<RefundAuditEvent> getEventsByIdempotencyKey(String idempotencyKey) {
        List<RefundAuditEvent> result = new ArrayList<RefundAuditEvent>();
        for (RefundAuditEvent event : events) {
            if (idempotencyKey != null && idempotencyKey.equals(event.getIdempotencyKey())) {
                result.add(event);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<RefundAuditEvent> getEventsByConversationId(String conversationId) {
        List<RefundAuditEvent> result = new ArrayList<RefundAuditEvent>();
        for (RefundAuditEvent event : events) {
            if (conversationId != null && conversationId.equals(event.getConversationId())) {
                result.add(event);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<RefundAuditEvent> getAllEvents() {
        return Collections.unmodifiableList(new ArrayList<RefundAuditEvent>(events));
    }

    @Override
    public void clear() {
        events.clear();
    }

    public int size() {
        return events.size();
    }
}
