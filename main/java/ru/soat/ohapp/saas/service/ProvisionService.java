package ru.soat.ohapp.saas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.model.Tenant;
import ru.soat.ohapp.saas.model.UserAccount;
import ru.soat.ohapp.saas.model.UserTenantMembership;
import ru.soat.ohapp.saas.repo.TenantRepository;
import ru.soat.ohapp.saas.repo.UserAccountRepository;
import ru.soat.ohapp.saas.repo.UserTenantMembershipRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProvisionService {

    private final UserAccountRepository users;
    private final TenantRepository tenants;
    private final UserTenantMembershipRepository memberships;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UUID provision(String slug,
                          String adminEmail,
                          String adminPassword,
                          String adminDisplayName,
                          String adminUsername) {

        // 1) Тенант по slug (без IgnoreCase, как в твоём репо)
        Tenant tenant = tenants.findBySlug(slug).orElseGet(() -> tenants.save(
                Tenant.builder()
                        .slug(slug)
                        .name(slug)
                        .createdAt(Instant.now())
                        .build()
        ));

        // 2) Пользователь-админ
        UserAccount user = users.findByEmailIgnoreCase(adminEmail).orElseGet(() -> users.save(
                UserAccount.builder()
                        .email(adminEmail)
                        .username(adminUsername != null ? adminUsername : adminEmail)
                        .displayName(adminDisplayName != null ? adminDisplayName : adminUsername)
                        .passwordHash(passwordEncoder.encode(adminPassword))
                        .role("ROLE_ADMIN_PLATFORM")
                        .isActive(Boolean.TRUE)
                        .createdAt(Instant.now())
                        .build()
        ));

        // 3) Membership user↔tenant (если нет)
        memberships.findByUserIdAndTenantId(user.getId(), tenant.getId())
                .orElseGet(() -> memberships.save(
                        UserTenantMembership.builder()
                                .user(user)
                                .tenant(tenant)
                                .role("ROLE_OWNER")
                                .build()
                ));

        return tenant.getId();
    }
}
