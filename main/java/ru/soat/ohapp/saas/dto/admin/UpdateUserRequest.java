package ru.soat.ohapp.saas.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record UpdateUserRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        String role
) {}
