package ru.soat.ohapp.saas.dto.admin;

import java.util.UUID;

public record AdminUserView(
        UUID id,
        String fullName,
        String email,
        String role
) {}
