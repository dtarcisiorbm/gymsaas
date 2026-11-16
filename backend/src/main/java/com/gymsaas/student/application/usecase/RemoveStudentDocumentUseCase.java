package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentDocument;
import com.gymsaas.student.domain.StudentDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class RemoveStudentDocumentUseCase {

    private final StudentGuard guard;
    private final StudentDocumentRepository documents;
    private final AuditService audit;

    public RemoveStudentDocumentUseCase(StudentGuard guard, StudentDocumentRepository documents,
                                        AuditService audit) {
        this.guard = guard;
        this.documents = documents;
        this.audit = audit;
    }

    @Transactional
    public void execute(UUID tenantId, UUID studentId, UUID documentId) {
        guard.requireStudent(tenantId, studentId);
        StudentDocument document = documents.findById(tenantId, studentId, documentId)
                .orElseThrow(() -> ApiException.notFound("DOCUMENT_NOT_FOUND", "Documento não encontrado."));
        documents.softDelete(tenantId, studentId, documentId, Instant.now());
        audit.recordForTenant(tenantId, "STUDENT_DOCUMENT_REMOVED", "student", studentId, "HIGH",
                Map.of("type", document.type()), null, Map.of());
    }
}
