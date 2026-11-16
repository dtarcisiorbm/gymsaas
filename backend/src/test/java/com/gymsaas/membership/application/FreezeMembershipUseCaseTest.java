package com.gymsaas.membership.application;

import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.membership.application.usecase.FreezeMembershipUseCase;
import com.gymsaas.membership.domain.ContractedConditions;
import com.gymsaas.membership.domain.FreezeStatus;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipFreeze;
import com.gymsaas.membership.domain.MembershipFreezeRepository;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.plan.domain.PlanRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.Money;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.tenant.application.TenantCalendar;
import com.gymsaas.tenant.domain.TenantCalendarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Freeze uses the policy snapshotted on the membership and extends the end date (decision D3). */
@ExtendWith(MockitoExtension.class)
class FreezeMembershipUseCaseTest {

    private static final UUID TENANT = UUID.randomUUID();
    // 15:00 UTC = 12:00 in São Paulo on 2027-03-10.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2027-03-10T15:00:00Z"), ZoneOffset.UTC);

    @Mock private MembershipRepository memberships;
    @Mock private MembershipFreezeRepository freezes;
    @Mock private MembershipJournal journal;
    @Mock private MembershipDetailLoader detailLoader;
    @Mock private StudentRepository students;
    @Mock private PlanRepository plans;
    @Mock private BranchRepository branches;
    @Mock private TenantCalendarRepository calendarRepository;

    private FreezeMembershipUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(calendarRepository.findTimezone(any())).thenReturn(Optional.of("America/Sao_Paulo"));
        useCase = new FreezeMembershipUseCase(new MembershipGuard(memberships, students, plans, branches), memberships,
                freezes, journal, detailLoader, new TenantCalendar(calendarRepository, CLOCK));
    }

    @Test
    void freezeStartsTodayAndExtendsEndDateByPlannedDays() {
        Membership membership = membership(MembershipStatus.ACTIVE, new FreezePolicy(30, 2, 5));
        stubMembership(membership);
        when(freezes.findByMembership(TENANT, membership.id())).thenReturn(List.of());
        when(freezes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberships.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(TENANT, membership.id(), new MembershipDtos.FreezeMembershipRequest(
                LocalDate.of(2027, 3, 19), "Viagem"));

        ArgumentCaptor<MembershipFreeze> freeze = ArgumentCaptor.forClass(MembershipFreeze.class);
        verify(freezes).save(freeze.capture());
        assertEquals(LocalDate.of(2027, 3, 10), freeze.getValue().startDate());
        assertEquals(10, freeze.getValue().plannedDays());

        ArgumentCaptor<Membership> saved = ArgumentCaptor.forClass(Membership.class);
        verify(memberships).save(saved.capture());
        assertEquals(MembershipStatus.FROZEN, saved.getValue().status());
        assertEquals(LocalDate.of(2027, 4, 10), saved.getValue().endDate());
        assertEquals(LocalDate.of(2027, 3, 31), saved.getValue().originalEndDate());
    }

    @Test
    void freezeCountLimitFromContractedPolicy() {
        Membership membership = membership(MembershipStatus.ACTIVE, new FreezePolicy(null, 2, null));
        stubMembership(membership);
        when(freezes.findByMembership(TENANT, membership.id())).thenReturn(List.of(
                closedFreeze(membership.id(), 3), closedFreeze(membership.id(), 2)));
        ApiException error = assertThrows(ApiException.class, () -> useCase.execute(TENANT, membership.id(),
                new MembershipDtos.FreezeMembershipRequest(LocalDate.of(2027, 3, 15), "Viagem")));
        assertEquals("FREEZE_LIMIT_REACHED", error.code());
        verify(memberships, never()).save(any());
    }

    @Test
    void freezeShorterThanMinimumIsRejected() {
        Membership membership = membership(MembershipStatus.ACTIVE, new FreezePolicy(null, null, 5));
        stubMembership(membership);
        when(freezes.findByMembership(TENANT, membership.id())).thenReturn(List.of());
        ApiException error = assertThrows(ApiException.class, () -> useCase.execute(TENANT, membership.id(),
                new MembershipDtos.FreezeMembershipRequest(LocalDate.of(2027, 3, 12), "Viagem")));
        assertEquals("FREEZE_TOO_SHORT", error.code());
    }

    @Test
    void onlyActiveMembershipsCanBeFrozen() {
        Membership membership = membership(MembershipStatus.SUSPENDED, FreezePolicy.unlimited());
        when(memberships.findById(TENANT, membership.id())).thenReturn(Optional.of(membership));
        ApiException error = assertThrows(ApiException.class, () -> useCase.execute(TENANT, membership.id(),
                new MembershipDtos.FreezeMembershipRequest(LocalDate.of(2027, 3, 15), "Viagem")));
        assertEquals("INVALID_STATUS_TRANSITION", error.code());
    }

    @Test
    void membershipWithScheduledRenewalCannotBeFrozen() {
        Membership membership = membership(MembershipStatus.ACTIVE, FreezePolicy.unlimited());
        when(memberships.findById(TENANT, membership.id())).thenReturn(Optional.of(membership));
        when(memberships.findLiveRenewalOf(TENANT, membership.id())).thenReturn(Optional.of(membership));
        ApiException error = assertThrows(ApiException.class, () -> useCase.execute(TENANT, membership.id(),
                new MembershipDtos.FreezeMembershipRequest(LocalDate.of(2027, 3, 15), "Viagem")));
        assertEquals("FREEZE_BLOCKED_BY_RENEWAL", error.code());
    }

    private void stubMembership(Membership membership) {
        when(memberships.findById(TENANT, membership.id())).thenReturn(Optional.of(membership));
        when(memberships.findLiveRenewalOf(TENANT, membership.id())).thenReturn(Optional.empty());
    }

    private static Membership membership(MembershipStatus status, FreezePolicy policy) {
        ContractedConditions conditions = new ContractedConditions(DurationType.MONTHLY, 1, new BigDecimal("100.00"),
                Money.zero(), "BRL", BillingPeriod.MONTHLY, 1, Money.zero(), null, new BigDecimal("100.00"), policy,
                Map.of());
        return new Membership(UUID.randomUUID(), TENANT, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "M2027000001", status, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 31),
                LocalDate.of(2027, 3, 31), LocalDate.of(2027, 3, 1), 1, conditions, null, null, null, null, null,
                null, 0, Instant.EPOCH, Instant.EPOCH);
    }

    private static MembershipFreeze closedFreeze(UUID membershipId, int days) {
        LocalDate start = LocalDate.of(2027, 3, 2);
        return new MembershipFreeze(UUID.randomUUID(), TENANT, membershipId, start, start.plusDays(days - 1L),
                start.plusDays(days - 1L), days, days, FreezeStatus.COMPLETED, "x", null, null, Instant.EPOCH,
                Instant.EPOCH);
    }
}
