package com.gymsaas.billing.application;

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
import java.util.UUID;

/** Money travels as decimal strings; dates are business dates (no zone conversion). */
public final class BillingDtos {

    private BillingDtos() {
    }

    public record ChargeFilter(String search, String status, UUID studentId, UUID membershipId, UUID branchId,
                               LocalDate dueFrom, LocalDate dueTo, int page, int size, String sort,
                               String direction) {
    }

    public record CreateChargeRequest(
            @NotNull UUID studentId,
            @NotNull UUID branchId,
            UUID membershipId,
            @NotBlank @Size(max = 300) String description,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull LocalDate dueDate,
            @Size(max = 1000) String notes) {
    }

    public record RegisterPaymentRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotBlank String method,
            Instant paidAt,
            @Size(max = 200) String reference,
            @Size(max = 1000) String notes) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    public record ChargeView(
            UUID id,
            String chargeNumber,
            String kind,
            String status,
            boolean overdue,
            UUID studentId,
            String studentName,
            String studentRegistration,
            UUID membershipId,
            String membershipRegistrationNumber,
            UUID branchId,
            String branchName,
            Integer installmentNumber,
            Integer installmentCount,
            String description,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            String currency,
            LocalDate dueDate,
            LocalDate periodStart,
            LocalDate periodEnd,
            Instant paidAt,
            Instant cancelledAt,
            UUID cancelledBy,
            String cancellationReason,
            String notes,
            UUID createdBy,
            Instant createdAt) {
    }

    public record PaymentView(
            UUID id,
            String paymentNumber,
            UUID chargeId,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            String currency,
            String method,
            Instant paidAt,
            String reference,
            String notes,
            String status,
            UUID receivedBy,
            Instant refundedAt,
            UUID refundedBy,
            String refundReason,
            Instant createdAt) {
    }

    public record ChargeDetail(ChargeView charge, List<PaymentView> payments) {
    }

    public record AmountView(long count, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total) {
    }

    public record MethodAmountView(String method, long count,
                                   @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total) {
    }

    public record SummaryView(
            LocalDate from,
            LocalDate to,
            LocalDate today,
            UUID branchId,
            String currency,
            AmountView receivable,
            AmountView overdue,
            AmountView received,
            List<MethodAmountView> receivedByMethod,
            AmountView refunded) {
    }
}
