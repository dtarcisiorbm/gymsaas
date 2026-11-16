package com.gymsaas.student.domain;

import com.gymsaas.shared.domain.PageResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository {

    Optional<Student> findById(UUID tenantId, UUID studentId);

    /** Non-deleted students of the tenant with the given ids (batch lookup for listings). */
    java.util.List<Student> findAllByIds(UUID tenantId, java.util.Collection<UUID> studentIds);

    PageResult<Student> search(UUID tenantId, StudentSearchCriteria criteria);

    Student save(Student student);

    boolean existsActiveCpf(UUID tenantId, String normalizedCpf, UUID excludeStudentId);

    void softDelete(UUID tenantId, UUID studentId, Instant at);
}
