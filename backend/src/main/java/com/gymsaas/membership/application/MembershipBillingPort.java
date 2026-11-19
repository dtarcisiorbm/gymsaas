package com.gymsaas.membership.application;

import com.gymsaas.membership.domain.Membership;

import java.time.LocalDate;

/**
 * Financial side effects of the membership lifecycle (Phase 6). Implemented by
 * the billing module so the membership module does not depend on it. Calls run
 * inside the membership transaction: a billing failure rolls the change back.
 */
public interface MembershipBillingPort {

    /** Generates the term's installments (and the setup fee when {@code renewal} is false). */
    void onContracted(Membership membership, boolean renewal);

    /** Cancels OPEN charges due on or after {@code effectiveDate}; overdue ones remain as debt. */
    void onCancelled(Membership membership, LocalDate effectiveDate, String reason);
}
