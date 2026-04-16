package ru.soat.ohapp.saas.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    /** Генератор ID (in-memory). */
    private static final AtomicLong TENANT_ID_SEQ = new AtomicLong(1);

    /** Тенанты: id -> dto. */
    static final Map<Long, TenantDto> TENANTS_BY_ID = new ConcurrentHashMap<>();

    /** Индекс по slug: slug -> id. */
    static final Map<String, Long> TENANT_ID_BY_SLUG = new ConcurrentHashMap<>();

    /** Связи user↔tenant: userId -> (tenantId -> role). */
    static final Map<Long, Map<Long, String>> USER_TENANT_BINDINGS = new ConcurrentHashMap<>();

    @Override
    public TenantDto create(String name, String slug) {
        Long existing = TENANT_ID_BY_SLUG.get(slug);
        if (existing != null) {
            return TENANTS_BY_ID.get(existing);
        }
        long id = TENANT_ID_SEQ.getAndIncrement();
        TenantDto dto = new TenantDto(id, name, slug, Instant.now());
        TENANTS_BY_ID.put(id, dto);
        TENANT_ID_BY_SLUG.put(slug, id);
        return dto;
    }

    @Override
    public Optional<TenantDto> findBySlug(String slug) {
        Long id = TENANT_ID_BY_SLUG.get(slug);
        return id == null ? Optional.empty() : Optional.ofNullable(TENANTS_BY_ID.get(id));
    }

    @Override
    public void bindUserRoleIfAbsent(long userId, long tenantId, String role) {
        if (!TENANTS_BY_ID.containsKey(tenantId)) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        USER_TENANT_BINDINGS
                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .putIfAbsent(tenantId, role);
    }
}
