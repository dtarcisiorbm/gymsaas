package com.gymsaas.membership.infrastructure.persistence;

import com.gymsaas.membership.domain.ContractedConditions;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.membership.domain.MembershipSearchCriteria;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.shared.domain.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class StudentMembershipRepositoryAdapter implements MembershipRepository {

    private static final Set<String> ALLOWED_SORTS =
            Set.of("createdAt", "startDate", "endDate", "registrationNumber", "status");
    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);
    private static final UUID NO_ID = new UUID(0L, 0L);

    private final StudentMembershipJpaRepository repository;

    public StudentMembershipRepositoryAdapter(StudentMembershipJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Membership> findById(UUID tenantId, UUID membershipId) {
        return repository.findByIdAndTenantId(membershipId, tenantId).map(StudentMembershipRepositoryAdapter::toDomain);
    }

    @Override
    public List<Membership> findAllByIds(UUID tenantId, Collection<UUID> membershipIds) {
        if (membershipIds == null || membershipIds.isEmpty()) {
            return List.of();
        }
        return repository.findByTenantIdAndIdIn(tenantId, membershipIds).stream()
                .map(StudentMembershipRepositoryAdapter::toDomain).toList();
    }

    @Override
    public PageResult<Membership> search(UUID tenantId, MembershipSearchCriteria criteria) {
        String sort = criteria.sort() != null && ALLOWED_SORTS.contains(criteria.sort()) ? criteria.sort() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(criteria.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<StudentMembershipJpaEntity> result = repository.search(tenantId,
                criteria.status() == null ? "" : criteria.status().name(), criteria.studentId(), criteria.planId(),
                criteria.branchId(), criteria.endingFrom() == null ? MIN_DATE : criteria.endingFrom(),
                criteria.endingTo() == null ? MAX_DATE : criteria.endingTo(),
                criteria.search() == null ? "" : criteria.search().trim(),
                PageRequest.of(criteria.page(), criteria.size(), Sort.by(direction, sort).and(Sort.by("id"))));
        return PageResult.of(result.getContent().stream().map(StudentMembershipRepositoryAdapter::toDomain).toList(),
                result.getTotalElements(), criteria.page(), criteria.size());
    }

    @Override
    public Membership save(Membership membership) {
        StudentMembershipJpaEntity entity = repository.findById(membership.id()).orElse(null);
        if (entity == null) {
            entity = newEntity(membership);
        } else if (entity.version != membership.version()) {
            throw new ObjectOptimisticLockingFailureException(StudentMembershipJpaEntity.class, membership.id());
        }
        // Only lifecycle/operational fields are mutable (see V11 immutability trigger).
        entity.status = membership.status().name();
        entity.branchId = membership.branchId();
        entity.endDate = membership.endDate();
        entity.notes = membership.notes();
        entity.cancelledAt = membership.cancelledAt();
        entity.cancelledBy = membership.cancelledBy();
        entity.cancellationReason = membership.cancellationReason();
        entity.updatedAt = membership.updatedAt();
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public boolean existsOpenOverlap(UUID tenantId, UUID studentId, UUID planId, LocalDate start, LocalDate end,
                                     UUID excludeId) {
        return repository.existsOpenOverlap(tenantId, studentId, planId, start, end,
                excludeId == null ? NO_ID : excludeId);
    }

    @Override
    public Optional<Membership> findLiveRenewalOf(UUID tenantId, UUID membershipId) {
        return repository.findFirstByTenantIdAndRenewedFromIdAndStatusNot(tenantId, membershipId,
                MembershipStatus.CANCELLED.name()).map(StudentMembershipRepositoryAdapter::toDomain);
    }

    @Override
    public List<Membership> findPendingStartingOnOrBefore(UUID tenantId, LocalDate date) {
        return repository.findByTenantIdAndStatusAndStartDateLessThanEqual(tenantId,
                MembershipStatus.PENDING.name(), date).stream().map(StudentMembershipRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Membership> findRunningEndedBefore(UUID tenantId, LocalDate date) {
        return repository.findByTenantIdAndStatusInAndEndDateLessThan(tenantId,
                        List.of(MembershipStatus.ACTIVE.name(), MembershipStatus.SUSPENDED.name()), date).stream()
                .map(StudentMembershipRepositoryAdapter::toDomain).toList();
    }

    private static StudentMembershipJpaEntity newEntity(Membership membership) {
        ContractedConditions conditions = membership.conditions();
        FreezePolicy policy = conditions.freezePolicy() == null ? FreezePolicy.unlimited() : conditions.freezePolicy();
        StudentMembershipJpaEntity entity = new StudentMembershipJpaEntity();
        entity.id = membership.id();
        entity.tenantId = membership.tenantId();
        entity.studentId = membership.studentId();
        entity.planId = membership.planId();
        entity.planPriceId = membership.planPriceId();
        entity.registrationNumber = membership.registrationNumber();
        entity.startDate = membership.startDate();
        entity.originalEndDate = membership.originalEndDate();
        entity.anchorDate = membership.anchorDate();
        entity.termSequence = membership.termSequence();
        entity.durationType = conditions.durationType().name();
        entity.durationValue = conditions.durationValue();
        entity.amount = conditions.amount();
        entity.setupFee = conditions.setupFee();
        entity.currency = conditions.currency();
        entity.billingPeriod = conditions.billingPeriod().name();
        entity.billingInterval = conditions.billingInterval();
        entity.discountAmount = conditions.discountAmount();
        entity.discountReason = conditions.discountReason();
        entity.finalAmount = conditions.finalAmount();
        entity.maxFreezeDays = policy.maxFreezeDays();
        entity.maxFreezes = policy.maxFreezes();
        entity.minFreezeDays = policy.minFreezeDays();
        entity.conditionsSnapshot = new HashMap<>(conditions.snapshot());
        entity.renewedFromId = membership.renewedFromId();
        entity.createdBy = membership.createdBy();
        entity.createdAt = membership.createdAt();
        return entity;
    }

    static Membership toDomain(StudentMembershipJpaEntity entity) {
        ContractedConditions conditions = new ContractedConditions(DurationType.parse(entity.durationType),
                entity.durationValue, entity.amount, entity.setupFee, entity.currency.trim(),
                BillingPeriod.parse(entity.billingPeriod), entity.billingInterval, entity.discountAmount,
                entity.discountReason, entity.finalAmount,
                new FreezePolicy(entity.maxFreezeDays, entity.maxFreezes, entity.minFreezeDays),
                entity.conditionsSnapshot);
        return new Membership(entity.id, entity.tenantId, entity.studentId, entity.planId, entity.planPriceId,
                entity.branchId, entity.registrationNumber, MembershipStatus.parse(entity.status), entity.startDate,
                entity.endDate, entity.originalEndDate, entity.anchorDate, entity.termSequence, conditions,
                entity.renewedFromId, entity.notes, entity.cancelledAt, entity.cancelledBy, entity.cancellationReason,
                entity.createdBy, entity.version, entity.createdAt, entity.updatedAt);
    }
}
