package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.student.application.StudentAssembler;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentDocument;
import com.gymsaas.student.domain.StudentDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UpdateStudentDocumentUseCase {

    private static final Set<String> TYPES = Set.of("CPF", "RG", "CNH", "PASSPORT", "OTHER");

    private final StudentGuard guard;
    private final StudentDocumentRepository documents;
    private final AuditService audit;

    public UpdateStudentDocumentUseCase(StudentGuard guard, StudentDocumentRepository documents,
                                        AuditService audit) {
        this.guard = guard;
        this.documents = documents;
        this.audit = audit;
    }

    @Transactional
    public StudentDtos.DocumentView execute(UUID tenantId, UUID studentId, UUID documentId,
                                            StudentDtos.DocumentRequest request) {
        guard.requireStudent(tenantId, studentId);
        StudentDocument current = documents.findById(tenantId, studentId, documentId)
                .orElseThrow(() -> ApiException.notFound("DOCUMENT_NOT_FOUND", "Documento não encontrado."));
        String type = normalizeType(request.type());
        String normalized = "CPF".equals(type)
                ? guard.validateDocumentCpf(tenantId, request.number(), documentId)
                : normalizeNumber(request.number());

        boolean primary = Boolean.TRUE.equals(request.primary());
        if (primary && !current.primary()) {
            documents.clearPrimary(tenantId, studentId, type, documentId);
        }
        Instant now = Instant.now();
        StudentDocument saved = documents.save(new StudentDocument(current.id(), tenantId, studentId,
                type, request.number().trim(), normalized,
                StudentGuard.trimToNull(request.issuer()), StudentGuard.upperToNull(request.state()),
                request.issuedAt(), request.expiresAt(), StudentGuard.trimToNull(request.notes()),
                primary || current.primary(), current.createdAt(), now, null));

        audit.recordForTenant(tenantId, "STUDENT_DOCUMENT_UPDATED", "student", studentId, "HIGH",
                Map.of("type", current.type()), Map.of("type", type), Map.of());
        return StudentAssembler.document(saved, false);
    }

    private String normalizeType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase();
        if (!TYPES.contains(type)) {
            throw ApiException.badRequest("INVALID_DOCUMENT_TYPE", "Tipo de documento inválido.");
        }
        return type;
    }

    private String normalizeNumber(String value) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("INVALID_DOCUMENT_NUMBER", "Número do documento é obrigatório.");
        }
        String digits = BrDocuments.document(value);
        return digits == null || digits.isBlank() ? value.trim().toUpperCase() : digits;
    }
}
