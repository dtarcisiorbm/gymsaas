package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentAddress;
import com.gymsaas.student.domain.StudentAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class UpsertStudentAddressUseCase {

    private final StudentGuard guard;
    private final StudentAddressRepository addresses;
    private final AuditService audit;

    public UpsertStudentAddressUseCase(StudentGuard guard, StudentAddressRepository addresses,
                                       AuditService audit) {
        this.guard = guard;
        this.addresses = addresses;
        this.audit = audit;
    }

    @Transactional
    public StudentDtos.AddressView execute(UUID tenantId, UUID studentId,
                                           StudentDtos.AddressRequest request) {
        guard.requireStudent(tenantId, studentId);
        Instant now = Instant.now();
        StudentAddress current = addresses.findByStudent(tenantId, studentId).orElse(null);
        StudentAddress saved = addresses.save(new StudentAddress(
                current == null ? UuidV7.generate() : current.id(), tenantId, studentId,
                BrDocuments.zipCode(request.zipCode()), StudentGuard.trimToNull(request.street()),
                StudentGuard.trimToNull(request.number()), StudentGuard.trimToNull(request.complement()),
                StudentGuard.trimToNull(request.neighborhood()), StudentGuard.trimToNull(request.city()),
                StudentGuard.upperToNull(request.state()),
                current == null ? now : current.createdAt(), now));
        audit.recordForTenant(tenantId, "STUDENT_ADDRESS_UPDATED", "student", studentId, "MEDIUM",
                null, Map.of("city", String.valueOf(saved.city())), Map.of());
        return new StudentDtos.AddressView(saved.id(), saved.zipCode(), saved.street(), saved.number(),
                saved.complement(), saved.neighborhood(), saved.city(), saved.state());
    }
}
