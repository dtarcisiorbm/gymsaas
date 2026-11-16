package com.gymsaas.student.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentDocumentRepository {

    List<StudentDocument> findByStudent(UUID tenantId, UUID studentId);

    List<StudentDocument> findCpfByStudentIds(UUID tenantId, java.util.Collection<UUID> studentIds);

    Optional<StudentDocument> findById(UUID tenantId, UUID studentId, UUID documentId);

    StudentDocument save(StudentDocument document);

    void softDelete(UUID tenantId, UUID studentId, UUID documentId, Instant at);

    boolean existsCpf(UUID tenantId, String normalizedNumber, UUID excludeDocumentId);

    void clearPrimary(UUID tenantId, UUID studentId, String type, UUID excludeDocumentId);
}
