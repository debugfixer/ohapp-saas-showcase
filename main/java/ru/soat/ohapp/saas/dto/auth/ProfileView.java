// ru.soat.ohapp.saas.dto.auth.ProfileView.java
package ru.soat.ohapp.saas.dto.auth;

import java.util.UUID;

public record ProfileView(
        UUID id,
        String email,
        String displayName,
        String role,
        String tenantSlug,
        String tenantName
) {}
