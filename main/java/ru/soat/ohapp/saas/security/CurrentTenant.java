package ru.soat.ohapp.saas.security;

import ru.soat.ohapp.saas.tenancy.TenantContext;

import java.util.UUID;

public final class CurrentTenant {

    private CurrentTenant() {
    }

    /**
     * ✅ ЧИТАЕМ tenantId из TenantContext (ThreadLocal)
     */
    public static UUID require() {
        UUID tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new IllegalStateException("TenantId is not available in request context");
        }

        return tenantId;
    }
}
