package ru.soat.ohapp.saas.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAccount {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    private String displayName;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role; // например ROLE_ADMIN_PLATFORM

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (isActive == null) isActive = true;
        if (role == null) role = "ROLE_USER";
    }
}
