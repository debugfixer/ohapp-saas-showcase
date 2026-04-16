package ru.soat.ohapp.saas.api.dto;

import lombok.Data;

/** Запрос на генерацию письма со ссылкой для сброса пароля. */
@Data
public class ForgotReq {
    private String email;
    /** URL фронтенда, куда ведёт ссылка, например: https://app.example.com */
    private String frontendUrl;
    /** Необязательный код арендатора (для мульти-тенанта) */
    private String tenant;
}
