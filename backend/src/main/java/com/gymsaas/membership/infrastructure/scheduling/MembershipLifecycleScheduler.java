package com.gymsaas.membership.infrastructure.scheduling;

import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.usecase.ProcessMembershipLifecycleUseCase;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.domain.TenantCalendarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Runs the date-driven membership transitions once per tenant. Each tenant is
 * processed inside its own tenant context (RLS scoped, no bypass) and its own
 * transaction; a failure in one tenant does not block the others and is
 * retried on the next run (the use case is idempotent).
 */
@Component
@ConditionalOnProperty(name = "app.memberships.lifecycle.enabled", havingValue = "true", matchIfMissing = true)
public class MembershipLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(MembershipLifecycleScheduler.class);

    private final ProcessMembershipLifecycleUseCase lifecycle;
    private final TenantCalendarRepository tenants;

    public MembershipLifecycleScheduler(ProcessMembershipLifecycleUseCase lifecycle, TenantCalendarRepository tenants) {
        this.lifecycle = lifecycle;
        this.tenants = tenants;
    }

    @Scheduled(cron = "${app.memberships.lifecycle.cron:0 5 * * * *}")
    public void run() {
        List<UUID> tenantIds = TenantContext.callWithBypass(tenants::findActiveTenantIds);
        for (UUID tenantId : tenantIds) {
            TenantContext.set(new TenantContext.Context(tenantId, null, false, false));
            try {
                MembershipDtos.LifecycleResult result = lifecycle.execute(tenantId);
                if (result.activated() + result.unfrozen() + result.expired() > 0) {
                    log.info("Membership lifecycle tenant={} activated={} unfrozen={} expired={}", tenantId,
                            result.activated(), result.unfrozen(), result.expired());
                }
            } catch (RuntimeException exception) {
                log.warn("Membership lifecycle failed for tenant={}: {}", tenantId, exception.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
