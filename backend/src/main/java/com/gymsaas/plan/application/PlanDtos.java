package com.gymsaas.plan.application;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Money is serialized as a decimal string to avoid floating point on clients. */
public final class PlanDtos {

    public static final String DURATION_TYPES = "MONTHLY|QUARTERLY|SEMIANNUAL|ANNUAL|CUSTOM_MONTHS|CUSTOM_DAYS";
    public static final String BILLING_PERIODS = "MONTHLY|QUARTERLY|SEMIANNUAL|ANNUAL|ONE_TIME";

    private PlanDtos() {
    }

    public record PlanFilter(String search, String status, int page, int size, String sort, String direction) {
    }

    public record FreezePolicyRequest(
            @Positive Integer maxFreezeDays,
            @PositiveOrZero Integer maxFreezes,
            @Positive Integer minFreezeDays) {
    }

    public record PriceRequest(
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @Pattern(regexp = "[A-Za-z]{3}", message = "Moeda inválida.") String currency,
            @NotBlank @Pattern(regexp = BILLING_PERIODS, message = "Periodicidade de cobrança inválida.")
            String billingPeriod,
            @Min(1) @Max(36) Integer billingInterval,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal setupFee,
            LocalDate validFrom,
            @Size(max = 1000) String notes) {
    }

    public record CreatePlanRequest(
            @NotBlank @Size(max = 30)
            @Pattern(regexp = "[A-Za-z0-9_-]+", message = "Código aceita letras, números, - e _.") String code,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 4000) String description,
            @NotBlank @Pattern(regexp = DURATION_TYPES, message = "Tipo de duração inválido.") String durationType,
            @Min(1) @Max(3650) Integer durationValue,
            @Valid FreezePolicyRequest freezePolicy,
            @Size(max = 20000) String contractTerms,
            @Valid PriceRequest initialPrice) {
    }

    public record UpdatePlanRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 4000) String description,
            @NotBlank @Pattern(regexp = DURATION_TYPES, message = "Tipo de duração inválido.") String durationType,
            @Min(1) @Max(3650) Integer durationValue,
            @Valid FreezePolicyRequest freezePolicy,
            @Size(max = 20000) String contractTerms,
            Integer version) {
    }

    public record ChangePlanStatusRequest(
            @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED", message = "Status inválido.") String status) {
    }

    public record CancelPriceRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    public record FeatureRequest(
            @NotBlank String type,
            UUID branchId,
            @Positive Integer intValue,
            LocalTime startTime,
            LocalTime endTime,
            @Min(1) @Max(127) Integer daysOfWeek,
            @Size(max = 150) String label,
            @Size(max = 1000) String description) {
    }

    public record ReplaceFeaturesRequest(@NotNull @Size(max = 50) @Valid List<FeatureRequest> features) {
    }

    public record FreezePolicyView(Integer maxFreezeDays, Integer maxFreezes, Integer minFreezeDays) {
    }

    public record PriceView(
            UUID id,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            String currency,
            String billingPeriod,
            int billingInterval,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal setupFee,
            LocalDate validFrom,
            LocalDate validUntil,
            String status,
            String notes,
            boolean current,
            boolean scheduled,
            Instant createdAt) {
    }

    public record FeatureView(
            UUID id,
            String type,
            UUID branchId,
            String branchName,
            Integer intValue,
            LocalTime startTime,
            LocalTime endTime,
            Integer daysOfWeek,
            String label,
            String description,
            int sortOrder) {
    }

    public record PlanSummary(
            UUID id,
            String code,
            String name,
            String durationType,
            int durationValue,
            String status,
            PriceView currentPrice,
            Instant updatedAt) {
    }

    public record PlanDetail(
            UUID id,
            String code,
            String name,
            String description,
            String durationType,
            int durationValue,
            String status,
            FreezePolicyView freezePolicy,
            String contractTerms,
            int version,
            PriceView currentPrice,
            List<FeatureView> features,
            Instant createdAt,
            Instant updatedAt) {
    }
}
