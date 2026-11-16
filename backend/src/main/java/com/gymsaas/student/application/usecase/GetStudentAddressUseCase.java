package com.gymsaas.student.application.usecase;

import com.gymsaas.shared.application.ApiException;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentAddress;
import com.gymsaas.student.domain.StudentAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetStudentAddressUseCase {

    private final StudentGuard guard;
    private final StudentAddressRepository addresses;

    public GetStudentAddressUseCase(StudentGuard guard, StudentAddressRepository addresses) {
        this.guard = guard;
        this.addresses = addresses;
    }

    @Transactional(readOnly = true)
    public StudentDtos.AddressView execute(UUID tenantId, UUID studentId) {
        guard.requireStudent(tenantId, studentId);
        StudentAddress address = addresses.findByStudent(tenantId, studentId)
                .orElseThrow(() -> ApiException.notFound("ADDRESS_NOT_FOUND", "Endereço não encontrado."));
        return new StudentDtos.AddressView(address.id(), address.zipCode(), address.street(),
                address.number(), address.complement(), address.neighborhood(), address.city(),
                address.state());
    }
}
