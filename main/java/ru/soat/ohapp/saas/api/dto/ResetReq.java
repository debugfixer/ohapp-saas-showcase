package ru.soat.ohapp.saas.api.dto;

import lombok.Data;

/** Запрос на применение нового пароля по токену. */
@Data
public class ResetReq {
    private String email;
    private String token;        // «сырой» токен из письма
    private String newPassword;
}
