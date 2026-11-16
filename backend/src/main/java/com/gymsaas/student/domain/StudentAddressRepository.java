package com.gymsaas.student.domain;

import java.util.Optional;
import java.util.UUID;

public interface StudentAddressRepository {

    Optional<StudentAddress> findByStudent(UUID tenantId, UUID studentId);

    StudentAddress save(StudentAddress address);
}
