package com.gymsaas.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentBranchAssignmentJpaRepository
        extends JpaRepository<StudentBranchAssignmentJpaEntity, UUID> {

    List<StudentBranchAssignmentJpaEntity> findByTenantIdAndStudentIdOrderByStartedAtAsc(
            UUID tenantId, UUID studentId);

    List<StudentBranchAssignmentJpaEntity> findByTenantIdAndStudentIdAndEndedAtIsNull(
            UUID tenantId, UUID studentId);

    List<StudentBranchAssignmentJpaEntity> findByTenantIdAndStudentIdInAndEndedAtIsNull(
            UUID tenantId, Collection<UUID> studentIds);

    Optional<StudentBranchAssignmentJpaEntity> findByTenantIdAndStudentIdAndEndedAtIsNullAndIsPrimaryTrue(
            UUID tenantId, UUID studentId);

    Optional<StudentBranchAssignmentJpaEntity> findByIdAndTenantIdAndStudentId(
            UUID id, UUID tenantId, UUID studentId);

    boolean existsByTenantIdAndStudentIdAndBranchIdAndEndedAtIsNull(
            UUID tenantId, UUID studentId, UUID branchId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update StudentBranchAssignmentJpaEntity a set a.isPrimary = false
            where a.tenantId = :tenantId and a.studentId = :studentId
              and a.endedAt is null and a.isPrimary = true
            """)
    int clearPrimary(@Param("tenantId") UUID tenantId, @Param("studentId") UUID studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update StudentBranchAssignmentJpaEntity a
            set a.endedAt = :endedAt, a.transferReason = :reason
            where a.id = :assignmentId and a.tenantId = :tenantId and a.studentId = :studentId
              and a.endedAt is null
            """)
    int end(@Param("tenantId") UUID tenantId, @Param("studentId") UUID studentId,
            @Param("assignmentId") UUID assignmentId, @Param("endedAt") Instant endedAt,
            @Param("reason") String reason);
}
