package ru.soat.ohapp.saas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProvisionTenantRequest(
        @NotBlank String secret,
        @NotBlank String slug,
        @NotBlank String name,
        @Email @NotBlank String adminEmail,
        @NotBlank String adminPassword,
        @NotBlank String adminUsername,
        String adminDisplayName
) {}
