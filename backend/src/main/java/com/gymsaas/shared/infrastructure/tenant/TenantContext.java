package com.gymsaas.shared.infrastructure.tenant;

import com.gymsaas.shared.application.ApiException;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Request-scoped tenant/user context. The tenant id always comes from the
 * signed JWT, never from a frontend-supplied header or body.
 */
public final class TenantContext {

    public record Context(UUID tenantId, UUID userId, boolean bypass, boolean platformAdmin) {
    }

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Context context) {
        HOLDER.set(context);
    }

    public static Context get() {
        return HOLDER.get();
    }

    public static UUID tenantId() {
        Context context = HOLDER.get();
        return context == null ? null : context.tenantId();
    }

    public static UUID userId() {
        Context context = HOLDER.get();
        return context == null ? null : context.userId();
    }

    public static boolean isPlatformAdmin() {
        Context context = HOLDER.get();
        return context != null && context.platformAdmin();
    }

    public static UUID requireTenantId() {
        UUID tenantId = tenantId();
        if (tenantId == null) {
            throw ApiException.forbidden("TENANT_REQUIRED", "Selecione uma academia para continuar.");
        }
        return tenantId;
    }

    public static void requirePlatformAdmin() {
        if (!isPlatformAdmin()) {
            throw ApiException.forbidden("PLATFORM_ADMIN_REQUIRED",
                    "Operação restrita ao administrador da plataforma.");
        }
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * Runs an operation with RLS bypass. Used only by authentication flows that
     * must look rows up before a tenant session exists (login, refresh, tenant
     * listing). Every query still filters by the authenticated user id.
     */
    public static <T> T callWithBypass(Supplier<T> action) {
        Context previous = HOLDER.get();
        HOLDER.set(new Context(previous != null ? previous.tenantId() : null,
                previous != null ? previous.userId() : null, true,
                previous != null && previous.platformAdmin()));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(previous);
            }
        }
    }

    public static void runWithBypass(Runnable action) {
        callWithBypass(() -> {
            action.run();
            return null;
        });
    }
}
