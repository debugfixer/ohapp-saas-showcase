// src/main/java/ru/soat/ohapp/saas/api/ReminderController.java
package ru.soat.ohapp.saas.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.domain.Reminder;
import ru.soat.ohapp.saas.service.ReminderService;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    // Простая проверка формата телефона E.164 (начальная валидация)
    private static final String E164 = "^\\+?[1-9]\\d{7,14}$";

    private final ReminderService service;

    public ReminderController(ReminderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateReminderReq req,
                                    @RequestHeader(value = "X-Tenant", required = false) String tenantHeader) {
        // 1) Парсим канал без падения
        final Reminder.Channel ch;
        try {
            ch = Reminder.Channel.valueOf(req.getChannel().trim().toUpperCase());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("channel must be one of: EMAIL | SMS | WHATSAPP");
        }

        // 2) Проверяем получателя
        final String recipient = req.getRecipient();
        if (recipient == null || recipient.isBlank()) {
            return ResponseEntity.badRequest().body("recipient is required");
        }
        if (ch == Reminder.Channel.EMAIL) {
            // очень простая проверка email
            if (!recipient.contains("@") || recipient.startsWith("@") || recipient.endsWith("@")) {
                return ResponseEntity.badRequest().body("recipient must be a valid email for EMAIL channel");
            }
        } else {
            // для SMS/WHATSAPP ожидаем E.164
            if (!recipient.matches(E164)) {
                return ResponseEntity.badRequest().body("recipient must be phone in E.164 for SMS/WHATSAPP");
            }
        }

        // 3) Взаимоисключающие cron / sendAt
        if ((req.getCron() == null || req.getCron().isBlank()) && req.getSendAt() == null) {
            return ResponseEntity.badRequest().body("either cron or sendAt must be provided");
        }
        if (req.getCron() != null && req.getSendAt() != null) {
            return ResponseEntity.badRequest().body("use either cron or sendAt, not both");
        }

        // 4) Собираем сущность
        Reminder r = Reminder.builder()
                .tenantId(parseUUID(tenantHeader))   // если в X-Tenant передаёте UUID арендатора
                .userId(parseUUID(req.getUserId()))
                .channel(ch)
                .recipient(recipient)
                .subject(req.getSubject())
                .message(req.getMessage())
                .sendAt(req.getSendAt())
                .cronExpr(req.getCron())
                .build();

        Reminder saved = service.create(r);
        return ResponseEntity.ok(new CreateReminderRes(saved.getId()));
    }

    private UUID parseUUID(String s) {
        try { return (s == null || s.isBlank()) ? null : UUID.fromString(s.trim()); }
        catch (Exception e) { return null; }
    }

    /* DTOs */
    @Data
    public static class CreateReminderReq {
        @NotBlank
        private String channel;   // EMAIL | SMS | WHATSAPP

        @NotBlank
        private String recipient; // email (для EMAIL) или номер (E.164) для SMS/WHATSAPP

        private String subject;   // для EMAIL (опционально)
        @NotBlank
        private String message;

        private Instant sendAt;   // либо это
        private String cron;      // либо CRON-строка

        private String userId;    // опц., для аудита
    }

    public record CreateReminderRes(UUID id) {}
}
