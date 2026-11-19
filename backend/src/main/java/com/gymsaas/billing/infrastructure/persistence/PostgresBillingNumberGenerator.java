package com.gymsaas.billing.infrastructure.persistence;

import com.gymsaas.billing.domain.BillingNumberGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Atomic per-tenant/kind/year counter (INSERT ... ON CONFLICT ... RETURNING). */
@Component
public class PostgresBillingNumberGenerator implements BillingNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    public PostgresBillingNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String nextCharge(UUID tenantId, int year) {
        return BillingNumberGenerator.format('C', year, next(tenantId, "CHARGE", year));
    }

    @Override
    public String nextPayment(UUID tenantId, int year) {
        return BillingNumberGenerator.format('R', year, next(tenantId, "PAYMENT", year));
    }

    private long next(UUID tenantId, String kind, int year) {
        Long value = jdbcTemplate.queryForObject("""
                insert into billing_number_counters (tenant_id, kind, year, last_value)
                values (?, ?, ?, 1)
                on conflict (tenant_id, kind, year)
                do update set last_value = billing_number_counters.last_value + 1
                returning last_value
                """, Long.class, tenantId, kind, year);
        return value == null ? 1 : value;
    }
}
