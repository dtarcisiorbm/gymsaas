package com.gymsaas.contract.application;

import com.gymsaas.contract.domain.ContractAcceptance;
import com.gymsaas.contract.domain.MembershipContract;

public final class ContractAssembler {

    private ContractAssembler() {
    }

    public static ContractDtos.AcceptanceView acceptance(ContractAcceptance acceptance) {
        if (acceptance == null) {
            return null;
        }
        return new ContractDtos.AcceptanceView(acceptance.id(), acceptance.acceptedAt(), acceptance.channel().name(),
                acceptance.acceptorName(), acceptance.acceptorDocumentMasked(), acceptance.contentSha256(),
                acceptance.declaration(), acceptance.ipAddress(), acceptance.userAgent(), acceptance.registeredBy());
    }

    public static ContractDtos.ContractSummary summary(MembershipContract contract, ContractAcceptance acceptance) {
        return new ContractDtos.ContractSummary(contract.id(), contract.membershipId(), contract.contractVersion(),
                contract.templateVersion(), contract.status().name(), contract.contentSha256(),
                contract.documentSha256(), contract.documentSizeBytes(), contract.documentContentType(),
                contract.generatedBy(), contract.generatedAt(), contract.statusChangedAt(), acceptance(acceptance));
    }

    public static ContractDtos.ContractView view(MembershipContract contract, ContractAcceptance acceptance) {
        return new ContractDtos.ContractView(contract.id(), contract.membershipId(), contract.contractVersion(),
                contract.templateVersion(), contract.status().name(), contract.content(), contract.contentSha256(),
                contract.documentSha256(), contract.documentSizeBytes(), contract.documentContentType(),
                contract.generatedBy(), contract.generatedAt(), contract.statusChangedAt(), acceptance(acceptance),
                ContractContentFactory.LEGAL_NOTICE);
    }
}
