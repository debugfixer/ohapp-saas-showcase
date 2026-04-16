package ru.soat.ohapp.saas.domain;

import java.util.Optional;

public interface UserService {

    // 🔄 Аутентификация без tenant (базовая проверка)
    Optional<ru.soat.ohapp.saas.dto.auth.UserDto> authenticate(String email, String password);

    // 🔄 Аутентификация с tenant (проверка membership)
    Optional<ru.soat.ohapp.saas.dto.auth.UserDto> authenticate(String email, String password, String tenantSlug);

    // Остальные методы (доменные операции)
    UserDto register(String email, String password, String name);

    void addUserToTenant(long userId, long tenantId, String role);

    boolean isUserBoundToTenant(long userId, long tenantId);

    Optional<UserDto> getByEmail(String email);

    Optional<UserDto> findUserInTenant(String email, String tenantSlug);

    // Для совместимости с Provision/TenantProvision
    UserDto getOrCreate(String email, String password, String name);

    UserDto getByEmailOrThrow(String email);
}
