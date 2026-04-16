package ru.soat.ohapp.saas.dto.admin;

import ru.soat.ohapp.saas.domain.LicensePlan;
import ru.soat.ohapp.saas.domain.LicenseStatus;

import java.time.Instant;
import java.util.UUID;

public record LicenseView(
        UUID id,
        UUID tenantId,
        LicensePlan plan,
        LicenseStatus status,
        int seats,
        Instant startsAt,
        Instant expiresAt,
        Instant suspendedUntil
) {}
