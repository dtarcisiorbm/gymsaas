package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentNote;
import com.gymsaas.student.domain.StudentNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AddStudentNoteUseCase {

    private final StudentGuard guard;
    private final StudentNoteRepository notes;
    private final AuditService audit;

    public AddStudentNoteUseCase(StudentGuard guard, StudentNoteRepository notes, AuditService audit) {
        this.guard = guard;
        this.notes = notes;
        this.audit = audit;
    }

    @Transactional
    public StudentDtos.NoteView execute(UUID tenantId, UUID studentId,
                                        StudentDtos.NoteRequest request) {
        guard.requireStudent(tenantId, studentId);
        String content = request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) {
            throw com.gymsaas.shared.application.ApiException.badRequest("INVALID_NOTE",
                    "Conteúdo da observação é obrigatório.");
        }

        Instant now = Instant.now();
        StudentNote saved = notes.save(new StudentNote(UuidV7.generate(), tenantId, studentId,
                content, TenantContext.userId(), now, now, null));

        audit.recordForTenant(tenantId, "STUDENT_NOTE_ADDED", "student", studentId, "LOW",
                null, Map.of("noteId", saved.id().toString()), Map.of());

        return toView(saved);
    }

    private StudentDtos.NoteView toView(StudentNote note) {
        return new StudentDtos.NoteView(note.id(), note.content(), note.createdBy(),
                note.createdAt(), note.updatedAt());
    }
}
