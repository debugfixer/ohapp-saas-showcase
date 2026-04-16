package ru.soat.ohapp.saas;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.soat.ohapp.saas.model.Tenant;
import ru.soat.ohapp.saas.model.UserAccount;
import ru.soat.ohapp.saas.model.UserTenantMembership;
import ru.soat.ohapp.saas.repo.TenantRepository;
import ru.soat.ohapp.saas.repo.UserAccountRepository;
import ru.soat.ohapp.saas.repo.UserTenantMembershipRepository;

import java.time.Instant;
import java.util.UUID;

@Component
@Profile({"dev"})
public class BootstrapSeed implements CommandLineRunner {

    private final TenantRepository tenants;
    private final UserAccountRepository users;
    private final UserTenantMembershipRepository memberships;
    private final PasswordEncoder passwordEncoder;

    public BootstrapSeed(TenantRepository tenants,
                         UserAccountRepository users,
                         UserTenantMembershipRepository memberships,
                         PasswordEncoder passwordEncoder) {
        this.tenants = tenants;
        this.users = users;
        this.memberships = memberships;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Tenant tenant = tenants.findBySlug("demo").orElseGet(() ->
                tenants.save(Tenant.builder()
                        .id(UUID.randomUUID())
                        .slug("demo")
                        .name("Demo Company")
                        .status("ACTIVE")
                        .createdAt(Instant.now())
                        .build())
        );

        UserAccount admin = users.findByEmailIgnoreCase("admin@demo.local").orElseGet(() ->
                users.save(UserAccount.builder()
                        .id(UUID.randomUUID())
                        .username("admin")
                        .email("admin@demo.local")
                        .displayName("Demo Admin")
                        .passwordHash(passwordEncoder.encode("admin"))
                        .role("ROLE_ADMIN_PLATFORM")
                        .isActive(true)
                        .createdAt(Instant.now())
                        .build())
        );

        memberships.findByUserIdAndTenantId(admin.getId(), tenant.getId())
                .orElseGet(() -> memberships.save(UserTenantMembership.builder()
                        .id(UUID.randomUUID())
                        .user(admin)
                        .tenant(tenant)
                        .role("ROLE_OWNER")
                        // ✅ УДАЛЕНА СТРОКА: .departmentScope("{}")
                        .build())
                );
    }
}
