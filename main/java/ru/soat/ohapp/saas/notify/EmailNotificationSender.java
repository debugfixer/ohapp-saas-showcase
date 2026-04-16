// src/main/java/ru/soat/ohapp/saas/notify/EmailNotificationSender.java
package ru.soat.ohapp.saas.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.soat.ohapp.saas.domain.Reminder;
import ru.soat.ohapp.saas.infra.MailService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final MailService mailService;

    /**
     * Флаг для отключения реальной отправки (удобно на dev/test):
     * ohapp.notify.email.enabled=false
     */
    @Value("${ohapp.notify.email.enabled:true}")
    private boolean emailEnabled;

    @Override
    public boolean supports(Reminder.Channel channel) {
        return channel == Reminder.Channel.EMAIL;
    }

    @Override
    public void send(Reminder r) {
        final String to = r.getRecipient();
        if (to == null || to.isBlank() || !to.contains("@") || to.startsWith("@") || to.endsWith("@")) {
            throw new IllegalArgumentException("Invalid email recipient: " + to);
        }

        final String subject = (r.getSubject() == null || r.getSubject().isBlank())
                ? "Reminder"
                : r.getSubject();
        final String body = r.getMessage() == null ? "" : r.getMessage();

        if (!emailEnabled) {
            log.info("[DEV] Email disabled. Would send to='{}' subject='{}' body='{}'", to, subject, body);
            return;
        }

        mailService.sendPlain(to, subject, body);
        log.debug("Email sent to='{}' subject='{}'", to, subject);
    }
}
