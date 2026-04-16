package ru.soat.ohapp.saas.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUserRequest(
        @NotNull UUID tenantId,
        @NotBlank String fullName,
        @Email @NotBlank String email,
        @NotBlank String role // e.g. ADMIN / MANAGER / EMPLOYEE
) {}
