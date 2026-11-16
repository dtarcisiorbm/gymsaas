package com.gymsaas.student.application.usecase;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentBranchAssignment;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import com.gymsaas.student.domain.StudentNote;
import com.gymsaas.student.domain.StudentNoteRepository;
import com.gymsaas.student.domain.StudentStatusEntry;
import com.gymsaas.student.domain.StudentStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GetStudentHistoryUseCase {

    private final StudentGuard guard;
    private final StudentStatusHistoryRepository statusHistory;
    private final StudentBranchAssignmentRepository assignments;
    private final StudentNoteRepository notes;
    private final BranchRepository branches;

    public GetStudentHistoryUseCase(StudentGuard guard,
                                    StudentStatusHistoryRepository statusHistory,
                                    StudentBranchAssignmentRepository assignments,
                                    StudentNoteRepository notes,
                                    BranchRepository branches) {
        this.guard = guard;
        this.statusHistory = statusHistory;
        this.assignments = assignments;
        this.notes = notes;
        this.branches = branches;
    }

    @Transactional(readOnly = true)
    public List<StudentDtos.HistoryView> execute(UUID tenantId, UUID studentId) {
        guard.requireStudent(tenantId, studentId);

        List<StudentBranchAssignment> studentAssignments = assignments.findByStudent(tenantId, studentId);
        List<UUID> branchIds = studentAssignments.stream()
                .map(StudentBranchAssignment::branchId)
                .distinct()
                .toList();
        Map<UUID, Branch> branchMap = branchIds.isEmpty()
                ? Map.of()
                : branches.findAllByIds(tenantId, branchIds).stream()
                        .collect(Collectors.toMap(Branch::id, branch -> branch));

        List<StudentDtos.HistoryView> entries = new ArrayList<>();

        for (StudentStatusEntry entry : statusHistory.findByStudent(tenantId, studentId)) {
            String detail = entry.fromStatus() == null
                    ? entry.toStatus().name()
                    : entry.fromStatus().name() + " → " + entry.toStatus().name();
            entries.add(new StudentDtos.HistoryView(entry.changedAt(), "STATUS",
                    "Alteração de status", detail + reasonSuffix(entry.reason()), entry.changedBy()));
        }

        for (StudentBranchAssignment assignment : studentAssignments) {
            String branchName = branchName(branchMap, assignment.branchId());
            entries.add(new StudentDtos.HistoryView(assignment.startedAt(), "BRANCH",
                    "Vínculo com unidade", branchName, assignment.assignedBy()));
            if (assignment.endedAt() != null) {
                entries.add(new StudentDtos.HistoryView(assignment.endedAt(), "BRANCH",
                        "Encerramento de vínculo",
                        branchName + reasonSuffix(assignment.transferReason()), assignment.assignedBy()));
            }
        }

        for (StudentNote note : notes.findByStudent(tenantId, studentId)) {
            entries.add(new StudentDtos.HistoryView(note.createdAt(), "NOTE",
                    "Observação", note.content(), note.createdBy()));
        }

        return entries.stream()
                .sorted(Comparator.comparing(StudentDtos.HistoryView::occurredAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    private String branchName(Map<UUID, Branch> branchMap, UUID branchId) {
        Branch branch = branchId == null ? null : branchMap.get(branchId);
        return branch == null ? "" : branch.name();
    }

    private String reasonSuffix(String reason) {
        return reason == null || reason.isBlank() ? "" : " — " + reason;
    }
}
