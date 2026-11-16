package com.gymsaas.student.application.usecase;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.student.application.StudentAssembler;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentBranchAssignment;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import com.gymsaas.student.domain.StudentDocument;
import com.gymsaas.student.domain.StudentDocumentRepository;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.student.domain.StudentSearchCriteria;
import com.gymsaas.student.domain.StudentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ListStudentsUseCase {

    private static final Set<String> ALLOWED_SORTS =
            Set.of("name", "registrationNumber", "createdAt", "status");

    private final StudentRepository students;
    private final StudentDocumentRepository documents;
    private final StudentBranchAssignmentRepository assignments;
    private final BranchRepository branches;

    public ListStudentsUseCase(StudentRepository students, StudentDocumentRepository documents,
                               StudentBranchAssignmentRepository assignments, BranchRepository branches) {
        this.students = students;
        this.documents = documents;
        this.assignments = assignments;
        this.branches = branches;
    }

    @Transactional(readOnly = true)
    public PageResult<StudentDtos.StudentSummary> execute(UUID tenantId, StudentDtos.StudentFilter filter) {
        int page = Math.max(0, filter.page());
        int size = filter.size() <= 0 ? 20 : Math.min(filter.size(), 100);
        String sort = filter.sort() == null ? "name" : filter.sort().trim();
        if (!ALLOWED_SORTS.contains(sort)) {
            sort = "name";
        }
        String direction = "desc".equalsIgnoreCase(filter.direction()) ? "desc" : "asc";
        StudentStatus status = filter.status() == null || filter.status().isBlank()
                ? null : parseStatus(filter.status());
        String document = filter.document() == null || filter.document().isBlank()
                ? null : BrDocuments.document(filter.document());

        StudentSearchCriteria criteria = new StudentSearchCriteria(
                filter.search() == null ? null : filter.search().trim(),
                document, status, filter.branchId(), page, size, sort, direction);
        PageResult<Student> result = students.search(tenantId, criteria);

        List<UUID> studentIds = result.content().stream().map(Student::id).toList();
        Map<UUID, String> cpfByStudent = new HashMap<>();
        if (!studentIds.isEmpty()) {
            for (StudentDocument doc : documents.findCpfByStudentIds(tenantId, studentIds)) {
                cpfByStudent.putIfAbsent(doc.studentId(), doc.normalizedNumber());
            }
        }

        Map<UUID, StudentBranchAssignment> primaryByStudent = new HashMap<>();
        if (!studentIds.isEmpty()) {
            for (StudentBranchAssignment assignment : assignments.findActiveByStudentIds(tenantId, studentIds)) {
                primaryByStudent.merge(assignment.studentId(), assignment,
                        (current, candidate) -> current.primary() ? current : candidate);
            }
        }

        List<UUID> branchIds = primaryByStudent.values().stream()
                .map(StudentBranchAssignment::branchId).distinct().toList();
        Map<UUID, Branch> branchMap = branches.findAllByIds(tenantId, branchIds).stream()
                .collect(Collectors.toMap(Branch::id, branch -> branch));

        List<StudentDtos.StudentSummary> content = result.content().stream()
                .map(student -> {
                    StudentBranchAssignment primary = primaryByStudent.get(student.id());
                    String cpf = cpfByStudent.get(student.id());
                    return StudentDtos.summary(student,
                            cpf == null ? null : com.gymsaas.shared.domain.BrDocuments.maskCpf(cpf),
                            primary == null ? null : branchMap.get(primary.branchId()));
                })
                .toList();

        return PageResult.of(content, result.totalElements(), page, size);
    }

    private StudentStatus parseStatus(String status) {
        try {
            return StudentStatus.parse(status);
        } catch (IllegalArgumentException exception) {
            throw com.gymsaas.shared.application.ApiException.badRequest("INVALID_STATUS", "Status inválido.");
        }
    }
}
