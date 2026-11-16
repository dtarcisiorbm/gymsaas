package com.gymsaas.contract.application.usecase;

import com.gymsaas.contract.application.ContractDtos;
import com.gymsaas.contract.application.ContractGuard;
import com.gymsaas.contract.domain.ContractDocumentStorage;
import com.gymsaas.contract.domain.MembershipContract;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.Sha256;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Streams the stored PDF after verifying its SHA-256 against the database record. */
@Service
public class DownloadContractDocumentUseCase {

    private final ContractGuard guard;
    private final MembershipGuard membershipGuard;
    private final ContractDocumentStorage storage;

    public DownloadContractDocumentUseCase(ContractGuard guard, MembershipGuard membershipGuard,
                                           ContractDocumentStorage storage) {
        this.guard = guard;
        this.membershipGuard = membershipGuard;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public ContractDtos.DocumentFile execute(UUID tenantId, UUID contractId) {
        MembershipContract contract = guard.requireContract(tenantId, contractId);
        Membership membership = membershipGuard.requireMembership(tenantId, contract.membershipId());
        byte[] content = storage.load(contract.storageKey());
        if (!Sha256.hex(content).equals(contract.documentSha256())) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DOCUMENT_INTEGRITY_ERROR",
                    "O arquivo armazenado não confere com o hash registrado.");
        }
        return new ContractDtos.DocumentFile(content,
                "contrato-" + membership.registrationNumber() + "-v" + contract.contractVersion() + ".pdf",
                contract.documentContentType());
    }
}
