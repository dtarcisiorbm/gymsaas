package com.gymsaas.student.application;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.student.domain.Student;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class StudentDtos {

    private StudentDtos() {
    }

    public record StudentFilter(
            String search,
            String document,
            String status,
            UUID branchId,
            int page,
            int size,
            String sort,
            String direction) {
    }

    public record CreateStudentRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String socialName,
            @Past(message = "Data de nascimento deve estar no passado.") LocalDate birthDate,
            @Email(message = "E-mail inválido.") @Size(max = 150) String email,
            @Size(max = 20) String phone,
            @Pattern(regexp = "MALE|FEMALE|OTHER|NOT_INFORMED|M|F|O", message = "Gênero inválido.")
            String gender,
            @Size(max = 500) String photoUrl,
            @Pattern(regexp = "PENDING|ACTIVE|INACTIVE|BLOCKED|ARCHIVED", message = "Status inválido.")
            String status,
            @Size(max = 4000) String notes,
            @Size(max = 14) String cpf,
            @Valid List<ContactRequest> contacts,
            @Valid AddressRequest address,
            UUID primaryBranchId) {
    }

    public record UpdateStudentRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String socialName,
            @Past(message = "Data de nascimento deve estar no passado.") LocalDate birthDate,
            @Email(message = "E-mail inválido.") @Size(max = 150) String email,
            @Size(max = 20) String phone,
            @Pattern(regexp = "MALE|FEMALE|OTHER|NOT_INFORMED|M|F|O", message = "Gênero inválido.")
            String gender,
            @Size(max = 500) String photoUrl,
            @Size(max = 4000) String notes,
            @Size(max = 14) String cpf) {
    }

    public record ChangeStudentStatusRequest(
            @NotBlank @Pattern(regexp = "PENDING|ACTIVE|INACTIVE|BLOCKED|ARCHIVED", message = "Status inválido.")
            String status,
            @Size(max = 1000) String reason) {
    }

    public record DocumentRequest(
            @NotBlank @Pattern(regexp = "CPF|RG|CNH|PASSPORT|OTHER", message = "Tipo de documento inválido.")
            String type,
            @NotBlank @Size(max = 100) String number,
            @Size(max = 100) String issuer,
            @Pattern(regexp = "[A-Za-z]{2}", message = "UF inválida.") String state,
            LocalDate issuedAt,
            LocalDate expiresAt,
            @Size(max = 1000) String notes,
            Boolean primary) {
    }

    public record ContactRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 50) String relationship,
            @NotBlank @Size(max = 20) String phone,
            @Email(message = "E-mail inválido.") @Size(max = 150) String email,
            Boolean primary,
            @Size(max = 1000) String notes) {
    }

    public record AddressRequest(
            @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido.") String zipCode,
            @Size(max = 200) String street,
            @Size(max = 20) String number,
            @Size(max = 100) String complement,
            @Size(max = 100) String neighborhood,
            @Size(max = 100) String city,
            @Pattern(regexp = "[A-Za-z]{2}", message = "UF inválida.") String state) {
    }

    public record AssignBranchRequest(
            @NotNull UUID branchId,
            Boolean primary) {
    }

    public record TransferBranchRequest(
            @NotNull UUID branchId,
            @Size(max = 1000) String reason) {
    }

    public record NoteRequest(
            @NotBlank @Size(max = 4000) String content) {
    }

    public record StudentSummary(
            UUID id,
            String registrationNumber,
            String name,
            String socialName,
            String cpfMasked,
            String phone,
            String status,
            UUID primaryBranchId,
            String primaryBranchName,
            Instant createdAt) {
    }

    public record StudentDetail(
            UUID id,
            UUID tenantId,
            String registrationNumber,
            String name,
            String socialName,
            LocalDate birthDate,
            String email,
            String phone,
            String gender,
            String photoUrl,
            String status,
            String notes,
            String cpf,
            String cpfMasked,
            AddressView address,
            List<DocumentView> documents,
            List<ContactView> contacts,
            List<BranchAssignmentView> branches,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record DocumentView(
            UUID id,
            String type,
            String number,
            String issuer,
            String state,
            LocalDate issuedAt,
            LocalDate expiresAt,
            String notes,
            boolean primary) {
    }

    public record ContactView(
            UUID id,
            String name,
            String relationship,
            String phone,
            String email,
            boolean primary,
            String notes) {
    }

    public record AddressView(
            UUID id,
            String zipCode,
            String street,
            String number,
            String complement,
            String neighborhood,
            String city,
            String state) {
    }

    public record BranchAssignmentView(
            UUID id,
            UUID branchId,
            String branchName,
            boolean primary,
            Instant startedAt,
            Instant endedAt,
            String transferReason) {
    }

    public record HistoryView(
            Instant occurredAt,
            String kind,
            String title,
            String detail,
            UUID actorId) {
    }

    public record NoteView(
            UUID id,
            String content,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt) {
    }

    public static StudentSummary summary(Student student, String cpfMasked, Branch branch) {
        return new StudentSummary(student.id(), student.registrationNumber(), student.name(),
                student.socialName(), cpfMasked, student.phone(), student.status().name(),
                branch == null ? null : branch.id(), branch == null ? null : branch.name(),
                student.createdAt());
    }
}
