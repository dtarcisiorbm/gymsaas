package com.gymsaas.student.presentation;

import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.usecase.AddStudentContactUseCase;
import com.gymsaas.student.application.usecase.AddStudentDocumentUseCase;
import com.gymsaas.student.application.usecase.AddStudentNoteUseCase;
import com.gymsaas.student.application.usecase.AssignStudentToBranchUseCase;
import com.gymsaas.student.application.usecase.ChangeStudentStatusUseCase;
import com.gymsaas.student.application.usecase.CreateStudentUseCase;
import com.gymsaas.student.application.usecase.FindStudentByIdUseCase;
import com.gymsaas.student.application.usecase.GetStudentAddressUseCase;
import com.gymsaas.student.application.usecase.GetStudentHistoryUseCase;
import com.gymsaas.student.application.usecase.ListStudentNotesUseCase;
import com.gymsaas.student.application.usecase.ListStudentsUseCase;
import com.gymsaas.student.application.usecase.RemoveStudentBranchAssignmentUseCase;
import com.gymsaas.student.application.usecase.RemoveStudentContactUseCase;
import com.gymsaas.student.application.usecase.RemoveStudentDocumentUseCase;
import com.gymsaas.student.application.usecase.TransferStudentBranchUseCase;
import com.gymsaas.student.application.usecase.UpdateStudentContactUseCase;
import com.gymsaas.student.application.usecase.UpdateStudentDocumentUseCase;
import com.gymsaas.student.application.usecase.UpdateStudentUseCase;
import com.gymsaas.student.application.usecase.UpsertStudentAddressUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final CreateStudentUseCase createStudent;
    private final UpdateStudentUseCase updateStudent;
    private final FindStudentByIdUseCase findStudent;
    private final ListStudentsUseCase listStudents;
    private final ChangeStudentStatusUseCase changeStudentStatus;
    private final AddStudentDocumentUseCase addDocument;
    private final UpdateStudentDocumentUseCase updateDocument;
    private final RemoveStudentDocumentUseCase removeDocument;
    private final AddStudentContactUseCase addContact;
    private final UpdateStudentContactUseCase updateContact;
    private final RemoveStudentContactUseCase removeContact;
    private final UpsertStudentAddressUseCase upsertAddress;
    private final GetStudentAddressUseCase getAddress;
    private final AssignStudentToBranchUseCase assignBranch;
    private final TransferStudentBranchUseCase transferBranch;
    private final RemoveStudentBranchAssignmentUseCase removeBranch;
    private final GetStudentHistoryUseCase getHistory;
    private final AddStudentNoteUseCase addNote;
    private final ListStudentNotesUseCase listNotes;
    private final com.gymsaas.student.domain.StudentRepository students;

    public StudentController(CreateStudentUseCase createStudent, UpdateStudentUseCase updateStudent,
                             FindStudentByIdUseCase findStudent, ListStudentsUseCase listStudents,
                             ChangeStudentStatusUseCase changeStudentStatus,
                             AddStudentDocumentUseCase addDocument,
                             UpdateStudentDocumentUseCase updateDocument,
                             RemoveStudentDocumentUseCase removeDocument,
                             AddStudentContactUseCase addContact,
                             UpdateStudentContactUseCase updateContact,
                             RemoveStudentContactUseCase removeContact,
                             UpsertStudentAddressUseCase upsertAddress,
                             GetStudentAddressUseCase getAddress,
                             AssignStudentToBranchUseCase assignBranch,
                             TransferStudentBranchUseCase transferBranch,
                             RemoveStudentBranchAssignmentUseCase removeBranch,
                             GetStudentHistoryUseCase getHistory,
                             AddStudentNoteUseCase addNote,
                             ListStudentNotesUseCase listNotes,
                             com.gymsaas.student.domain.StudentRepository students) {
        this.createStudent = createStudent;
        this.updateStudent = updateStudent;
        this.findStudent = findStudent;
        this.listStudents = listStudents;
        this.changeStudentStatus = changeStudentStatus;
        this.addDocument = addDocument;
        this.updateDocument = updateDocument;
        this.removeDocument = removeDocument;
        this.addContact = addContact;
        this.updateContact = updateContact;
        this.removeContact = removeContact;
        this.upsertAddress = upsertAddress;
        this.getAddress = getAddress;
        this.assignBranch = assignBranch;
        this.transferBranch = transferBranch;
        this.removeBranch = removeBranch;
        this.getHistory = getHistory;
        this.addNote = addNote;
        this.listNotes = listNotes;
        this.students = students;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('students:create','student:create')")
    public ResponseEntity<StudentDtos.StudentDetail> create(
            @Valid @RequestBody StudentDtos.CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createStudent.execute(TenantContext.requireTenantId(), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('students:read','student:read')")
    public PageResult<StudentDtos.StudentSummary> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String document,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return listStudents.execute(TenantContext.requireTenantId(),
                new StudentDtos.StudentFilter(search, document, status, branchId, page, size, sort, direction));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyAuthority('students:read','student:read')")
    public StudentDtos.StudentDetail find(@PathVariable UUID studentId,
                                          @RequestParam(defaultValue = "false") boolean reveal,
                                          Authentication authentication) {
        boolean revealSensitive = reveal && hasAuthority(authentication, "students:read_sensitive");
        return findStudent.execute(TenantContext.requireTenantId(), studentId, revealSensitive);
    }

    @PutMapping("/{studentId}")
    @PreAuthorize("hasAnyAuthority('students:update','student:update')")
    public StudentDtos.StudentDetail update(@PathVariable UUID studentId,
                                            @Valid @RequestBody StudentDtos.UpdateStudentRequest request) {
        return updateStudent.execute(TenantContext.requireTenantId(), studentId, request);
    }

    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasAnyAuthority('students:delete','student:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID studentId) {
        students.softDelete(TenantContext.requireTenantId(), studentId, Instant.now());
    }

    @PatchMapping("/{studentId}/status")
    @PreAuthorize("hasAnyAuthority('students:change_status','students:update','student:block')")
    public StudentDtos.StudentDetail changeStatus(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDtos.ChangeStudentStatusRequest request) {
        return changeStudentStatus.execute(TenantContext.requireTenantId(), studentId, request);
    }

    @GetMapping("/{studentId}/history")
    @PreAuthorize("hasAnyAuthority('students:read','student:read')")
    public List<StudentDtos.HistoryView> history(@PathVariable UUID studentId) {
        return getHistory.execute(TenantContext.requireTenantId(), studentId);
    }

    @GetMapping("/{studentId}/documents")
    @PreAuthorize("hasAnyAuthority('students:read','student:read')")
    public List<StudentDtos.DocumentView> listDocuments(@PathVariable UUID studentId,
                                                        @RequestParam(defaultValue = "false") boolean reveal,
                                                        Authentication authentication) {
        boolean revealSensitive = reveal && hasAuthority(authentication, "students:read_sensitive");
        return findStudent.execute(TenantContext.requireTenantId(), studentId, revealSensitive).documents();
    }

    @PostMapping("/{studentId}/documents")
    @PreAuthorize("hasAnyAuthority('students:manage_documents','students:update','student:update')")
    public ResponseEntity<StudentDtos.DocumentView> addDocument(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDtos.DocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addDocument.execute(TenantContext.requireTenantId(), studentId, request));
    }

    @PutMapping("/{studentId}/documents/{documentId}")
    @PreAuthorize("hasAnyAuthority('students:manage_documents','students:update','student:update')")
    public StudentDtos.DocumentView updateDocument(@PathVariable UUID studentId,
                                                   @PathVariable UUID documentId,
                                                   @Valid @RequestBody StudentDtos.DocumentRequest request) {
        return updateDocument.execute(TenantContext.requireTenantId(), studentId, documentId, request);
    }

    @DeleteMapping("/{studentId}/documents/{documentId}")
    @PreAuthorize("hasAnyAuthority('students:manage_documents','students:update','student:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDocument(@PathVariable UUID studentId, @PathVariable UUID documentId) {
        removeDocument.execute(TenantContext.requireTenantId(), studentId, documentId);
    }

    @GetMapping("/{studentId}/contacts")
    @PreAuthorize("hasAnyAuthority('students:read','student:read')")
    public List<StudentDtos.ContactView> listContacts(@PathVariable UUID studentId) {
        return findStudent.execute(TenantContext.requireTenantId(), studentId, false).contacts();
    }

    @PostMapping("/{studentId}/contacts")
    @PreAuthorize("hasAnyAuthority('students:manage_contacts','students:update')")
    public ResponseEntity<StudentDtos.ContactView> addContact(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDtos.ContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addContact.execute(TenantContext.requireTenantId(), studentId, request));
    }

    @PutMapping("/{studentId}/contacts/{contactId}")
    @PreAuthorize("hasAnyAuthority('students:manage_contacts','students:update')")
    public StudentDtos.ContactView updateContact(@PathVariable UUID studentId,
                                                 @PathVariable UUID contactId,
                                                 @Valid @RequestBody StudentDtos.ContactRequest request) {
        return updateContact.execute(TenantContext.requireTenantId(), studentId, contactId, request);
    }

    @DeleteMapping("/{studentId}/contacts/{contactId}")
    @PreAuthorize("hasAnyAuthority('students:manage_contacts','students:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeContact(@PathVariable UUID studentId, @PathVariable UUID contactId) {
        removeContact.execute(TenantContext.requireTenantId(), studentId, contactId);
    }

    @GetMapping("/{studentId}/address")
    @PreAuthorize("hasAnyAuthority('students:read','student:read')")
    public StudentDtos.AddressView getAddress(@PathVariable UUID studentId) {
        return getAddress.execute(TenantContext.requireTenantId(), studentId);
    }

    @PutMapping("/{studentId}/address")
    @PreAuthorize("hasAnyAuthority('students:update','student:update')")
    public StudentDtos.AddressView upsertAddress(@PathVariable UUID studentId,
                                                 @Valid @RequestBody StudentDtos.AddressRequest request) {
        return upsertAddress.execute(TenantContext.requireTenantId(), studentId, request);
    }

    @GetMapping("/{studentId}/branches")
    @PreAuthorize("hasAnyAuthority('students:read','student:read')")
    public List<StudentDtos.BranchAssignmentView> listBranches(@PathVariable UUID studentId) {
        return findStudent.execute(TenantContext.requireTenantId(), studentId, false).branches();
    }

    @PostMapping("/{studentId}/branches")
    @PreAuthorize("hasAnyAuthority('students:manage_branches','students:update')")
    public ResponseEntity<StudentDtos.BranchAssignmentView> assignBranch(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDtos.AssignBranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignBranch.execute(TenantContext.requireTenantId(), studentId, request));
    }

    @PostMapping("/{studentId}/transfer")
    @PreAuthorize("hasAnyAuthority('students:manage_branches','students:update')")
    public StudentDtos.BranchAssignmentView transfer(@PathVariable UUID studentId,
                                                     @Valid @RequestBody StudentDtos.TransferBranchRequest request) {
        return transferBranch.execute(TenantContext.requireTenantId(), studentId, request);
    }

    @DeleteMapping("/{studentId}/branches/{assignmentId}")
    @PreAuthorize("hasAnyAuthority('students:manage_branches','students:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBranch(@PathVariable UUID studentId, @PathVariable UUID assignmentId) {
        removeBranch.execute(TenantContext.requireTenantId(), studentId, assignmentId);
    }

    @GetMapping("/{studentId}/notes")
    @PreAuthorize("hasAnyAuthority('students:read','student:read','students:manage_notes')")
    public List<StudentDtos.NoteView> listNotes(@PathVariable UUID studentId) {
        return listNotes.execute(TenantContext.requireTenantId(), studentId);
    }

    @PostMapping("/{studentId}/notes")
    @PreAuthorize("hasAnyAuthority('students:manage_notes','students:update')")
    public ResponseEntity<StudentDtos.NoteView> addNote(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDtos.NoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addNote.execute(TenantContext.requireTenantId(), studentId, request));
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
