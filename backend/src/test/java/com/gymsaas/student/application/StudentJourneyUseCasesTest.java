package com.gymsaas.student.application;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.student.application.usecase.AddStudentNoteUseCase;
import com.gymsaas.student.application.usecase.AssignStudentToBranchUseCase;
import com.gymsaas.student.application.usecase.GetStudentHistoryUseCase;
import com.gymsaas.student.application.usecase.ListStudentNotesUseCase;
import com.gymsaas.student.application.usecase.RemoveStudentBranchAssignmentUseCase;
import com.gymsaas.student.application.usecase.TransferStudentBranchUseCase;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentBranchAssignment;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import com.gymsaas.student.domain.StudentNote;
import com.gymsaas.student.domain.StudentNoteRepository;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.student.domain.StudentStatus;
import com.gymsaas.student.domain.StudentStatusEntry;
import com.gymsaas.student.domain.StudentStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentJourneyUseCasesTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID BRANCH_A = UUID.randomUUID();
    private static final UUID BRANCH_B = UUID.randomUUID();

    @Mock private StudentRepository students;
    @Mock private StudentStatusHistoryRepository statusHistory;
    @Mock private StudentBranchAssignmentRepository assignments;
    @Mock private StudentNoteRepository notes;
    @Mock private BranchRepository branches;
    @Mock private com.gymsaas.student.domain.StudentDocumentRepository documents;
    @Mock private AuditService audit;

    private StudentGuard guard;
    private AssignStudentToBranchUseCase assign;
    private TransferStudentBranchUseCase transfer;
    private RemoveStudentBranchAssignmentUseCase remove;
    private GetStudentHistoryUseCase history;
    private AddStudentNoteUseCase addNote;
    private ListStudentNotesUseCase listNotes;

    @BeforeEach
    void setUp() {
        guard = new StudentGuard(students, documents, branches);
        assign = new AssignStudentToBranchUseCase(guard, assignments, audit);
        transfer = new TransferStudentBranchUseCase(guard, assignments, audit);
        remove = new RemoveStudentBranchAssignmentUseCase(guard, assignments, audit);
        history = new GetStudentHistoryUseCase(guard, statusHistory, assignments, notes, branches);
        addNote = new AddStudentNoteUseCase(guard, notes, audit);
        listNotes = new ListStudentNotesUseCase(guard, notes);
        when(students.findById(TENANT, STUDENT_ID)).thenReturn(Optional.of(student()));
    }

    @Test
    void firstAssignmentBecomesPrimary() {
        when(branches.findById(TENANT, BRANCH_A)).thenReturn(Optional.of(branch(BRANCH_A, "Centro")));
        when(assignments.save(any(StudentBranchAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentDtos.BranchAssignmentView view = assign.execute(TENANT, STUDENT_ID,
                new StudentDtos.AssignBranchRequest(BRANCH_A, false));

        assertTrue(view.primary());
        assertEquals("Centro", view.branchName());
        verify(assignments).clearPrimary(TENANT, STUDENT_ID);
    }

    @Test
    void assigningDuplicatedActiveBranchConflicts() {
        when(branches.findById(TENANT, BRANCH_A)).thenReturn(Optional.of(branch(BRANCH_A, "Centro")));
        when(assignments.existsActive(TENANT, STUDENT_ID, BRANCH_A)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> assign.execute(TENANT,
                STUDENT_ID, new StudentDtos.AssignBranchRequest(BRANCH_A, false)));
        assertEquals("BRANCH_ALREADY_ASSIGNED", exception.code());
        assertEquals(409, exception.status().value());
        verify(assignments, never()).save(any());
    }

    @Test
    void assigningBranchFromAnotherTenantIsNotFound() {
        when(branches.findById(TENANT, BRANCH_B)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> assign.execute(TENANT,
                STUDENT_ID, new StudentDtos.AssignBranchRequest(BRANCH_B, false)));
        assertEquals("BRANCH_NOT_FOUND", exception.code());
    }

    @Test
    void transferClosesCurrentPrimaryAndCreatesNewPrimary() {
        UUID assignmentId = UUID.randomUUID();
        when(branches.findById(TENANT, BRANCH_B)).thenReturn(Optional.of(branch(BRANCH_B, "Sul")));
        when(assignments.findActivePrimary(TENANT, STUDENT_ID))
                .thenReturn(Optional.of(assignment(assignmentId, BRANCH_A, true, null)));
        when(assignments.save(any(StudentBranchAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentDtos.BranchAssignmentView view = transfer.execute(TENANT, STUDENT_ID,
                new StudentDtos.TransferBranchRequest(BRANCH_B, "Mudança de bairro"));

        assertEquals(BRANCH_B, view.branchId());
        assertTrue(view.primary());
        verify(assignments).end(eq(TENANT), eq(STUDENT_ID), eq(assignmentId), any(), eq("Mudança de bairro"));
    }

    @Test
    void transferToSameBranchConflicts() {
        when(branches.findById(TENANT, BRANCH_A)).thenReturn(Optional.of(branch(BRANCH_A, "Centro")));
        when(assignments.findActivePrimary(TENANT, STUDENT_ID))
                .thenReturn(Optional.of(assignment(UUID.randomUUID(), BRANCH_A, true, null)));

        ApiException exception = assertThrows(ApiException.class, () -> transfer.execute(TENANT,
                STUDENT_ID, new StudentDtos.TransferBranchRequest(BRANCH_A, null)));
        assertEquals("SAME_BRANCH", exception.code());
    }

    @Test
    void removingPrimaryPromotesOldestRemainingAssignment() {
        UUID primaryId = UUID.randomUUID();
        StudentBranchAssignment primary = assignment(primaryId, BRANCH_A, true, null);
        StudentBranchAssignment remaining = assignment(UUID.randomUUID(), BRANCH_B, false, null);
        when(assignments.findById(TENANT, STUDENT_ID, primaryId)).thenReturn(Optional.of(primary));
        when(assignments.findActive(TENANT, STUDENT_ID)).thenReturn(List.of(remaining));
        when(assignments.save(any(StudentBranchAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        remove.execute(TENANT, STUDENT_ID, primaryId);

        verify(assignments).end(eq(TENANT), eq(STUDENT_ID), eq(primaryId), any(), eq("Removido"));
        verify(assignments).save(any(StudentBranchAssignment.class));
    }

    @Test
    void historyAggregatesStatusAssignmentsAndNotesNewestFirst() {
        Instant base = Instant.parse("2026-01-10T10:00:00Z");
        StudentStatusEntry status = new StudentStatusEntry(UUID.randomUUID(), TENANT, STUDENT_ID,
                StudentStatus.PENDING, StudentStatus.ACTIVE, "Ativação", null, base.plusSeconds(50));
        StudentBranchAssignment active = new StudentBranchAssignment(UUID.randomUUID(), TENANT,
                STUDENT_ID, BRANCH_A, true, base.plusSeconds(20), null, null, null,
                base.plusSeconds(20), base.plusSeconds(20));
        StudentBranchAssignment ended = new StudentBranchAssignment(UUID.randomUUID(), TENANT,
                STUDENT_ID, BRANCH_B, false, base.plusSeconds(10), base.plusSeconds(30),
                "Transferência", null, base.plusSeconds(10), base.plusSeconds(30));
        StudentNote note = new StudentNote(UUID.randomUUID(), TENANT, STUDENT_ID, "Observação",
                null, base.plusSeconds(40), base.plusSeconds(40), null);

        when(statusHistory.findByStudent(TENANT, STUDENT_ID)).thenReturn(List.of(status));
        when(assignments.findByStudent(TENANT, STUDENT_ID)).thenReturn(List.of(active, ended));
        when(notes.findByStudent(TENANT, STUDENT_ID)).thenReturn(List.of(note));
        when(branches.findAllByIds(TENANT, List.of(BRANCH_A, BRANCH_B)))
                .thenReturn(List.of(branch(BRANCH_A, "Centro"), branch(BRANCH_B, "Sul")));

        List<StudentDtos.HistoryView> entries = history.execute(TENANT, STUDENT_ID);

        assertEquals(5, entries.size());
        assertEquals("STATUS", entries.get(0).kind());
        assertEquals(base.plusSeconds(50), entries.get(0).occurredAt());
    }

    @Test
    void addNoteTrimsContentAndAudits() {
        when(notes.save(any(StudentNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentDtos.NoteView view = addNote.execute(TENANT, STUDENT_ID,
                new StudentDtos.NoteRequest("  Aluno pediu contato por WhatsApp.  "));

        assertEquals("Aluno pediu contato por WhatsApp.", view.content());
        verify(audit).recordForTenant(eq(TENANT), eq("STUDENT_NOTE_ADDED"), eq("student"),
                eq(STUDENT_ID), eq("LOW"), any(), any(), any());
    }

    @Test
    void listNotesReturnsRepositoryOrder() {
        StudentNote note = new StudentNote(UUID.randomUUID(), TENANT, STUDENT_ID, "Nota",
                null, Instant.now(), Instant.now(), null);
        when(notes.findByStudent(TENANT, STUDENT_ID)).thenReturn(List.of(note));

        assertEquals(1, listNotes.execute(TENANT, STUDENT_ID).size());
    }

    private Student student() {
        Instant now = Instant.parse("2026-01-10T10:00:00Z");
        return new Student(STUDENT_ID, TENANT, "202600001", "Maria Silva", null,
                LocalDate.of(1990, 5, 1), "maria@example.com", "11999998888", "FEMALE",
                null, StudentStatus.ACTIVE, null, 0, now, now, null);
    }

    private Branch branch(UUID id, String name) {
        Instant now = Instant.parse("2026-01-10T10:00:00Z");
        return new Branch(id, TENANT, name, "BR" + id.toString().substring(0, 4), null,
                null, null, null, null, null, null, null, null, null, "ACTIVE", now, now);
    }

    private StudentBranchAssignment assignment(UUID id, UUID branchId, boolean primary, Instant endedAt) {
        Instant now = Instant.parse("2026-01-10T10:00:00Z");
        return new StudentBranchAssignment(id, TENANT, STUDENT_ID, branchId, primary, now, endedAt,
                null, null, now, now);
    }
}
