package com.gymsaas.student.application;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.student.application.usecase.AddStudentContactUseCase;
import com.gymsaas.student.application.usecase.AddStudentDocumentUseCase;
import com.gymsaas.student.application.usecase.GetStudentAddressUseCase;
import com.gymsaas.student.application.usecase.RemoveStudentDocumentUseCase;
import com.gymsaas.student.application.usecase.UpdateStudentDocumentUseCase;
import com.gymsaas.student.application.usecase.UpsertStudentAddressUseCase;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentAddress;
import com.gymsaas.student.domain.StudentAddressRepository;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import com.gymsaas.student.domain.StudentContact;
import com.gymsaas.student.domain.StudentContactRepository;
import com.gymsaas.student.domain.StudentDocument;
import com.gymsaas.student.domain.StudentDocumentRepository;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.student.domain.StudentStatus;
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
class StudentProfileUseCasesTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final String VALID_CPF = "52998224725";

    @Mock private StudentRepository students;
    @Mock private StudentDocumentRepository documents;
    @Mock private StudentContactRepository contacts;
    @Mock private StudentAddressRepository addresses;
    @Mock private StudentBranchAssignmentRepository assignments;
    @Mock private BranchRepository branches;
    @Mock private AuditService audit;

    private StudentGuard guard;
    private AddStudentDocumentUseCase addDocument;
    private UpdateStudentDocumentUseCase updateDocument;
    private RemoveStudentDocumentUseCase removeDocument;
    private AddStudentContactUseCase addContact;
    private UpsertStudentAddressUseCase upsertAddress;
    private GetStudentAddressUseCase getAddress;

    @BeforeEach
    void setUp() {
        guard = new StudentGuard(students, documents, branches);
        addDocument = new AddStudentDocumentUseCase(guard, documents, audit);
        updateDocument = new UpdateStudentDocumentUseCase(guard, documents, audit);
        removeDocument = new RemoveStudentDocumentUseCase(guard, documents, audit);
        addContact = new AddStudentContactUseCase(guard, contacts, audit);
        upsertAddress = new UpsertStudentAddressUseCase(guard, addresses, audit);
        getAddress = new GetStudentAddressUseCase(guard, addresses);
        when(students.findById(TENANT, STUDENT_ID)).thenReturn(Optional.of(student()));
    }

    @Test
    void addCpfDocumentRejectsDuplicatedCpf() {
        when(documents.existsCpf(TENANT, VALID_CPF, null)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> addDocument.execute(TENANT,
                STUDENT_ID, documentRequest("CPF", "529.982.247-25", false)));
        assertEquals("CPF_ALREADY_EXISTS", exception.code());
        assertEquals(409, exception.status().value());
        verify(documents, never()).save(any());
    }

    @Test
    void addCpfDocumentRejectsInvalidCpf() {
        ApiException exception = assertThrows(ApiException.class, () -> addDocument.execute(TENANT,
                STUDENT_ID, documentRequest("CPF", "111.111.111-11", false)));
        assertEquals("INVALID_CPF", exception.code());
    }

    @Test
    void addCpfDocumentStoresNormalizedNumberAndMasksResponse() {
        when(documents.save(any(StudentDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentDtos.DocumentView view = addDocument.execute(TENANT, STUDENT_ID,
                documentRequest("CPF", "529.982.247-25", true));

        assertEquals("***.***.***-25", view.number());
        assertTrue(view.primary());
        verify(documents).clearPrimary(TENANT, STUDENT_ID, "CPF", null);
        verify(audit).recordForTenant(eq(TENANT), eq("STUDENT_DOCUMENT_ADDED"), eq("student"),
                eq(STUDENT_ID), eq("HIGH"), any(), any(), any());
    }

    @Test
    void updateDocumentKeepsIdentityAndExcludesItselfFromCpfDuplicity() {
        UUID documentId = UUID.randomUUID();
        StudentDocument current = document(documentId);
        when(documents.findById(TENANT, STUDENT_ID, documentId)).thenReturn(Optional.of(current));
        when(documents.existsCpf(TENANT, VALID_CPF, documentId)).thenReturn(false);
        when(documents.save(any(StudentDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentDtos.DocumentView view = updateDocument.execute(TENANT, STUDENT_ID, documentId,
                documentRequest("CPF", VALID_CPF, false));

        assertEquals(documentId, view.id());
        assertEquals("***.***.***-25", view.number());
    }

    @Test
    void removeDocumentSoftDeletesWithinTenant() {
        UUID documentId = UUID.randomUUID();
        when(documents.findById(TENANT, STUDENT_ID, documentId)).thenReturn(Optional.of(document(documentId)));

        removeDocument.execute(TENANT, STUDENT_ID, documentId);

        verify(documents).softDelete(eq(TENANT), eq(STUDENT_ID), eq(documentId), any());
    }

    @Test
    void firstContactBecomesPrimaryAutomatically() {
        when(contacts.findByStudent(TENANT, STUDENT_ID)).thenReturn(List.of());
        when(contacts.save(any(StudentContact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentDtos.ContactView view = addContact.execute(TENANT, STUDENT_ID,
                new StudentDtos.ContactRequest("João", "Pai", "(11) 98888-7777",
                        "joao@example.com", false, null));

        assertTrue(view.primary());
        assertEquals("11988887777", view.phone().replaceAll("\\D", ""));
    }

    @Test
    void primaryContactClearsPreviousPrimary() {
        when(contacts.findByStudent(TENANT, STUDENT_ID)).thenReturn(List.of(contact()));
        when(contacts.save(any(StudentContact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        addContact.execute(TENANT, STUDENT_ID, new StudentDtos.ContactRequest("João", "Pai",
                "(11) 98888-7777", null, true, null));

        verify(contacts).clearPrimary(TENANT, STUDENT_ID, null);
    }

    @Test
    void addressUpsertReusesExistingRow() {
        UUID addressId = UUID.randomUUID();
        StudentAddress existing = new StudentAddress(addressId, TENANT, STUDENT_ID, "01001000",
                "Rua A", "10", null, "Centro", "São Paulo", "SP",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
        when(addresses.findByStudent(TENANT, STUDENT_ID)).thenReturn(Optional.of(existing));
        when(addresses.save(any(StudentAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentDtos.AddressView view = upsertAddress.execute(TENANT, STUDENT_ID,
                new StudentDtos.AddressRequest("01001-000", "Rua B", "20", null, "Centro",
                        "São Paulo", "SP"));

        assertEquals(addressId, view.id());
        assertEquals("Rua B", view.street());
    }

    @Test
    void addressFromAnotherTenantIsNotFound() {
        when(addresses.findByStudent(TENANT, STUDENT_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> getAddress.execute(TENANT, STUDENT_ID));
        assertEquals("ADDRESS_NOT_FOUND", exception.code());
    }

    private StudentDtos.DocumentRequest documentRequest(String type, String number, boolean primary) {
        return new StudentDtos.DocumentRequest(type, number, null, null, null,
                LocalDate.of(2030, 1, 1), null, primary);
    }

    private Student student() {
        Instant now = Instant.parse("2026-01-10T10:00:00Z");
        return new Student(STUDENT_ID, TENANT, "202600001", "Maria Silva", null,
                LocalDate.of(1990, 5, 1), "maria@example.com", "11999998888", "FEMALE",
                null, StudentStatus.ACTIVE, null, 0, now, now, null);
    }

    private StudentDocument document(UUID id) {
        Instant now = Instant.parse("2026-01-10T10:00:00Z");
        return new StudentDocument(id, TENANT, STUDENT_ID, "CPF", VALID_CPF, VALID_CPF,
                null, null, null, null, null, true, now, now, null);
    }

    private StudentContact contact() {
        Instant now = Instant.parse("2026-01-10T10:00:00Z");
        return new StudentContact(UUID.randomUUID(), TENANT, STUDENT_ID, "Maria", "Mãe",
                "11999998888", null, true, null, now, now, null);
    }
}
