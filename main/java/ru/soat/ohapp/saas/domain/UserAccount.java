package ru.soat.ohapp.saas.domain;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserAccount {
    private UUID id;
    private String username;
    private String passwordHash;
    private String email;
    private String displayName;
    private String role;

    // Для корректного геттера Lombok сгенерирует isActive()
    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
