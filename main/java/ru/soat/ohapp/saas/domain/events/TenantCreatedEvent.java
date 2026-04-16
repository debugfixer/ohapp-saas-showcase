package ru.soat.ohapp.saas.domain.events;

import java.util.UUID;

/** Публикуется после успешного создания арендатора. */
public record TenantCreatedEvent(UUID tenantId) {}
