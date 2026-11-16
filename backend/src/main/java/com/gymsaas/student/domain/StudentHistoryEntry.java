package com.gymsaas.student.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Unified timeline entry assembled from status changes, branch movements and
 * administrative notes. It is a read model, not a persisted table.
 */
public record StudentHistoryEntry(
        Instant occurredAt,
        String kind,
        String title,
        String detail,
        UUID actorId) {
}
