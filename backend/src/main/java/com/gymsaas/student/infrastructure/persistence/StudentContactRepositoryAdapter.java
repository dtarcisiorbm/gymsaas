package com.gymsaas.student.infrastructure.persistence;

import com.gymsaas.student.domain.StudentContact;
import com.gymsaas.student.domain.StudentContactRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StudentContactRepositoryAdapter implements StudentContactRepository {

    private final StudentContactJpaRepository repository;

    public StudentContactRepositoryAdapter(StudentContactJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StudentContact> findByStudent(UUID tenantId, UUID studentId) {
        return repository
                .findByTenantIdAndStudentIdAndDeletedAtIsNullOrderByCreatedAtAsc(tenantId, studentId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<StudentContact> findById(UUID tenantId, UUID studentId, UUID contactId) {
        return repository.findByIdAndTenantIdAndStudentIdAndDeletedAtIsNull(contactId, tenantId, studentId)
                .map(this::toDomain);
    }

    @Override
    public StudentContact save(StudentContact contact) {
        StudentContactJpaEntity entity = repository.findById(contact.id())
                .orElseGet(() -> new StudentContactJpaEntity(contact.id(), contact.tenantId(),
                        contact.studentId(), contact.name(), contact.relationship(), contact.phone(),
                        contact.email(), contact.primary(), contact.notes(), contact.createdAt(),
                        contact.updatedAt(), contact.deletedAt()));
        entity.name = contact.name();
        entity.relationship = contact.relationship();
        entity.phone = contact.phone();
        entity.email = contact.email();
        entity.isPrimary = contact.primary();
        entity.notes = contact.notes();
        entity.updatedAt = contact.updatedAt();
        entity.deletedAt = contact.deletedAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public void softDelete(UUID tenantId, UUID studentId, UUID contactId, Instant at) {
        repository.softDelete(tenantId, studentId, contactId, at);
    }

    @Override
    public void clearPrimary(UUID tenantId, UUID studentId, UUID excludeContactId) {
        repository.clearPrimary(tenantId, studentId, excludeContactId);
    }

    private StudentContact toDomain(StudentContactJpaEntity entity) {
        return new StudentContact(entity.id, entity.tenantId, entity.studentId, entity.name,
                entity.relationship, entity.phone, entity.email, entity.isPrimary, entity.notes,
                entity.createdAt, entity.updatedAt, entity.deletedAt);
    }
}
