// src/main/java/ru/soat/ohapp/saas/infra/SmtpMailService.java
package ru.soat.ohapp.saas.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class SmtpMailService implements MailService {
    private final JavaMailSender sender;

    @Override
    public void sendPlain(String to, String subject, String body) {
        var msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        sender.send(msg);
    }
}
