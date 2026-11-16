package com.gymsaas.student.infrastructure.persistence;

import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.student.domain.StudentSearchCriteria;
import com.gymsaas.student.domain.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class StudentRepositoryAdapter implements StudentRepository {

    private static final Set<String> ALLOWED_SORTS =
            Set.of("name", "registrationNumber", "createdAt", "status");

    private final StudentJpaRepository repository;
    private final StudentDocumentJpaRepository documents;

    public StudentRepositoryAdapter(StudentJpaRepository repository,
                                    StudentDocumentJpaRepository documents) {
        this.repository = repository;
        this.documents = documents;
    }

    @Override
    public Optional<Student> findById(UUID tenantId, UUID studentId) {
        return repository.findByIdAndTenantIdAndDeletedAtIsNull(studentId, tenantId)
                .map(this::toDomain);
    }

    @Override
    public java.util.List<Student> findAllByIds(UUID tenantId, java.util.Collection<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return java.util.List.of();
        }
        return repository.findByTenantIdAndIdInAndDeletedAtIsNull(tenantId, studentIds).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public PageResult<Student> search(UUID tenantId, StudentSearchCriteria criteria) {
        String sort = criteria.sort() != null && ALLOWED_SORTS.contains(criteria.sort())
                ? criteria.sort() : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(criteria.direction())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Page<StudentJpaEntity> result = repository.search(tenantId,
                criteria.status() == null ? "" : criteria.status().name(),
                emptyIfNull(criteria.search()),
                emptyIfNull(criteria.document()),
                criteria.branchId(),
                PageRequest.of(criteria.page(), criteria.size(), Sort.by(direction, sort)));
        return PageResult.of(result.getContent().stream().map(this::toDomain).toList(),
                result.getTotalElements(), criteria.page(), criteria.size());
    }

    @Override
    public Student save(Student student) {
        StudentJpaEntity entity = repository.findById(student.id())
                .orElseGet(() -> new StudentJpaEntity(student.id(), student.tenantId(),
                        student.registrationNumber(), student.name(), student.socialName(),
                        student.birthDate(), student.email(), student.phone(), student.gender(),
                        student.photoUrl(), student.status().name(), student.notes(),
                        (int) student.version(), student.createdAt(), student.updatedAt(),
                        student.deletedAt()));
        entity.name = student.name();
        entity.socialName = student.socialName();
        entity.birthDate = student.birthDate();
        entity.email = student.email();
        entity.phone = student.phone();
        entity.gender = student.gender();
        entity.photoUrl = student.photoUrl();
        entity.status = student.status().name();
        entity.notes = student.notes();
        entity.updatedAt = student.updatedAt();
        entity.deletedAt = student.deletedAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public boolean existsActiveCpf(UUID tenantId, String normalizedCpf, UUID excludeStudentId) {
        if (normalizedCpf == null) {
            return false;
        }
        return excludeStudentId == null
                ? documents.existsByTenantIdAndDocumentTypeAndNormalizedNumberAndDeletedAtIsNull(
                        tenantId, "CPF", normalizedCpf)
                : documents.existsByTenantIdAndDocumentTypeAndNormalizedNumberAndDeletedAtIsNullAndIdNot(
                        tenantId, "CPF", normalizedCpf, studentDocumentId(tenantId, excludeStudentId));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void softDelete(UUID tenantId, UUID studentId, Instant at) {
        int updated = repository.softDelete(tenantId, studentId, at);
        if (updated == 0) {
            throw com.gymsaas.shared.application.ApiException.notFound("STUDENT_NOT_FOUND",
                    "Aluno não encontrado.");
        }
    }

    /** The unique index is on documents; exclusion by student is resolved to its CPF document. */
    private UUID studentDocumentId(UUID tenantId, UUID excludeStudentId) {
        return documents
                .findByTenantIdAndStudentIdInAndDocumentTypeAndDeletedAtIsNull(
                        tenantId, java.util.List.of(excludeStudentId), "CPF")
                .stream()
                .map(document -> document.id)
                .findFirst()
                .orElse(null);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value.trim();
    }

    private Student toDomain(StudentJpaEntity entity) {
        return new Student(entity.id, entity.tenantId, entity.registrationNumber, entity.name,
                entity.socialName, entity.birthDate, entity.email, entity.phone, entity.gender,
                entity.photoUrl, StudentStatus.parse(entity.status), entity.notes, entity.version,
                entity.createdAt, entity.updatedAt, entity.deletedAt);
    }
}
