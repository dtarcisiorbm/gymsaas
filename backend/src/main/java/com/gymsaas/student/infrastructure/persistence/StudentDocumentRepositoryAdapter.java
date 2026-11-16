package com.gymsaas.student.infrastructure.persistence;

import com.gymsaas.student.domain.StudentDocument;
import com.gymsaas.student.domain.StudentDocumentRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StudentDocumentRepositoryAdapter implements StudentDocumentRepository {

    private final StudentDocumentJpaRepository repository;

    public StudentDocumentRepositoryAdapter(StudentDocumentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StudentDocument> findByStudent(UUID tenantId, UUID studentId) {
        return repository
                .findByTenantIdAndStudentIdAndDeletedAtIsNullOrderByCreatedAtAsc(tenantId, studentId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<StudentDocument> findCpfByStudentIds(UUID tenantId, Collection<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        return repository.findByTenantIdAndStudentIdInAndDocumentTypeAndDeletedAtIsNull(
                tenantId, studentIds, "CPF").stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<StudentDocument> findById(UUID tenantId, UUID studentId, UUID documentId) {
        return repository.findByIdAndTenantIdAndStudentIdAndDeletedAtIsNull(documentId, tenantId, studentId)
                .map(this::toDomain);
    }

    @Override
    public StudentDocument save(StudentDocument document) {
        StudentDocumentJpaEntity entity = repository.findById(document.id())
                .orElseGet(() -> new StudentDocumentJpaEntity(document.id(), document.tenantId(),
                        document.studentId(), document.type(), document.number(),
                        document.normalizedNumber(), document.issuer(), document.state(),
                        document.issuedAt(), document.expiresAt(), document.notes(),
                        document.primary(), document.createdAt(), document.updatedAt(),
                        document.deletedAt()));
        entity.documentType = document.type();
        entity.documentNumber = document.number();
        entity.normalizedNumber = document.normalizedNumber();
        entity.issuer = document.issuer();
        entity.state = document.state();
        entity.issuedAt = document.issuedAt();
        entity.expiresAt = document.expiresAt();
        entity.notes = document.notes();
        entity.isPrimary = document.primary();
        entity.updatedAt = document.updatedAt();
        entity.deletedAt = document.deletedAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public void softDelete(UUID tenantId, UUID studentId, UUID documentId, Instant at) {
        repository.softDelete(tenantId, studentId, documentId, at);
    }

    @Override
    public boolean existsCpf(UUID tenantId, String normalizedNumber, UUID excludeDocumentId) {
        if (normalizedNumber == null) {
            return false;
        }
        return excludeDocumentId == null
                ? repository.existsByTenantIdAndDocumentTypeAndNormalizedNumberAndDeletedAtIsNull(
                        tenantId, "CPF", normalizedNumber)
                : repository.existsByTenantIdAndDocumentTypeAndNormalizedNumberAndDeletedAtIsNullAndIdNot(
                        tenantId, "CPF", normalizedNumber, excludeDocumentId);
    }

    @Override
    public void clearPrimary(UUID tenantId, UUID studentId, String type, UUID excludeDocumentId) {
        repository.clearPrimary(tenantId, studentId, type, excludeDocumentId);
    }

    private StudentDocument toDomain(StudentDocumentJpaEntity entity) {
        return new StudentDocument(entity.id, entity.tenantId, entity.studentId,
                entity.documentType, entity.documentNumber, entity.normalizedNumber, entity.issuer,
                entity.state, entity.issuedAt, entity.expiresAt, entity.notes, entity.isPrimary,
                entity.createdAt, entity.updatedAt, entity.deletedAt);
    }
}
