package com.gymsaas.student.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentContactRepository {

    List<StudentContact> findByStudent(UUID tenantId, UUID studentId);

    Optional<StudentContact> findById(UUID tenantId, UUID studentId, UUID contactId);

    StudentContact save(StudentContact contact);

    void softDelete(UUID tenantId, UUID studentId, UUID contactId, Instant at);

    void clearPrimary(UUID tenantId, UUID studentId, UUID excludeContactId);
}
