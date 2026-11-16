package com.gymsaas.student.infrastructure.persistence;

import com.gymsaas.student.domain.StudentAddress;
import com.gymsaas.student.domain.StudentAddressRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class StudentAddressRepositoryAdapter implements StudentAddressRepository {

    private final StudentAddressJpaRepository repository;

    public StudentAddressRepositoryAdapter(StudentAddressJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<StudentAddress> findByStudent(UUID tenantId, UUID studentId) {
        return repository.findByTenantIdAndStudentId(tenantId, studentId).map(this::toDomain);
    }

    @Override
    public StudentAddress save(StudentAddress address) {
        StudentAddressJpaEntity entity = repository.findById(address.id())
                .orElseGet(() -> new StudentAddressJpaEntity(address.id(), address.tenantId(),
                        address.studentId(), address.zipCode(), address.street(), address.number(),
                        address.complement(), address.neighborhood(), address.city(), address.state(),
                        address.createdAt(), address.updatedAt()));
        entity.zipCode = address.zipCode();
        entity.street = address.street();
        entity.addressNumber = address.number();
        entity.complement = address.complement();
        entity.neighborhood = address.neighborhood();
        entity.city = address.city();
        entity.state = address.state();
        entity.updatedAt = address.updatedAt();
        return toDomain(repository.save(entity));
    }

    private StudentAddress toDomain(StudentAddressJpaEntity entity) {
        return new StudentAddress(entity.id, entity.tenantId, entity.studentId, entity.zipCode,
                entity.street, entity.addressNumber, entity.complement, entity.neighborhood,
                entity.city, entity.state, entity.createdAt, entity.updatedAt);
    }
}
