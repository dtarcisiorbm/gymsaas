package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.student.application.StudentAssembler;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentContact;
import com.gymsaas.student.domain.StudentContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class UpdateStudentContactUseCase {

    private final StudentGuard guard;
    private final StudentContactRepository contacts;
    private final AuditService audit;

    public UpdateStudentContactUseCase(StudentGuard guard, StudentContactRepository contacts,
                                       AuditService audit) {
        this.guard = guard;
        this.contacts = contacts;
        this.audit = audit;
    }

    @Transactional
    public StudentDtos.ContactView execute(UUID tenantId, UUID studentId, UUID contactId,
                                           StudentDtos.ContactRequest request) {
        guard.requireStudent(tenantId, studentId);
        StudentContact current = contacts.findById(tenantId, studentId, contactId)
                .orElseThrow(() -> ApiException.notFound("CONTACT_NOT_FOUND", "Contato não encontrado."));
        boolean primary = Boolean.TRUE.equals(request.primary()) || current.primary();
        if (primary && !current.primary()) {
            contacts.clearPrimary(tenantId, studentId, contactId);
        }
        Instant now = Instant.now();
        StudentContact saved = contacts.save(new StudentContact(current.id(), tenantId, studentId,
                request.name().trim(), StudentGuard.trimToNull(request.relationship()),
                BrDocuments.phone(request.phone()), StudentGuard.trimToNull(request.email()),
                primary, StudentGuard.trimToNull(request.notes()), current.createdAt(), now, null));
        audit.recordForTenant(tenantId, "STUDENT_CONTACT_UPDATED", "student", studentId, "MEDIUM",
                null, Map.of("primary", primary), Map.of());
        return StudentAssembler.contact(saved);
    }
}
