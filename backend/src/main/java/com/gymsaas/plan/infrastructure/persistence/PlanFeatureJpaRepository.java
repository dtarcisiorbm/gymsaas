package com.gymsaas.plan.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PlanFeatureJpaRepository extends JpaRepository<PlanFeatureJpaEntity, UUID> {

    List<PlanFeatureJpaEntity> findByTenantIdAndPlanIdOrderBySortOrderAsc(UUID tenantId, UUID planId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PlanFeatureJpaEntity f where f.tenantId = :tenantId and f.planId = :planId")
    int deleteByPlan(@Param("tenantId") UUID tenantId, @Param("planId") UUID planId);
}
