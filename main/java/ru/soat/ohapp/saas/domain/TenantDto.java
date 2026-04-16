package ru.soat.ohapp.saas.domain;

import java.time.Instant;

public record TenantDto(long id, String name, String slug, Instant createdAt) {
    // Совместимость с контроллерами, которые ожидают "get*"
    public long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public Instant getCreatedAt() { return createdAt; }
}
