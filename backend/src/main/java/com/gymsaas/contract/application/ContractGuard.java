package com.gymsaas.contract.application;

import com.gymsaas.contract.domain.ContractRepository;
import com.gymsaas.contract.domain.MembershipContract;
import com.gymsaas.shared.application.ApiException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ContractGuard {

    private final ContractRepository contracts;

    public ContractGuard(ContractRepository contracts) {
        this.contracts = contracts;
    }

    public MembershipContract requireContract(UUID tenantId, UUID contractId) {
        return contracts.findById(tenantId, contractId)
                .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "Contrato não encontrado."));
    }
}
