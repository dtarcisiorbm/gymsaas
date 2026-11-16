package com.gymsaas.student.infrastructure.persistence;

import com.gymsaas.student.domain.StudentNote;
import com.gymsaas.student.domain.StudentNoteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class StudentNoteRepositoryAdapter implements StudentNoteRepository {

    private final StudentNoteJpaRepository repository;

    public StudentNoteRepositoryAdapter(StudentNoteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public StudentNote save(StudentNote note) {
        StudentNoteJpaEntity entity = repository.findById(note.id())
                .orElseGet(() -> new StudentNoteJpaEntity(note.id(), note.tenantId(), note.studentId(),
                        note.content(), note.createdBy(), note.createdAt(), note.updatedAt(),
                        note.deletedAt()));
        entity.content = note.content();
        entity.updatedAt = note.updatedAt();
        entity.deletedAt = note.deletedAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public List<StudentNote> findByStudent(UUID tenantId, UUID studentId) {
        return repository
                .findByTenantIdAndStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId, studentId)
                .stream().map(this::toDomain).toList();
    }

    private StudentNote toDomain(StudentNoteJpaEntity entity) {
        return new StudentNote(entity.id, entity.tenantId, entity.studentId, entity.content,
                entity.createdBy, entity.createdAt, entity.updatedAt, entity.deletedAt);
    }
}
