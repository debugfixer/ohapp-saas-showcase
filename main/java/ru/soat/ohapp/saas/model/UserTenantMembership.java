package ru.soat.ohapp.saas.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(
        name = "user_tenant_memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tenant_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserTenantMembership {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false)
    private String role; // ROLE_ADMIN / ROLE_HR / ROLE_VIEWER

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
