package ru.soat.ohapp.saas.dto.admin;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record LicenseStatusRequest(
        @NotNull String action,     // SUSPEND | RESUME
        Instant until               // для SUSPEND можно указать дату
) {}
