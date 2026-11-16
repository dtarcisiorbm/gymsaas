package com.gymsaas.student.application.usecase;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.student.application.StudentAssembler;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentAddressRepository;
import com.gymsaas.student.domain.StudentBranchAssignment;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import com.gymsaas.student.domain.StudentContactRepository;
import com.gymsaas.student.domain.StudentDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FindStudentByIdUseCase {

    private final StudentGuard guard;
    private final StudentDocumentRepository documents;
    private final StudentContactRepository contacts;
    private final StudentAddressRepository addresses;
    private final StudentBranchAssignmentRepository assignments;
    private final BranchRepository branches;

    public FindStudentByIdUseCase(StudentGuard guard, StudentDocumentRepository documents,
                                  StudentContactRepository contacts, StudentAddressRepository addresses,
                                  StudentBranchAssignmentRepository assignments, BranchRepository branches) {
        this.guard = guard;
        this.documents = documents;
        this.contacts = contacts;
        this.addresses = addresses;
        this.assignments = assignments;
        this.branches = branches;
    }

    @Transactional(readOnly = true)
    public StudentDtos.StudentDetail execute(UUID tenantId, UUID studentId, boolean revealSensitive) {
        Student student = guard.requireStudent(tenantId, studentId);
        List<StudentBranchAssignment> studentAssignments = assignments.findByStudent(tenantId, studentId);
        List<UUID> branchIds = studentAssignments.stream()
                .map(StudentBranchAssignment::branchId).distinct().toList();
        Map<UUID, Branch> branchMap = branches.findAllByIds(tenantId, branchIds).stream()
                .collect(Collectors.toMap(Branch::id, branch -> branch));
        return StudentAssembler.detail(student, documents.findByStudent(tenantId, studentId),
                contacts.findByStudent(tenantId, studentId),
                addresses.findByStudent(tenantId, studentId).orElse(null),
                studentAssignments, branchMap, revealSensitive);
    }
}
