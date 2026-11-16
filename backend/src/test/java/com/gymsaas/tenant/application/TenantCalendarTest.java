package com.gymsaas.tenant.application;

import com.gymsaas.tenant.domain.TenantCalendarRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** "Today" comes from the tenant zone, never from the JVM default zone. */
class TenantCalendarTest {

    // 01:30 UTC on 2027-03-11 = 22:30 on 2027-03-10 in São Paulo (UTC-3) and 10:30 on 2027-03-11 in Tokyo.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2027-03-11T01:30:00Z"), ZoneOffset.UTC);

    @Test
    void todayUsesTenantZone() {
        assertEquals(LocalDate.of(2027, 3, 10), calendar("America/Sao_Paulo").today(UUID.randomUUID()));
        assertEquals(LocalDate.of(2027, 3, 11), calendar("Asia/Tokyo").today(UUID.randomUUID()));
    }

    @Test
    void invalidOrMissingZoneFallsBackToPlatformDefault() {
        assertEquals(LocalDate.of(2027, 3, 10), calendar("Not/AZone").today(UUID.randomUUID()));
        assertEquals(LocalDate.of(2027, 3, 10), calendar(null).today(UUID.randomUUID()));
    }

    static TenantCalendar calendar(String zone) {
        return calendar(zone, CLOCK);
    }

    public static TenantCalendar calendar(String zone, Clock clock) {
        TenantCalendarRepository repository = new TenantCalendarRepository() {
            @Override
            public Optional<String> findTimezone(UUID tenantId) {
                return Optional.ofNullable(zone);
            }

            @Override
            public List<UUID> findActiveTenantIds() {
                return List.of();
            }
        };
        return new TenantCalendar(repository, clock);
    }
}
