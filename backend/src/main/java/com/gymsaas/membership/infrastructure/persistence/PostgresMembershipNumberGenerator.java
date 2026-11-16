package com.gymsaas.membership.infrastructure.persistence;

import com.gymsaas.membership.domain.MembershipNumberGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Atomic per-tenant/per-year counter (UPDATE ... RETURNING), same pattern as student registration. */
@Component
public class PostgresMembershipNumberGenerator implements MembershipNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    public PostgresMembershipNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String next(UUID tenantId, int year) {
        Long value = jdbcTemplate.queryForObject("""
                insert into membership_number_counters (tenant_id, year, last_value)
                values (?, ?, 1)
                on conflict (tenant_id, year)
                do update set last_value = membership_number_counters.last_value + 1
                returning last_value
                """, Long.class, tenantId, year);
        return MembershipNumberGenerator.format(year, value == null ? 1 : value);
    }
}
