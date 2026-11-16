package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentContact;
import com.gymsaas.student.domain.StudentContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class RemoveStudentContactUseCase {

    private final StudentGuard guard;
    private final StudentContactRepository contacts;
    private final AuditService audit;

    public RemoveStudentContactUseCase(StudentGuard guard, StudentContactRepository contacts,
                                       AuditService audit) {
        this.guard = guard;
        this.contacts = contacts;
        this.audit = audit;
    }

    @Transactional
    public void execute(UUID tenantId, UUID studentId, UUID contactId) {
        guard.requireStudent(tenantId, studentId);
        StudentContact contact = contacts.findById(tenantId, studentId, contactId)
                .orElseThrow(() -> ApiException.notFound("CONTACT_NOT_FOUND", "Contato não encontrado."));
        contacts.softDelete(tenantId, studentId, contactId, Instant.now());
        audit.recordForTenant(tenantId, "STUDENT_CONTACT_REMOVED", "student", studentId, "MEDIUM",
                Map.of("primary", contact.primary()), null, Map.of());
    }
}
