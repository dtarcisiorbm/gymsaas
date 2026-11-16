package com.gymsaas.tenant.infrastructure.persistence;

import com.gymsaas.tenant.domain.TenantCalendarRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TenantCalendarJdbcRepository implements TenantCalendarRepository {

    private final JdbcTemplate jdbcTemplate;

    public TenantCalendarJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> findTimezone(UUID tenantId) {
        if (tenantId == null) {
            return Optional.empty();
        }
        return jdbcTemplate.queryForList("select timezone from tenants where id = ?", String.class, tenantId)
                .stream().findFirst();
    }

    @Override
    public List<UUID> findActiveTenantIds() {
        return jdbcTemplate.queryForList("select id from tenants where status = 'ACTIVE' order by id", UUID.class);
    }
}
