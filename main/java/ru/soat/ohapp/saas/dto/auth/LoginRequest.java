package ru.soat.ohapp.saas.dto.auth;

import lombok.Data;

/** Запрос на вход в систему. */
@Data
public class LoginRequest {
    private String email;    // или username — контроллер сам решит, что использовать
    private String password;
    private String tenant;   // опционально; если не передан — выбрать на бэке
}
