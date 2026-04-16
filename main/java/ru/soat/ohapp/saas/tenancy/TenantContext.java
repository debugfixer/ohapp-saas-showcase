package ru.soat.ohapp.saas.tenancy;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TENANT_SLUG = new ThreadLocal<>();

    private TenantContext() {
    }

    // ✅ UUID методы
    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT_ID.get();
    }

    // ✅ String slug методы (для совместимости со старым кодом)
    public static void set(String slug) {
        CURRENT_TENANT_SLUG.set(slug);
    }

    public static String get() {
        return CURRENT_TENANT_SLUG.get();
    }

    public static void clear() {
        CURRENT_TENANT_ID.remove();
        CURRENT_TENANT_SLUG.remove();
    }
}
