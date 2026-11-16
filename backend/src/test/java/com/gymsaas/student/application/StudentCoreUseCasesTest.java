package com.gymsaas.student.application;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.student.application.usecase.ChangeStudentStatusUseCase;
import com.gymsaas.student.application.usecase.CreateStudentUseCase;
import com.gymsaas.student.application.usecase.FindStudentByIdUseCase;
import com.gymsaas.student.application.usecase.ListStudentsUseCase;
import com.gymsaas.student.application.usecase.UpdateStudentUseCase;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentAddressRepository;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import com.gymsaas.student.domain.StudentContactRepository;
import com.gymsaas.student.domain.StudentDocument;
import com.gymsaas.student.domain.StudentDocumentRepository;
import com.gymsaas.student.domain.StudentNoteRepository;
import com.gymsaas.student.domain.StudentRegistrationGenerator;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.student.domain.StudentStatus;
import com.gymsaas.student.domain.StudentStatusEntry;
import com.gymsaas.student.domain.StudentStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCoreUseCasesTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final String VALID_CPF = "52998224725";

    @Mock private StudentRepository students;
    @Mock private StudentDocumentRepository documents;
    @Mock private StudentContactRepository contacts;
    @Mock private StudentAddressRepository addresses;
    @Mock private StudentBranchAssignmentRepository assignments;
    @Mock private StudentStatusHistoryRepository statusHistory;
    @Mock private StudentNoteRepository notes;
    @Mock private BranchRepository branches;
    @Mock private StudentRegistrationGenerator registration;
    @Mock private AuditService audit;

    private StudentGuard guard;
    private CreateStudentUseCase create;
    private UpdateStudentUseCase update;
    private FindStudentByIdUseCase find;
    private ListStudentsUseCase list;
    private ChangeStudentStatusUseCase changeStatus;

    @BeforeEach
    void setUp() {
        guard = new StudentGuard(students, documents, branches);
        create = new CreateStudentUseCase(guard, students, registration, documents, contacts,
                addresses, assignments, statusHistory, audit);
        update = new UpdateStudentUseCase(guard, students, documents, contacts, addresses,
                assignments, branches, audit);
        find = new FindStudentByIdUseCase(guard, documents, contacts, addresses, assignments, branches);
        list = new ListStudentsUseCase(students, documents, assignments, branches);
        changeStatus = new ChangeStudentStatusUseCase(guard, students, statusHistory, documents,
                contacts, addresses, assignments, branches, audit);
    }

    @Test
    void createsStudentWithGeneratedRegistrationAndPendingStatus() {
        when(registration.next(TENANT)).thenReturn("202600001");
        when(students.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documents.save(any(StudentDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statusHistory.save(any(StudentStatusEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentDtos.StudentDetail detail = create.execute(TENANT, request("Maria Silva", VALID_CPF));

        assertEquals("202600001", detail.registrationNumber());
        assertEquals("PENDING", detail.status());
        assertEquals("Maria Silva", detail.name());
        assertEquals("***.***.***-25", detail.cpfMasked());
        assertNotNull(detail.id());
        verify(audit).recordForTenant(eq(TENANT), eq("STUDENT_CREATED"), eq("student"),
                any(UUID.class), eq("MEDIUM"), any(), any(), any());
    }

    @Test
    void rejectsInvalidCpf() {
        ApiException exception = assertThrows(ApiException.class,
                () -> create.execute(TENANT, request("Maria Silva", "111.111.111-11")));
        assertEquals("INVALID_CPF", exception.code());
        assertEquals(400, exception.status().value());
    }

    @Test
    void rejectsDuplicatedCpfInTenant() {
        when(students.existsActiveCpf(TENANT, VALID_CPF, null)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> create.execute(TENANT, request("Maria Silva", VALID_CPF)));
        assertEquals("CPF_ALREADY_EXISTS", exception.code());
        assertEquals(409, exception.status().value());
    }

    @Test
    void rejectsUnknownPrimaryBranch() {
        when(branches.findById(eq(TENANT), any(UUID.class))).thenReturn(Optional.empty());
        UUID branchId = UUID.randomUUID();
        StudentDtos.CreateStudentRequest request = new StudentDtos.CreateStudentRequest(
                "Maria Silva", null, null, null, null, null, null, null, null, null,
                List.of(), null, branchId);

        ApiException exception = assertThrows(ApiException.class, () -> create.execute(TENANT, request));
        assertEquals("BRANCH_NOT_FOUND", exception.code());
    }

    @Test
    void updateKeepsRegistrationAndStatus() {
        Student current = student(StudentStatus.ACTIVE);
        when(students.findById(TENANT, STUDENT_ID)).thenReturn(Optional.of(current));
        when(students.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documents.findByStudent(TENANT, STUDENT_ID)).thenReturn(List.of());

        StudentDtos.UpdateStudentRequest request = new StudentDtos.UpdateStudentRequest(
                "Maria Souza", "Mari", LocalDate.of(1990, 5, 1), "maria@example.com",
                "11999998888", "FEMALE", null, "Nova observação", null);

        StudentDtos.StudentDetail detail = update.execute(TENANT, STUDENT_ID, request);

        assertEquals("202600001", detail.registrationNumber());
        assertEquals("ACTIVE", detail.status());
        assertEquals("Maria Souza", detail.name());
        assertEquals("Mari", detail.socialName());
    }

    @Test
    void findMasksCpfByDefaultAndRevealsWhenAllowed() {
        Student current = student(StudentStatus.ACTIVE);
        StudentDocument cpf = cpfDocument();
        when(students.findById(TENANT, STUDENT_ID)).thenReturn(Optional.of(current));
        when(documents.findByStudent(TENANT, STUDENT_ID)).thenReturn(List.of(cpf));
        when(contacts.findByStudent(TENANT, STUDENT_ID)).thenReturn(List.of());
        when(addresses.findByStudent(TENANT, STUDENT_ID)).thenReturn(Optional.empty());
        when(assignments.findByStudent(TENANT, STUDENT_ID)).thenReturn(List.of());

        StudentDtos.StudentDetail masked = find.execute(TENANT, STUDENT_ID, false);
        StudentDtos.StudentDetail revealed = find.execute(TENANT, STUDENT_ID, true);

        assertEquals("***.***.***-25", masked.cpfMasked());
        assertEquals(null, masked.cpf());
        assertEquals(VALID_CPF, revealed.cpf());
    }

    @Test
    void listClampsSizeAndFallsBackToAllowedSort() {
        Student current = student(StudentStatus.ACTIVE);
        when(students.search(eq(TENANT), any())).thenReturn(PageResult.of(List.of(current), 1, 0, 100));
        when(documents.findCpfByStudentIds(TENANT, List.of(STUDENT_ID))).thenReturn(List.of(cpfDocument()));
        when(assignments.findActiveByStudentIds(TENANT, List.of(STUDENT_ID))).thenReturn(List.of());
        when(branches.findAllByIds(eq(TENANT), any())).thenReturn(List.of());

        StudentDtos.StudentFilter filter = new StudentDtos.StudentFilter(
                "Maria", null, "ACTIVE", null, 0, 1000, "password", "asc");
        PageResult<StudentDtos.StudentSummary> page = list.execute(TENANT, filter);

        ArgumentCaptor<com.gymsaas.student.domain.StudentSearchCriteria> captor =
                ArgumentCaptor.forClass(com.gymsaas.student.domain.StudentSearchCriteria.class);
        verify(students).search(eq(TENANT), captor.capture());
        assertEquals(100, captor.getValue().size());
        assertEquals("name", captor.getValue().sort());
        assertEquals(1, page.totalElements());
        assertEquals("***.***.***-25", page.content().get(0).cpfMasked());
    }

    @Test
    void statusChangeRecordsHistory() {
        Student current = student(StudentStatus.ACTIVE);
        when(students.findById(TENANT, STUDENT_ID)).thenReturn(Optional.of(current));
        when(students.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statusHistory.save(any(StudentStatusEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documents.findByStudent(TENANT, STUDENT_ID)).thenReturn(List.of());

        StudentDtos.StudentDetail detail = changeStatus.execute(TENANT, STUDENT_ID,
                new StudentDtos.ChangeStudentStatusRequest("BLOCKED", "Chargeback"));

        assertEquals("BLOCKED", detail.status());
        ArgumentCaptor<StudentStatusEntry> captor = ArgumentCaptor.forClass(StudentStatusEntry.class);
        verify(statusHistory).save(captor.capture());
        assertEquals(StudentStatus.ACTIVE, captor.getValue().fromStatus());
        assertEquals(StudentStatus.BLOCKED, captor.getValue().toStatus());
        assertEquals("Chargeback", captor.getValue().reason());
    }

    @Test
    void invalidStatusTransitionFails() {
        Student archived = student(StudentStatus.ARCHIVED);
        when(students.findById(TENANT, STUDENT_ID)).thenReturn(Optional.of(archived));

        ApiException exception = assertThrows(ApiException.class, () -> changeStatus.execute(TENANT,
                STUDENT_ID, new StudentDtos.ChangeStudentStatusRequest("BLOCKED", null)));
        assertEquals("INVALID_STATUS_TRANSITION", exception.code());
        assertEquals(409, exception.status().value());
    }

    @Test
    void studentFromAnotherTenantIsNotFound() {
        when(students.findById(TENANT, STUDENT_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> find.execute(TENANT, STUDENT_ID, false));
        assertEquals("STUDENT_NOT_FOUND", exception.code());
        assertEquals(404, exception.status().value());
    }

    private StudentDtos.CreateStudentRequest request(String name, String cpf) {
        return new StudentDtos.CreateStudentRequest(name, null, LocalDate.of(1990, 5, 1),
                "maria@example.com", "(11) 99999-8888", "FEMALE", null, null, null, cpf,
                List.of(), null, null);
    }

    private Student student(StudentStatus status) {
        Instant now = Instant.parse("2026-01-10T10:00:00Z");
        return new Student(STUDENT_ID, TENANT, "202600001", "Maria Silva", null,
                LocalDate.of(1990, 5, 1), "maria@example.com", "11999998888", "FEMALE",
                null, status, null, 0, now, now, null);
    }

    private StudentDocument cpfDocument() {
        Instant now = Instant.parse("2026-01-10T10:00:00Z");
        return new StudentDocument(UUID.randomUUID(), TENANT, STUDENT_ID, "CPF", VALID_CPF,
                VALID_CPF, null, null, null, null, null, true, now, now, null);
    }
}
