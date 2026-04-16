package ru.soat.ohapp.saas.domain;

import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.soat.ohapp.saas.model.UserAccount;
import ru.soat.ohapp.saas.model.Tenant;
import ru.soat.ohapp.saas.model.UserTenantMembership;
import ru.soat.ohapp.saas.repo.UserAccountRepository;
import ru.soat.ohapp.saas.repo.TenantRepository;
import ru.soat.ohapp.saas.repo.UserTenantMembershipRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Primary // ✅ ПРИОРИТЕТНЫЙ BEAN!
public class UserServiceImpl implements UserService {

    private final UserAccountRepository userRepo;
    private final TenantRepository tenantRepo;
    private final UserTenantMembershipRepository membershipRepo;
    private final PasswordEncoder passwordEncoder;

    // ✅ ЯВНЫЙ КОНСТРУКТОР С ДИАГНОСТИКОЙ
    public UserServiceImpl(
            UserAccountRepository userRepo,
            TenantRepository tenantRepo,
            UserTenantMembershipRepository membershipRepo,
            PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
        this.membershipRepo = membershipRepo;
        this.passwordEncoder = passwordEncoder;
        System.err.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.err.println("║ ✅ UserServiceImpl @Primary INITIALIZED                           ║");
        System.err.println("║ Class: " + this.getClass().getName() + "                            ║");
        System.err.println("╚═══════════════════════════════════════════════════════════════════╝");
    }

    /**
     * ✅ Аутентификация БЕЗ tenant (базовая проверка email + пароль)
     */
    @Override
    public Optional<ru.soat.ohapp.saas.dto.auth.UserDto> authenticate(String email, String password) {
        System.err.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.err.println("║ ⚠️ authenticate(2 params) CALLED - SHOULD NOT BE USED!            ║");
        System.err.println("╚═══════════════════════════════════════════════════════════════════╝");
        return userRepo.findByEmailIgnoreCase(email)
                .filter(user -> user.getIsActive())
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .map(user -> new ru.soat.ohapp.saas.dto.auth.UserDto(
                        user.getId().toString(),  // ✅ ДОБАВЛЕН ID!
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole()
                ));
    }

    /**
     * ✅ Аутентификация С tenant (проверяем membership + роль в tenant)
     */
    @Override
    public Optional<ru.soat.ohapp.saas.dto.auth.UserDto> authenticate(String email, String password, String tenantSlug) {
        System.err.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.err.println("║ 🔥🔥🔥 AUTHENTICATE WITH TENANT CALLED 🔥🔥🔥                   ║");
        System.err.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.err.println("=== AUTHENTICATE START ===");
        System.err.println("Email: " + email);
        System.err.println("Password provided: " + (password != null ? "YES" : "NO"));
        System.err.println("Tenant: " + tenantSlug);

        // 1. Найти пользователя
        Optional<UserAccount> userOpt = userRepo.findByEmailIgnoreCase(email);
        System.err.println("User found: " + userOpt.isPresent());

        if (userOpt.isEmpty()) {
            System.err.println("=== AUTHENTICATE FAILED: User not found ===");
            return Optional.empty();
        }

        UserAccount user = userOpt.get();
        System.err.println("User ID: " + user.getId());
        System.err.println("User active: " + user.getIsActive());

        // 2. Проверить активность
        if (!user.getIsActive()) {
            System.err.println("=== AUTHENTICATE FAILED: User is not active ===");
            return Optional.empty();
        }

        // 3. Проверить пароль
        System.err.println("Password hash from DB: " + user.getPasswordHash());
        System.err.println("Testing password match...");
        boolean passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
        System.err.println("Password matches: " + passwordMatches);

        if (!passwordMatches) {
            System.err.println("=== AUTHENTICATE FAILED: Password mismatch ===");
            return Optional.empty();
        }

        // 4. Найти tenant
        Optional<Tenant> tenantOpt = tenantRepo.findBySlug(tenantSlug);
        System.err.println("Tenant found: " + tenantOpt.isPresent());

        if (tenantOpt.isEmpty()) {
            System.err.println("=== AUTHENTICATE FAILED: Tenant not found ===");
            return Optional.empty();
        }

        Tenant tenant = tenantOpt.get();
        System.err.println("Tenant ID: " + tenant.getId());

        // 5. Проверить membership
        Optional<UserTenantMembership> membershipOpt =
                membershipRepo.findByUserIdAndTenantId(user.getId(), tenant.getId());
        System.err.println("Membership found: " + membershipOpt.isPresent());

        if (membershipOpt.isEmpty()) {
            System.err.println("=== AUTHENTICATE FAILED: Membership not found ===");
            return Optional.empty();
        }

        // 6. Получить роль в tenant (приоритет: роль в membership > роль пользователя)
        String role = membershipOpt.get().getRole();
        System.err.println("Role from membership: " + role);

        if (role == null || role.isBlank()) {
            role = user.getRole();
            System.err.println("Using user's default role: " + role);
        }

        System.err.println("=== AUTHENTICATE SUCCESS ===");

        // ✅ ИСПРАВЛЕНО: Добавлен ID пользователя!
        return Optional.of(new ru.soat.ohapp.saas.dto.auth.UserDto(
                user.getId().toString(),  // ✅ ID as String (ПЕРВЫЙ ПАРАМЕТР!)
                user.getEmail(),
                user.getDisplayName(),
                role
        ));
    }

