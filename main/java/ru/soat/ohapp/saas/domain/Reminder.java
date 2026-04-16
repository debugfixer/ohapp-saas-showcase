package ru.soat.ohapp.saas.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.Builder.Default;   // <— важно: импорт @Builder.Default

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reminders")
public class Reminder {
    public enum Channel { EMAIL, SMS, WHATSAPP }
    public enum Status  { PENDING, SENT, FAILED, CANCELLED }

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private String recipient;

    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "send_at")
    private Instant sendAt;

    @Column(name = "cron_expr")
    private String cronExpr;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Default                               // <— дефолт работает и при использовании билдера
    private Status status = Status.PENDING;

    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (nextRunAt == null) nextRunAt = sendAt; // для одноразовых
        if (status == null) status = Status.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
