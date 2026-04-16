// src/main/java/ru/soat/ohapp/saas/notify/NotificationSender.java
package ru.soat.ohapp.saas.notify;

import ru.soat.ohapp.saas.domain.Reminder;

public interface NotificationSender {
    boolean supports(Reminder.Channel ch);
    void send(Reminder r) throws Exception;
}
