// src/main/java/ru/soat/ohapp/saas/infra/MailService.java
package ru.soat.ohapp.saas.infra;

public interface MailService {
    void sendPlain(String to, String subject, String body);
}
