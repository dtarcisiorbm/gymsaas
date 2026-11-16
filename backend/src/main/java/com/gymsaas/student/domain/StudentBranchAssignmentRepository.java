package com.gymsaas.student.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentBranchAssignmentRepository {

    List<StudentBranchAssignment> findByStudent(UUID tenantId, UUID studentId);

    List<StudentBranchAssignment> findActive(UUID tenantId, UUID studentId);

    List<StudentBranchAssignment> findActiveByStudentIds(UUID tenantId, java.util.Collection<UUID> studentIds);

    Optional<StudentBranchAssignment> findActivePrimary(UUID tenantId, UUID studentId);

    Optional<StudentBranchAssignment> findById(UUID tenantId, UUID studentId, UUID assignmentId);

    StudentBranchAssignment save(StudentBranchAssignment assignment);

    boolean existsActive(UUID tenantId, UUID studentId, UUID branchId);

    void clearPrimary(UUID tenantId, UUID studentId);

    void end(UUID tenantId, UUID studentId, UUID assignmentId, Instant endedAt, String reason);
}
