package ru.soat.ohapp.saas.dto.admin;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ru.soat.ohapp.saas.domain.LicensePlan;

import java.time.Instant;
import java.util.UUID;

public record LicenseUpsertRequest(
        @NotNull UUID tenantId,
        @NotNull LicensePlan plan,
        @Min(1) int seats,
        @NotNull Instant startsAt,
        @Future @NotNull Instant expiresAt
) {}
