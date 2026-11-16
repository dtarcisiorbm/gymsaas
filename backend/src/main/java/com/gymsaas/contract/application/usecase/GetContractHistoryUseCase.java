package com.gymsaas.contract.application.usecase;

import com.gymsaas.contract.application.ContractAssembler;
import com.gymsaas.contract.application.ContractDtos;
import com.gymsaas.contract.domain.ContractAcceptance;
import com.gymsaas.contract.domain.ContractAcceptanceRepository;
import com.gymsaas.contract.domain.ContractRepository;
import com.gymsaas.contract.domain.MembershipContract;
import com.gymsaas.membership.application.MembershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** All contract versions of a membership (newest first) with acceptance evidence. */
@Service
public class GetContractHistoryUseCase {

    private final MembershipGuard membershipGuard;
    private final ContractRepository contracts;
    private final ContractAcceptanceRepository acceptances;

    public GetContractHistoryUseCase(MembershipGuard membershipGuard, ContractRepository contracts,
                                     ContractAcceptanceRepository acceptances) {
        this.membershipGuard = membershipGuard;
        this.contracts = contracts;
        this.acceptances = acceptances;
    }

    @Transactional(readOnly = true)
    public List<ContractDtos.ContractSummary> execute(UUID tenantId, UUID membershipId) {
        membershipGuard.requireMembership(tenantId, membershipId);
        List<MembershipContract> versions = contracts.findByMembership(tenantId, membershipId);
        Map<UUID, ContractAcceptance> acceptanceByContract = acceptances.findByContracts(tenantId,
                        versions.stream().map(MembershipContract::id).toList()).stream()
                .collect(Collectors.toMap(ContractAcceptance::contractId, Function.identity()));
        return versions.stream()
                .map(contract -> ContractAssembler.summary(contract, acceptanceByContract.get(contract.id())))
                .toList();
    }
}
