package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.student.application.StudentAssembler;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentContact;
import com.gymsaas.student.domain.StudentContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AddStudentContactUseCase {

    private final StudentGuard guard;
    private final StudentContactRepository contacts;
    private final AuditService audit;

    public AddStudentContactUseCase(StudentGuard guard, StudentContactRepository contacts,
                                    AuditService audit) {
        this.guard = guard;
        this.contacts = contacts;
        this.audit = audit;
    }

    @Transactional
    public StudentDtos.ContactView execute(UUID tenantId, UUID studentId,
                                           StudentDtos.ContactRequest request) {
        guard.requireStudent(tenantId, studentId);
        List<StudentContact> existing = contacts.findByStudent(tenantId, studentId);
        boolean primary = Boolean.TRUE.equals(request.primary()) || existing.isEmpty();
        if (primary) {
            contacts.clearPrimary(tenantId, studentId, null);
        }
        Instant now = Instant.now();
        StudentContact saved = contacts.save(new StudentContact(UuidV7.generate(), tenantId, studentId,
                request.name().trim(), StudentGuard.trimToNull(request.relationship()),
                BrDocuments.phone(request.phone()), StudentGuard.trimToNull(request.email()),
                primary, StudentGuard.trimToNull(request.notes()), now, now, null));
        audit.recordForTenant(tenantId, "STUDENT_CONTACT_ADDED", "student", studentId, "MEDIUM",
                null, Map.of("primary", primary), Map.of());
        return StudentAssembler.contact(saved);
    }
}
