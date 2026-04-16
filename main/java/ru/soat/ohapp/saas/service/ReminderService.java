// src/main/java/ru/soat/ohapp/saas/service/ReminderService.java
package ru.soat.ohapp.saas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.domain.Reminder;
import ru.soat.ohapp.saas.repo.ReminderRepository;

import java.time.Instant;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final ReminderRepository repo;

    /**
     * Создание черновика напоминания.
     * @PrePersist в сущности Reminder сам проставит:
     *  - id (если null),
     *  - createdAt / updatedAt,
     *  - status=PENDING (если null),
     *  - nextRunAt=sendAt (если cron отсутствует и nextRunAt не задан).
     *
     * Здесь лишь вычисляем nextRunAt для cron-сценария.
     */
    @Transactional
    public Reminder create(Reminder draft) {
        if (draft.getCronExpr() != null && !draft.getCronExpr().isBlank()) {
            CronExpression cron = CronExpression.parse(draft.getCronExpr());
            Instant next = cron.next(Instant.now().atZone(ZoneId.systemDefault())).toInstant();
            draft.setNextRunAt(next);
        }
        return repo.save(draft);
    }

    /**
     * Хелпер: отметить как отправленное одноразовое напоминание.
     * (для повторяющихся cron-напоминаний это не используется)
     */
    @Transactional
    public Reminder markSent(Reminder reminder) {
        reminder.setStatus(Reminder.Status.SENT);
        reminder.setNextRunAt(null);
        return repo.save(reminder);
    }
}
