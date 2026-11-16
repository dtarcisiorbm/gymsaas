package com.gymsaas.student.application;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentAddress;
import com.gymsaas.student.domain.StudentBranchAssignment;
import com.gymsaas.student.domain.StudentContact;
import com.gymsaas.student.domain.StudentDocument;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure mapping from domain records to API views. */
public final class StudentAssembler {

    private StudentAssembler() {
    }

    public static StudentDtos.StudentDetail detail(Student student,
                                                   List<StudentDocument> documents,
                                                   List<StudentContact> contacts,
                                                   StudentAddress address,
                                                   List<StudentBranchAssignment> assignments,
                                                   Map<UUID, Branch> branches,
                                                   boolean revealSensitive) {
        String cpf = cpfValue(documents);
        return new StudentDtos.StudentDetail(
                student.id(), student.tenantId(), student.registrationNumber(), student.name(),
                student.socialName(), student.birthDate(), student.email(), student.phone(),
                student.gender(), student.photoUrl(), student.status().name(), student.notes(),
                revealSensitive ? cpf : null,
                cpf == null ? null : BrDocuments.maskCpf(cpf),
                address == null ? null : new StudentDtos.AddressView(address.id(), address.zipCode(),
                        address.street(), address.number(), address.complement(), address.neighborhood(),
                        address.city(), address.state()),
                documents.stream().map(document -> document(document, revealSensitive)).toList(),
                contacts.stream().map(StudentAssembler::contact).toList(),
                assignments.stream().map(assignment -> assignment(assignment, branches)).toList(),
                student.createdAt(), student.updatedAt());
    }

    public static StudentDtos.DocumentView document(StudentDocument document, boolean revealSensitive) {
        String number = document.number();
        if (!revealSensitive) {
            number = "CPF".equals(document.type())
                    ? BrDocuments.maskCpf(document.normalizedNumber())
                    : maskGeneric(number);
        }
        return new StudentDtos.DocumentView(document.id(), document.type(), number,
                document.issuer(), document.state(), document.issuedAt(), document.expiresAt(),
                document.notes(), document.primary());
    }

    public static StudentDtos.ContactView contact(StudentContact contact) {
        return new StudentDtos.ContactView(contact.id(), contact.name(), contact.relationship(),
                contact.phone(), contact.email(), contact.primary(), contact.notes());
    }

    public static StudentDtos.BranchAssignmentView assignment(StudentBranchAssignment assignment,
                                                              Map<UUID, Branch> branches) {
        Branch branch = assignment.branchId() == null ? null : branches.get(assignment.branchId());
        return new StudentDtos.BranchAssignmentView(assignment.id(), assignment.branchId(),
                branch == null ? null : branch.name(), assignment.primary(), assignment.startedAt(),
                assignment.endedAt(), assignment.transferReason());
    }

    public static String cpfValue(List<StudentDocument> documents) {
        if (documents == null) {
            return null;
        }
        return documents.stream()
                .filter(document -> "CPF".equals(document.type()))
                .map(StudentDocument::normalizedNumber)
                .findFirst()
                .orElse(null);
    }

    private static String maskGeneric(String number) {
        if (number == null || number.length() <= 4) {
            return "****";
        }
        return "*".repeat(number.length() - 4) + number.substring(number.length() - 4);
    }
}