    /**
     * ✅ Регистрация нового пользователя (создание в БД)
     */
    @Override
    public UserDto register(String email, String password, String name) {
        // Проверить, существует ли пользователь
        Optional<UserAccount> existing = userRepo.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            UserAccount user = existing.get();
            return new UserDto(user.getId().hashCode(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
        }

        // Создать нового пользователя
        UserAccount newUser = UserAccount.builder()
                .id(UUID.randomUUID())
                .username(email.split("@")[0])
                .email(email)
                .displayName(name)
                .passwordHash(passwordEncoder.encode(password))
                .role("ROLE_USER")
                .isActive(true)
                .createdAt(Instant.now())
                .build();

        UserAccount saved = userRepo.save(newUser);
        return new UserDto(saved.getId().hashCode(), saved.getEmail(), saved.getDisplayName(), saved.getCreatedAt());
    }

    @Override
    public void addUserToTenant(long userId, long tenantId, String role) {
        throw new UnsupportedOperationException("Use addUserToTenant(UUID, UUID, String) instead");
    }

    public void addUserToTenant(UUID userId, UUID tenantId, String role) {
        UserAccount user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        if (membershipRepo.existsByUserIdAndTenantId(userId, tenantId)) {
            return;
        }

        UserTenantMembership membership = UserTenantMembership.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tenant(tenant)
                .role(role)
                .build();

        membershipRepo.save(membership);
    }

    @Override
    public boolean isUserBoundToTenant(long userId, long tenantId) {
        throw new UnsupportedOperationException("Use isUserBoundToTenant(UUID, UUID) instead");
    }

    public boolean isUserBoundToTenant(UUID userId, UUID tenantId) {
        return membershipRepo.existsByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    public Optional<UserDto> getByEmail(String email) {
        return userRepo.findByEmailIgnoreCase(email)
                .map(user -> new UserDto(
                        user.getId().hashCode(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getCreatedAt()
                ));
    }

    @Override
    public Optional<UserDto> findUserInTenant(String email, String tenantSlug) {
        Optional<UserAccount> userOpt = userRepo.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) return Optional.empty();

        Optional<Tenant> tenantOpt = tenantRepo.findBySlug(tenantSlug);
        if (tenantOpt.isEmpty()) return Optional.empty();

        UserAccount user = userOpt.get();
        Tenant tenant = tenantOpt.get();

        boolean isMember = membershipRepo.existsByUserIdAndTenantId(user.getId(), tenant.getId());
        if (!isMember) return Optional.empty();

        return Optional.of(new UserDto(
                user.getId().hashCode(),
                user.getEmail(),
                user.getDisplayName(),
                user.getCreatedAt()
        ));
    }

    @Override
    public UserDto getOrCreate(String email, String password, String name) {
        return getByEmail(email).orElseGet(() -> register(email, password, name));
    }

    @Override
    public UserDto getByEmailOrThrow(String email) {
        return getByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }
}
