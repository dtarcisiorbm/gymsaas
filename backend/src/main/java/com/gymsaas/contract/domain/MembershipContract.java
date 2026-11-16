package com.gymsaas.contract.domain;

import java.time.Instant;
import java.util.UUID;

/** Immutable contract version; only the status may change (see V12 trigger). */
public record MembershipContract(
        UUID id,
        UUID tenantId,
        UUID membershipId,
        int contractVersion,
        String templateVersion,
        ContractStatus status,
        String content,
        String contentSha256,
        String storageKey,
        String documentSha256,
        long documentSizeBytes,
        String documentContentType,
        UUID generatedBy,
        Instant generatedAt,
        Instant statusChangedAt) {

    public MembershipContract withStatus(ContractStatus target, Instant at) {
        if (status != ContractStatus.GENERATED || target == ContractStatus.GENERATED) {
            throw new IllegalStateException("Invalid contract status transition " + status + " -> " + target);
        }
        return new MembershipContract(id, tenantId, membershipId, contractVersion, templateVersion, target, content,
                contentSha256, storageKey, documentSha256, documentSizeBytes, documentContentType, generatedBy,
                generatedAt, at);
    }
}
