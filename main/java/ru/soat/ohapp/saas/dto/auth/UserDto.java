package ru.soat.ohapp.saas.dto.auth;

public record UserDto(
        String id,      // ✅ ID пользователя (UUID или long as String)
        String email,
        String name,
        String role     // ✅ Роль пользователя
) {

    // ✅ Геттеры для совместимости с контроллерами
    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role != null ? role : "USER";
    }
}
