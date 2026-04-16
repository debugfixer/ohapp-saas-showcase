package ru.soat.ohapp.saas.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/provision")
@RequiredArgsConstructor
public class TenantProvisionController {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    @Transactional
    public ResponseEntity<?> provision(@Valid @RequestBody ProvisionReq req) {
        // 1) TENANT upsert
        UUID tenantId = findTenantIdBySlug(req.slug());
        if (tenantId == null) {
            tenantId = UUID.randomUUID();
            String code = (req.code() == null || req.code().isBlank())
                    ? req.slug().toUpperCase()
                    : req.code();
            jdbc.update("""
                    INSERT INTO tenants(id, slug, code, name, status, created_at)
                    VALUES (?, ?, ?, ?, 'ACTIVE', now())
                    ON CONFLICT (slug) DO NOTHING
                    """,
                    tenantId, req.slug(), code, req.name() == null ? req.slug() : req.name()
            );
            UUID again = findTenantIdBySlug(req.slug());
            if (again != null) tenantId = again;
        }

        // 2) USER upsert (user_accounts)
        UUID userId = findUserIdByEmail(req.ownerEmail());
        if (userId == null) {
            userId = UUID.randomUUID();
            String username = req.ownerEmail(); // username=Email (unique)
            String displayName = req.ownerName() == null ? "Owner" : req.ownerName();
            String hash = passwordEncoder.encode(req.ownerPassword());
            jdbc.update("""
                    INSERT INTO user_accounts(id, username, email, display_name, password_hash, role, is_active, created_at)
                    VALUES (?, ?, ?, ?, ?, NULL, TRUE, now())
                    ON CONFLICT (email) DO NOTHING
                    """,
                    userId, username, req.ownerEmail(), displayName, hash
            );
            UUID again = findUserIdByEmail(req.ownerEmail());
            if (again != null) userId = again;
        }

        // 3) MEMBERSHIP upsert (singular table user_tenant_membership)
        jdbc.update("""
                INSERT INTO user_tenant_membership(id, user_id, tenant_id, role, department_scope)
                VALUES (?, ?, ?, ?, '{}'::text)
                ON CONFLICT ON CONSTRAINT uk_user_tenant_unique DO NOTHING
                """,
                UUID.randomUUID(), userId, tenantId, "OWNER"
        );

        // 4) LICENSE upsert (unique per tenant_id)
        upsertDefaultLicenseIfAbsent(tenantId, req.plan(), req.seats(), req.validityDays());

        // 5) Ответ
        Map<String, Object> dto = Map.of(
                "tenant", Map.of("id", tenantId, "slug", req.slug()),
                "owner",  Map.of("id", userId, "email", req.ownerEmail()),
                "membership", "OWNER",
                "license", Map.of(
                        "plan", (req.plan() == null || req.plan().isBlank()) ? "BASIC" : req.plan(),
                        "seats", (req.seats() == null || req.seats() <= 0) ? 10 : req.seats(),
                        "validDays", (req.validityDays() == null || req.validityDays() <= 0) ? 365 : req.validityDays()
                )
        );
        return ResponseEntity.ok(dto);
    }

    // ===== helpers =====

    private UUID findTenantIdBySlug(String slug) {
        try {
            return jdbc.queryForObject("SELECT id FROM tenants WHERE slug = ?", UUID.class, slug);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private UUID findUserIdByEmail(String email) {
        try {
            return jdbc.queryForObject("SELECT id FROM user_accounts WHERE email = ?", UUID.class, email);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private void upsertDefaultLicenseIfAbsent(UUID tenantId, String plan, Integer seats, Integer validityDays) {
        String effectivePlan = (plan == null || plan.isBlank()) ? "BASIC" : plan;
        int effectiveSeats = (seats == null || seats <= 0) ? 10 : seats;
        int days = (validityDays == null || validityDays <= 0) ? 365 : validityDays;

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime exp = now.plusDays(days);

        jdbc.update("""
                INSERT INTO licenses(id, tenant_id, plan, status, seats, starts_at, expires_at, issued_at, created_at, updated_at)
                VALUES (?, ?, ?, 'ACTIVE', ?, ?, ?, ?, now(), now())
                ON CONFLICT (tenant_id) DO NOTHING
                """,
                UUID.randomUUID(), tenantId, effectivePlan, effectiveSeats, now, exp, now
        );
    }

    // ===== DTO без Lombok: record с валидацией =====
    public record ProvisionReq(
            @NotBlank String slug,
            String code,
            String name,
            @Email @NotBlank String ownerEmail,
            @NotBlank String ownerPassword,
            String ownerName,
            String plan,
            Integer seats,
            Integer validityDays
    ) {}
}
