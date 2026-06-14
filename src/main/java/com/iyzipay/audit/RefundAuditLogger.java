package com.iyzipay.audit;

import java.util.List;

public interface RefundAuditLogger {

    void log(RefundAuditEvent event);

    List<RefundAuditEvent> getEventsByIdempotencyKey(String idempotencyKey);

    List<RefundAuditEvent> getEventsByConversationId(String conversationId);

    List<RefundAuditEvent> getAllEvents();

    void clear();
}
