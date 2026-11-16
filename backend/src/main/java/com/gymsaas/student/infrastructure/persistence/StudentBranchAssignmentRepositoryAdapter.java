package com.gymsaas.student.infrastructure.persistence;

import com.gymsaas.student.domain.StudentBranchAssignment;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StudentBranchAssignmentRepositoryAdapter implements StudentBranchAssignmentRepository {

    private final StudentBranchAssignmentJpaRepository repository;

    public StudentBranchAssignmentRepositoryAdapter(StudentBranchAssignmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StudentBranchAssignment> findByStudent(UUID tenantId, UUID studentId) {
        return repository.findByTenantIdAndStudentIdOrderByStartedAtAsc(tenantId, studentId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<StudentBranchAssignment> findActive(UUID tenantId, UUID studentId) {
        return repository.findByTenantIdAndStudentIdAndEndedAtIsNull(tenantId, studentId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<StudentBranchAssignment> findActiveByStudentIds(UUID tenantId,
                                                                Collection<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        return repository.findByTenantIdAndStudentIdInAndEndedAtIsNull(tenantId, studentIds)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<StudentBranchAssignment> findActivePrimary(UUID tenantId, UUID studentId) {
        return repository
                .findByTenantIdAndStudentIdAndEndedAtIsNullAndIsPrimaryTrue(tenantId, studentId)
                .map(this::toDomain);
    }

    @Override
    public Optional<StudentBranchAssignment> findById(UUID tenantId, UUID studentId, UUID assignmentId) {
        return repository.findByIdAndTenantIdAndStudentId(assignmentId, tenantId, studentId)
                .map(this::toDomain);
    }

    @Override
    public StudentBranchAssignment save(StudentBranchAssignment assignment) {
        StudentBranchAssignmentJpaEntity entity = repository.findById(assignment.id())
                .orElseGet(() -> new StudentBranchAssignmentJpaEntity(assignment.id(),
                        assignment.tenantId(), assignment.studentId(), assignment.branchId(),
                        assignment.primary(), assignment.startedAt(), assignment.endedAt(),
                        assignment.transferReason(), assignment.assignedBy(), assignment.createdAt(),
                        assignment.updatedAt()));
        entity.isPrimary = assignment.primary();
        entity.endedAt = assignment.endedAt();
        entity.transferReason = assignment.transferReason();
        entity.updatedAt = assignment.updatedAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public boolean existsActive(UUID tenantId, UUID studentId, UUID branchId) {
        return repository.existsByTenantIdAndStudentIdAndBranchIdAndEndedAtIsNull(
                tenantId, studentId, branchId);
    }

    @Override
    public void clearPrimary(UUID tenantId, UUID studentId) {
        repository.clearPrimary(tenantId, studentId);
    }

    @Override
    public void end(UUID tenantId, UUID studentId, UUID assignmentId, Instant endedAt, String reason) {
        repository.end(tenantId, studentId, assignmentId, endedAt, reason);
    }

    private StudentBranchAssignment toDomain(StudentBranchAssignmentJpaEntity entity) {
        return new StudentBranchAssignment(entity.id, entity.tenantId, entity.studentId,
                entity.branchId, entity.isPrimary, entity.startedAt, entity.endedAt,
                entity.transferReason, entity.assignedBy, entity.createdAt, entity.updatedAt);
    }
}
