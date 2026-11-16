package com.gymsaas.student.application.usecase;

import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentNote;
import com.gymsaas.student.domain.StudentNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ListStudentNotesUseCase {

    private final StudentGuard guard;
    private final StudentNoteRepository notes;

    public ListStudentNotesUseCase(StudentGuard guard, StudentNoteRepository notes) {
        this.guard = guard;
        this.notes = notes;
    }

    @Transactional(readOnly = true)
    public List<StudentDtos.NoteView> execute(UUID tenantId, UUID studentId) {
        guard.requireStudent(tenantId, studentId);
        return notes.findByStudent(tenantId, studentId).stream()
                .map(this::toView)
                .toList();
    }

    private StudentDtos.NoteView toView(StudentNote note) {
        return new StudentDtos.NoteView(note.id(), note.content(), note.createdBy(),
                note.createdAt(), note.updatedAt());
    }
}
