package com.gymsaas.student.infrastructure.persistence;

import com.gymsaas.student.domain.StudentRegistrationGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Component
public class PostgresStudentRegistrationGenerator implements StudentRegistrationGenerator {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final JdbcTemplate jdbcTemplate;

    public PostgresStudentRegistrationGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String next(UUID tenantId) {
        int year = LocalDate.now(ZONE).getYear();
        Long value = jdbcTemplate.queryForObject("""
                insert into student_registration_counters (tenant_id, year, last_value)
                values (?, ?, 1)
                on conflict (tenant_id, year)
                do update set last_value = student_registration_counters.last_value + 1
                returning last_value
                """, Long.class, tenantId, year);
        return StudentRegistrationGenerator.format(year, value == null ? 1 : value);
    }
}
