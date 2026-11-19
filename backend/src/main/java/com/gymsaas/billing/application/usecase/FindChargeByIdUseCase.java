package com.gymsaas.billing.application.usecase;

import com.gymsaas.billing.application.BillingDtos;
import com.gymsaas.billing.application.BillingLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FindChargeByIdUseCase {

    private final BillingLoader loader;

    public FindChargeByIdUseCase(BillingLoader loader) {
        this.loader = loader;
    }

    @Transactional(readOnly = true)
    public BillingDtos.ChargeDetail execute(UUID tenantId, UUID chargeId) {
        return loader.detail(tenantId, loader.requireCharge(tenantId, chargeId));
    }
}
