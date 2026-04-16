package ru.soat.ohapp.saas.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;

@Configuration
@Profile("test") // активируется только в профиле test
public class NoopMailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        // Полностью «пустой» sender, ничего никуда не шлёт
        return new JavaMailSender() {
            @Override public MimeMessage createMimeMessage() { return new MimeMessage((Session) null); }
            @Override public MimeMessage createMimeMessage(InputStream contentStream) {
                try { return new MimeMessage(null, contentStream); }
                catch (MessagingException e) { throw new RuntimeException(e); }
            }
            @Override public void send(MimeMessage mimeMessage) {}
            @Override public void send(MimeMessage... mimeMessages) {}
            @Override public void send(MimeMessagePreparator mimeMessagePreparator) {}
            @Override public void send(MimeMessagePreparator... mimeMessagePreparators) {}
            @Override public void send(SimpleMailMessage simpleMessage) {}
            @Override public void send(SimpleMailMessage... simpleMessages) {}
        };
    }
}
