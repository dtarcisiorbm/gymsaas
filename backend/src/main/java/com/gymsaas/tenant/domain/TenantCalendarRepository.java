package com.gymsaas.tenant.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only access to tenant calendar settings. Business dates ("today") are
 * always derived from the tenant time zone, never from the JVM default zone.
 */
public interface TenantCalendarRepository {

    Optional<String> findTimezone(UUID tenantId);

    /** Ids of ACTIVE tenants; used by system jobs that run per tenant. */
    List<UUID> findActiveTenantIds();
}
