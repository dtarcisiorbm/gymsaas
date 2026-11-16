package com.gymsaas.contract.application;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ContractDtos {

    private ContractDtos() {
    }

    /**
     * contentSha256 is the hash of the content the operator displayed to the
     * student; acceptance fails if the stored content differs.
     */
    public record RegisterAcceptanceRequest(
            @NotBlank @Size(max = 200) String acceptorName,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}", message = "Hash do conteúdo inválido.") String contentSha256,
            @NotNull @AssertTrue(message = "Confirme que o aluno leu e aceitou o conteúdo.") Boolean confirmed) {
    }

    public record AcceptanceView(
            UUID id,
            Instant acceptedAt,
            String channel,
            String acceptorName,
            String acceptorDocumentMasked,
            String contentSha256,
            String declaration,
            String ipAddress,
            String userAgent,
            UUID registeredBy) {
    }

    public record ContractSummary(
            UUID id,
            UUID membershipId,
            int contractVersion,
            String templateVersion,
            String status,
            String contentSha256,
            String documentSha256,
            long documentSizeBytes,
            String documentContentType,
            UUID generatedBy,
            Instant generatedAt,
            Instant statusChangedAt,
            AcceptanceView acceptance) {
    }

    public record ContractView(
            UUID id,
            UUID membershipId,
            int contractVersion,
            String templateVersion,
            String status,
            String content,
            String contentSha256,
            String documentSha256,
            long documentSizeBytes,
            String documentContentType,
            UUID generatedBy,
            Instant generatedAt,
            Instant statusChangedAt,
            AcceptanceView acceptance,
            String legalNotice) {
    }

    public record DocumentFile(byte[] content, String filename, String contentType) {
    }
}
