// src/main/java/ru/soat/ohapp/saas/infra/DevLogMailService.java
package ru.soat.ohapp.saas.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("dev")
public class DevLogMailService implements MailService {
    @Override
    public void sendPlain(String to, String subject, String body) {
        log.info("[DEV MAIL] to={} subj={} body={}", to, subject, body);
    }
}
