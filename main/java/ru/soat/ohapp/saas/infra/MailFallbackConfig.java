package ru.soat.ohapp.saas.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fallback-конфигурация почты: регистрирует NoopMailService, если в контексте нет другого MailService.
 * Удобно для тестов и локальной разработки без SMTP.
 */
@Configuration
public class MailFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(MailService.class)
    public MailService mailService() {
        return new NoopMailService();
    }

    @Slf4j
    static class NoopMailService implements MailService {
        @Override
        public void sendPlain(String to, String subject, String body) {
            log.info("[NOOP-MAIL] to={}, subject={}, body={}", to, subject, body);
        }

        // Если в интерфейсе есть другие методы — раскомментируй/добавь:
        // @Override
        // public void sendHtml(String to, String subject, String html) {
        //     log.info("[NOOP-MAIL][HTML] to={}, subject={}, html-len={}", to, subject, html != null ? html.length() : 0);
        // }
        //
        // @Override
        // public void sendWithAttachments(String to, String subject, String body, List<Attachment> atts) {
        //     log.info("[NOOP-MAIL][ATTACH] to={}, subject={}, attachments={}", to, subject, atts != null ? atts.size() : 0);
        // }
    }
}
