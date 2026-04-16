package ru.soat.ohapp.saas.notify;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.soat.ohapp.saas.service.admin.LicenseService;

@Component
@RequiredArgsConstructor
public class LicenseScheduler {

    private final LicenseService service;

    @Scheduled(cron = "0 */10 * * * *") // каждые 10 минут
    public void tick() {
        service.autoExpireAndActivate();
    }
}
