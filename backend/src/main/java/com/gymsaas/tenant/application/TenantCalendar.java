package com.gymsaas.tenant.application;

import com.gymsaas.tenant.domain.TenantCalendarRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Resolves business dates in the tenant's configured time zone. The JVM
 * default zone is never used; an invalid/missing configuration falls back to
 * the platform default documented in V2 (America/Sao_Paulo).
 */
@Component
public class TenantCalendar {

    public static final ZoneId FALLBACK_ZONE = ZoneId.of("America/Sao_Paulo");

    private final TenantCalendarRepository repository;
    private final Clock clock;

    public TenantCalendar(TenantCalendarRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public ZoneId zone(UUID tenantId) {
        return repository.findTimezone(tenantId).map(TenantCalendar::parseZone).orElse(FALLBACK_ZONE);
    }

    public LocalDate today(UUID tenantId) {
        return LocalDate.ofInstant(clock.instant(), zone(tenantId));
    }

    public Instant now() {
        return clock.instant();
    }

    private static ZoneId parseZone(String value) {
        try {
            return ZoneId.of(value);
        } catch (DateTimeException exception) {
            return FALLBACK_ZONE;
        }
    }
}
