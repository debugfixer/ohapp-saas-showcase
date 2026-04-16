// src/main/java/ru/soat/ohapp/saas/api/dto/CreateReminderReq.java
package ru.soat.ohapp.saas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateReminderReq {
    @NotBlank
    private String tenantId;     // UUID строкой
    @NotBlank
    private String userId;       // UUID строкой
    @NotBlank
    private String channel;      // EMAIL|SMS|WHATSAPP

    @NotBlank
    private String recipient;    // email/phone

    private String subject;      // для EMAIL
    @NotBlank
    private String message;

    // Либо одноразовая дата/время (ISO-8601), либо cron
    private String sendAt;       // Instant ISO-8601
    private String cron;         // "0 0 9 * * ?"
}
