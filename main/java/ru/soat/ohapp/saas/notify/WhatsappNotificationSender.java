// src/main/java/ru/soat/ohapp/saas/notify/WhatsappNotificationSender.java
package ru.soat.ohapp.saas.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.soat.ohapp.saas.domain.Reminder;

@Slf4j
@Component
public class WhatsappNotificationSender implements NotificationSender {

    /** Включение реальной отправки WhatsApp (по умолчанию выключено для dev/test). */
    @Value("${ohapp.notify.whatsapp.enabled:false}")
    private boolean enabled;

    // Простой паттерн E.164: + и 8–15 цифр, без пробелов/скобок/дефисов
    private static final String E164 = "^\\+?[1-9]\\d{7,14}$";

    @Override
    public boolean supports(Reminder.Channel channel) {
        return channel == Reminder.Channel.WHATSAPP;
    }

    @Override
    public void send(Reminder r) {
        final String to = r.getRecipient();
        final String text = r.getMessage() == null ? "" : r.getMessage();

        if (to == null || to.isBlank() || !to.matches(E164)) {
            throw new IllegalArgumentException("Invalid WhatsApp recipient (must be E.164): " + to);
        }

        if (!enabled) {
            log.info("[DEV] WhatsApp disabled. Would send to='{}' text='{}'", maskPhone(to), text);
            return;
        }

        // TODO: интеграция с провайдером (Twilio WhatsApp / Meta Cloud API):
        // whatsappClient.send(new WhatsappMessage(to, text));
        log.info("[WHATSAPP] Sent to='{}' text='{}'", maskPhone(to), text);
    }

    /** Маскируем номер в логах: оставляем только последние 4 цифры. */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        int keep = 4;
        int maskLen = phone.length() - keep;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maskLen; i++) sb.append('*');
        sb.append(phone.substring(maskLen));
        return sb.toString();
    }
}
