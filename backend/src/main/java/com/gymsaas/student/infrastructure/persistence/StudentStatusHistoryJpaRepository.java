package com.gymsaas.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentStatusHistoryJpaRepository
        extends JpaRepository<StudentStatusHistoryJpaEntity, UUID> {

    List<StudentStatusHistoryJpaEntity> findByTenantIdAndStudentIdOrderByChangedAtDesc(
            UUID tenantId, UUID studentId);
}
