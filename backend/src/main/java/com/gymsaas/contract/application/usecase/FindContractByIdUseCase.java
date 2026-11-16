package com.gymsaas.contract.application.usecase;

import com.gymsaas.contract.application.ContractAssembler;
import com.gymsaas.contract.application.ContractDtos;
import com.gymsaas.contract.application.ContractGuard;
import com.gymsaas.contract.domain.ContractAcceptanceRepository;
import com.gymsaas.contract.domain.MembershipContract;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FindContractByIdUseCase {

    private final ContractGuard guard;
    private final ContractAcceptanceRepository acceptances;

    public FindContractByIdUseCase(ContractGuard guard, ContractAcceptanceRepository acceptances) {
        this.guard = guard;
        this.acceptances = acceptances;
    }

    @Transactional(readOnly = true)
    public ContractDtos.ContractView execute(UUID tenantId, UUID contractId) {
        MembershipContract contract = guard.requireContract(tenantId, contractId);
        return ContractAssembler.view(contract, acceptances.findByContract(tenantId, contractId).orElse(null));
    }
}
