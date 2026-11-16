package com.gymsaas.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentAddressJpaRepository extends JpaRepository<StudentAddressJpaEntity, UUID> {

    Optional<StudentAddressJpaEntity> findByTenantIdAndStudentId(UUID tenantId, UUID studentId);
}
