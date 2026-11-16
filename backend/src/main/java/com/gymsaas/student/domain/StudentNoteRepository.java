package com.gymsaas.student.domain;

import java.util.List;
import java.util.UUID;

public interface StudentNoteRepository {

    StudentNote save(StudentNote note);

    List<StudentNote> findByStudent(UUID tenantId, UUID studentId);
}
