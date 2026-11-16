package com.gymsaas.student.infrastructure.persistence;

import com.gymsaas.student.domain.StudentStatus;
import com.gymsaas.student.domain.StudentStatusEntry;
import com.gymsaas.student.domain.StudentStatusHistoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class StudentStatusHistoryRepositoryAdapter implements StudentStatusHistoryRepository {

    private final StudentStatusHistoryJpaRepository repository;

    public StudentStatusHistoryRepositoryAdapter(StudentStatusHistoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public StudentStatusEntry save(StudentStatusEntry entry) {
        StudentStatusHistoryJpaEntity entity = new StudentStatusHistoryJpaEntity(entry.id(),
                entry.tenantId(), entry.studentId(),
                entry.fromStatus() == null ? null : entry.fromStatus().name(),
                entry.toStatus().name(), entry.reason(), entry.changedBy(), entry.changedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public List<StudentStatusEntry> findByStudent(UUID tenantId, UUID studentId) {
        return repository.findByTenantIdAndStudentIdOrderByChangedAtDesc(tenantId, studentId)
                .stream().map(this::toDomain).toList();
    }

    private StudentStatusEntry toDomain(StudentStatusHistoryJpaEntity entity) {
        return new StudentStatusEntry(entity.id, entity.tenantId, entity.studentId,
                entity.fromStatus == null ? null : StudentStatus.parse(entity.fromStatus),
                StudentStatus.parse(entity.toStatus), entity.reason, entity.changedBy, entity.changedAt);
    }
}
