package com.gymsaas.billing.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Optional<Payment> findById(UUID tenantId, UUID paymentId);

    List<Payment> findByCharge(UUID tenantId, UUID chargeId);

    Payment insert(Payment payment);

    Payment markRefunded(Payment payment);
}
