package ru.soat.ohapp.saas.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_account_id", nullable = false)
    private UUID userAccountId;   // <-- было String, стало UUID

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant expiresAt;
    private Instant usedAt;
}
