package com.gymsaas.student.application;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentDocumentRepository;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.student.domain.StudentStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Shared guards for student use cases. Keeps tenant scoping, CPF duplication
 * policy and status parsing out of controllers and JPA entities.
 */
@Component
public class StudentGuard {

    private final StudentRepository students;
    private final StudentDocumentRepository documents;
    private final BranchRepository branches;

    public StudentGuard(StudentRepository students, StudentDocumentRepository documents,
                        BranchRepository branches) {
        this.students = students;
        this.documents = documents;
        this.branches = branches;
    }

    public Student requireStudent(UUID tenantId, UUID studentId) {
        return students.findById(tenantId, studentId)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Aluno não encontrado."));
    }

    public Branch requireBranch(UUID tenantId, UUID branchId) {
        return branches.findById(tenantId, branchId)
                .orElseThrow(() -> ApiException.notFound("BRANCH_NOT_FOUND", "Unidade não encontrada."));
    }

    /** Validates a CPF for a new student or a CPF change on an existing student. */
    public String validateStudentCpf(UUID tenantId, String rawCpf, UUID excludeStudentId) {
        String normalized = normalizeCpf(rawCpf);
        if (students.existsActiveCpf(tenantId, normalized, excludeStudentId)) {
            throw ApiException.conflict("CPF_ALREADY_EXISTS",
                    "Já existe um aluno com este CPF nesta academia.");
        }
        return normalized;
    }

    /** Validates a CPF document, excluding the document being updated. */
    public String validateDocumentCpf(UUID tenantId, String rawCpf, UUID excludeDocumentId) {
        String normalized = normalizeCpf(rawCpf);
        if (documents.existsCpf(tenantId, normalized, excludeDocumentId)) {
            throw ApiException.conflict("CPF_ALREADY_EXISTS",
                    "Já existe um documento com este CPF nesta academia.");
        }
        return normalized;
    }

    public String normalizeCpf(String rawCpf) {
        String normalized = BrDocuments.normalizeCpf(rawCpf);
        if (normalized == null) {
            throw ApiException.badRequest("INVALID_CPF", "CPF inválido.");
        }
        return normalized;
    }

    public StudentStatus parseStatusOrDefault(String rawStatus, StudentStatus fallback) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return fallback;
        }
        try {
            return StudentStatus.parse(rawStatus);
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_STATUS", "Status inválido.");
        }
    }

    public StudentStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            throw ApiException.badRequest("INVALID_STATUS", "Status é obrigatório.");
        }
        try {
            return StudentStatus.parse(rawStatus);
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_STATUS", "Status inválido.");
        }
    }

    public String normalizeGender(String rawGender) {
        if (rawGender == null || rawGender.isBlank()) {
            return null;
        }
        return switch (rawGender.trim().toUpperCase()) {
            case "M", "MALE" -> "MALE";
            case "F", "FEMALE" -> "FEMALE";
            case "O", "OTHER" -> "OTHER";
            case "NOT_INFORMED" -> "NOT_INFORMED";
            default -> throw ApiException.badRequest("INVALID_GENDER", "Gênero inválido.");
        };
    }

    public static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String upperToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
