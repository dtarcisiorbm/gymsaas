package com.gymsaas.membership.application;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Requests never carry prices: only an optional discount, validated against
 * permission and the price resolved by the backend. Money is serialized as
 * decimal strings.
 */
public final class MembershipDtos {

    private MembershipDtos() {
    }

    public record MembershipFilter(String search, UUID studentId, UUID planId, UUID branchId, String status,
                                   LocalDate endingFrom, LocalDate endingTo, int page, int size, String sort,
                                   String direction) {
    }

    public record CreateMembershipRequest(
            @NotNull UUID studentId,
            @NotNull UUID planId,
            @NotNull UUID branchId,
            LocalDate startDate,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal discountAmount,
            @Size(max = 1000) String discountReason,
            @Size(max = 4000) String notes) {
    }

    public record UpdateMembershipRequest(
            @NotNull UUID branchId,
            @Size(max = 4000) String notes,
            Integer version) {
    }

    public record RenewMembershipRequest(
            LocalDate startDate,
            UUID branchId,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal discountAmount,
            @Size(max = 1000) String discountReason,
            @Size(max = 4000) String notes) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    public record OptionalReasonRequest(@Size(max = 1000) String reason) {
    }

    public record FreezeMembershipRequest(
            @NotNull LocalDate endDate,
            @NotBlank @Size(max = 1000) String reason) {
    }

    public record FreezePolicyView(Integer maxFreezeDays, Integer maxFreezes, Integer minFreezeDays) {
    }

    public record ConditionsView(
            String durationType,
            int durationValue,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal setupFee,
            String currency,
            String billingPeriod,
            int billingInterval,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal discountAmount,
            String discountReason,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal finalAmount,
            FreezePolicyView freezePolicy,
            Map<String, Object> snapshot) {
    }

    public record QuoteView(
            UUID studentId,
            String studentName,
            UUID planId,
            String planName,
            UUID planPriceId,
            UUID branchId,
            String branchName,
            LocalDate startDate,
            LocalDate endDate,
            String initialStatus,
            ConditionsView conditions) {
    }

    public record FreezeView(
            UUID id,
            LocalDate startDate,
            LocalDate plannedEndDate,
            LocalDate actualEndDate,
            int plannedDays,
            Integer effectiveDays,
            String status,
            String reason,
            Instant createdAt,
            Instant endedAt) {
    }

    public record MembershipSummary(
            UUID id,
            String registrationNumber,
            String status,
            UUID studentId,
            String studentName,
            String studentRegistration,
            UUID planId,
            String planName,
            UUID branchId,
            String branchName,
            LocalDate startDate,
            LocalDate endDate,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal finalAmount,
            String currency,
            String billingPeriod,
            Instant createdAt) {
    }

    public record MembershipDetail(
            UUID id,
            String registrationNumber,
            String status,
            UUID studentId,
            String studentName,
            String studentRegistration,
            UUID planId,
            String planName,
            UUID planPriceId,
            UUID branchId,
            String branchName,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate originalEndDate,
            LocalDate anchorDate,
            int termSequence,
            ConditionsView conditions,
            UUID renewedFromId,
            UUID renewedById,
            String notes,
            Instant cancelledAt,
            UUID cancelledBy,
            String cancellationReason,
            int freezesUsed,
            int frozenDaysUsed,
            List<FreezeView> freezes,
            int version,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record HistoryEntryView(
            UUID id,
            String type,
            String fromStatus,
            String toStatus,
            String reason,
            Map<String, Object> metadata,
            UUID changedBy,
            Instant changedAt) {
    }

    public record LifecycleResult(int activated, int unfrozen, int expired) {
    }
}
