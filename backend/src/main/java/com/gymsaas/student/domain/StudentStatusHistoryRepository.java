package com.gymsaas.student.domain;

import java.util.List;
import java.util.UUID;

public interface StudentStatusHistoryRepository {

    StudentStatusEntry save(StudentStatusEntry entry);

    List<StudentStatusEntry> findByStudent(UUID tenantId, UUID studentId);
}
