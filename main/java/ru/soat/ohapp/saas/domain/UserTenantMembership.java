package ru.soat.ohapp.saas.domain;

import lombok.Data;

import java.util.UUID;

/**
 * Плоский DTO для ответов/запросов.
 * Не содержит JPA-аннотаций и ссылок на сущности, чтобы не конфликтовать с model.*.
 */
@Data
public class UserTenantMembership {
    private UUID id;
    private UUID userId;
    private UUID tenantId;
    private String role;
    private String departmentScope;
}
