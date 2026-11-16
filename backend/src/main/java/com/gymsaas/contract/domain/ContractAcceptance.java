package com.gymsaas.contract.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Evidence of a SIMPLE acceptance: who accepted (name, masked document),
 * when, through which channel, from which client (IP / user agent), which
 * exact content (SHA-256) and which operator registered it. It is not a
 * qualified digital signature.
 */
public record ContractAcceptance(
        UUID id,
        UUID tenantId,
        UUID contractId,
        UUID membershipId,
        UUID studentId,
        Instant acceptedAt,
        AcceptanceChannel channel,
        String acceptorName,
        String acceptorDocumentMasked,
        String contentSha256,
        String declaration,
        String ipAddress,
        String userAgent,
        UUID registeredBy,
        Instant createdAt) {
}
