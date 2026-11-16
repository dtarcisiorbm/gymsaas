package com.gymsaas.plan.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanPriceJpaRepository extends JpaRepository<PlanPriceJpaEntity, UUID> {

    List<PlanPriceJpaEntity> findByTenantIdAndPlanIdOrderByValidFromDescCreatedAtDesc(UUID tenantId, UUID planId);

    Optional<PlanPriceJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            select p from PlanPriceJpaEntity p
            where p.tenantId = :tenantId and p.planId = :planId and p.status = 'ACTIVE'
              and p.validFrom <= :date and (p.validUntil is null or p.validUntil >= :date)
            """)
    Optional<PlanPriceJpaEntity> findEffective(@Param("tenantId") UUID tenantId, @Param("planId") UUID planId,
                                               @Param("date") LocalDate date);

    @Query("""
            select p from PlanPriceJpaEntity p
            where p.tenantId = :tenantId and p.planId in :planIds and p.status = 'ACTIVE'
              and p.validFrom <= :date and (p.validUntil is null or p.validUntil >= :date)
            """)
    List<PlanPriceJpaEntity> findEffectiveIn(@Param("tenantId") UUID tenantId,
                                             @Param("planIds") Collection<UUID> planIds,
                                             @Param("date") LocalDate date);
}
