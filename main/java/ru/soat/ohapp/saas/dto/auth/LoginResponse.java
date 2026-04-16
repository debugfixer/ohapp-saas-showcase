package ru.soat.ohapp.saas.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ при успешном логине.
 * Возвращается фронтенду и сохраняется в localStorage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;     // JWT access token
    private UserDto user;     // краткие данные пользователя
    private TenantDto tenant; // данные текущего арендатора
    private String role;      // роль пользователя
}
