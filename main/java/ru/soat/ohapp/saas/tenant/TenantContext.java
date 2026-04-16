package ru.soat.ohapp.saas.tenant;

/** @deprecated Используй ru.soat.ohapp.saas.tenancy.TenantContext */
@Deprecated
public final class TenantContext {
    private TenantContext() {}
    public static void set(String slug) { ru.soat.ohapp.saas.tenancy.TenantContext.set(slug); }
    public static String get() { return ru.soat.ohapp.saas.tenancy.TenantContext.get(); }
    public static void clear() { ru.soat.ohapp.saas.tenancy.TenantContext.clear(); }
}
