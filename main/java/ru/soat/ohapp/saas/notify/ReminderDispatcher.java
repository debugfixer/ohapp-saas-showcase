// src/main/java/ru/soat/ohapp/saas/notify/ReminderDispatcher.java
package ru.soat.ohapp.saas.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.domain.Reminder;
import ru.soat.ohapp.saas.repo.ReminderRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderDispatcher {

    private final ReminderRepository repo;
    private final List<NotificationSender> senders;

    @Scheduled(fixedDelayString = "${reminders.poll.delay-ms:2000}")
    @Transactional
    public void tick() {
        Instant now = Instant.now();
        List<Reminder> due = repo.findDue(now);

        if (due.isEmpty()) {
            // раз в 30 циклов можно логировать пустой опрос, чтобы не спамить
            // (упрощённо: логируем всегда, но на DEBUG)
            log.debug("[reminders] no due reminders at {}", now);
            return;
        }

        log.info("[reminders] found {} due reminder(s)", due.size());

        for (Reminder r : due) {
            try {
                NotificationSender sender = senders.stream()
                        .filter(s -> s.supports(r.getChannel()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No sender for channel: " + r.getChannel()));

                sender.send(r);
                log.info("[reminders] sent id={} channel={} recipient={}", r.getId(), r.getChannel(), r.getRecipient());

                if (r.getCronExpr() != null && !r.getCronExpr().isBlank()) {
                    CronExpression cron = CronExpression.parse(r.getCronExpr());
                    Instant next = null;
                    // системная зона; при желании можно хранить таймзону на reminder
                    var nextZdt = cron.next(now.atZone(ZoneId.systemDefault()));
                    if (nextZdt != null) {
                        next = nextZdt.toInstant();
                    }
                    if (next == null) {
                        // CRON больше не даёт дат — помечаем завершённым
                        r.setStatus(Reminder.Status.SENT);
                        r.setNextRunAt(null);
                    } else {
                        r.setNextRunAt(next);
                        r.setStatus(Reminder.Status.PENDING);
                    }
                } else {
                    // одноразовое
                    r.setStatus(Reminder.Status.SENT);
                    r.setNextRunAt(null);
                }
                r.setAttempts(0);
                r.setLastError(null);
            } catch (Exception ex) {
                log.warn("[reminders] send failed id={} err={}", r.getId(), ex.toString());
                r.setAttempts(r.getAttempts() + 1);
                r.setLastError(ex.getMessage());
                // повтор через минуту
                r.setNextRunAt(Instant.now().plusSeconds(60));
                r.setStatus(Reminder.Status.FAILED);
            }
        }
        repo.saveAll(due);
    }
}
