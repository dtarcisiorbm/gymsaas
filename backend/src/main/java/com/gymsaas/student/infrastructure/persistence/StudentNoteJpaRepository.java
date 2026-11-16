package com.gymsaas.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentNoteJpaRepository extends JpaRepository<StudentNoteJpaEntity, UUID> {

    List<StudentNoteJpaEntity> findByTenantIdAndStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID tenantId, UUID studentId);
}
