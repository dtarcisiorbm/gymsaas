package com.gymsaas.billing.domain;

import com.gymsaas.shared.domain.PageResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargeRepository {

    Optional<Charge> findById(UUID tenantId, UUID chargeId);

    /** Row lock (SELECT ... FOR UPDATE) to serialize payments/refunds/cancellation of one charge. */
    Optional<Charge> lockById(UUID tenantId, UUID chargeId);

    List<Charge> findByMembership(UUID tenantId, UUID membershipId);

    /** OPEN charges of a membership due on or after {@code date}, locked for update. */
    List<Charge> lockOpenByMembershipDueFrom(UUID tenantId, UUID membershipId, LocalDate date);

    PageResult<Charge> search(UUID tenantId, ChargeSearchCriteria criteria);

    Charge insert(Charge charge);

    /** Persists the status fields (the only mutable ones). */
    Charge updateStatus(Charge charge);

    /**
     * Summary aggregates. Charges use business dates ({@code from..to}, {@code today});
     * payments use the instants {@code [receivedFrom, receivedTo)} of the same period in the tenant's zone.
     */
    BillingTotals totals(UUID tenantId, UUID branchId, LocalDate from, LocalDate to, LocalDate today,
                         Instant receivedFrom, Instant receivedTo);
}
