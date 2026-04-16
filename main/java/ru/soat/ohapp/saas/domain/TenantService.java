package ru.soat.ohapp.saas.domain;

import java.util.NoSuchElementException;
import java.util.Optional;

public interface TenantService {
    TenantDto create(String name, String slug);
    Optional<TenantDto> findBySlug(String slug);

    // нужно контроллерам
    default TenantDto getOrCreateBySlug(String slug, String name) {
        return findBySlug(slug).orElseGet(() -> create(name, slug));
    }

    default TenantDto getBySlugOrThrow(String slug) {
        return findBySlug(slug).orElseThrow(() ->
                new NoSuchElementException("Tenant not found by slug: " + slug));
    }

    /** Привязать пользователя к тенанту с ролью, если связи ещё нет. */
    void bindUserRoleIfAbsent(long userId, long tenantId, String role);
}
