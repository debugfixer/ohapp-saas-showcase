package ru.soat.ohapp.saas.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory реализация доменных сервисов без циклических зависимостей бинов.
 */
@Configuration
public class InMemoryDomainServices {

    /* ========================= STORE (общие структуры) ========================= */
    public static final class Store {
        // Tenants
        private final AtomicLong tenantSeq = new AtomicLong(1);
        private final Map<Long, TenantDto> tenantsById = new ConcurrentHashMap<>();
        private final Map<String, Long> tenantIdBySlug = new ConcurrentHashMap<>();

        // Users
        private final AtomicLong userSeq = new AtomicLong(1);
        private final Map<Long, UserDto> usersById = new ConcurrentHashMap<>();
        private final Map<String, Long> userIdByEmail = new ConcurrentHashMap<>();
        private final Map<String, String> passwordByEmail = new ConcurrentHashMap<>();

        // User↔Tenant bindings: userId -> (tenantId -> role)
        private final Map<Long, Map<Long, String>> userTenantBindings = new ConcurrentHashMap<>();

        /* getters */
        public AtomicLong tenantSeq() { return tenantSeq; }
        public Map<Long, TenantDto> tenantsById() { return tenantsById; }
        public Map<String, Long> tenantIdBySlug() { return tenantIdBySlug; }
        public AtomicLong userSeq() { return userSeq; }
        public Map<Long, UserDto> usersById() { return usersById; }
        public Map<String, Long> userIdByEmail() { return userIdByEmail; }
        public Map<String, String> passwordByEmail() { return passwordByEmail; }
        public Map<Long, Map<Long, String>> userTenantBindings() { return userTenantBindings; }
    }

    @Bean
    public Store store() {
        return new Store();
    }

    /* ========================= TenantService ========================= */
    @Bean
    public TenantService tenantService(Store store) {
        return new TenantServiceImpl(store);
    }

    static final class TenantServiceImpl implements TenantService {
        private final Store store;

        TenantServiceImpl(Store store) {
            this.store = store;
        }

        @Override
        public TenantDto create(String name, String slug) {
            Long existing = store.tenantIdBySlug().get(slug);
            if (existing != null) {
                return store.tenantsById().get(existing);
            }

            long id = store.tenantSeq().getAndIncrement();
            TenantDto dto = new TenantDto(id, name, slug, Instant.now());
            store.tenantsById().put(id, dto);
            store.tenantIdBySlug().put(slug, id);
            return dto;
        }

        @Override
        public Optional<TenantDto> findBySlug(String slug) {
            Long id = store.tenantIdBySlug().get(slug);
            return id == null ? Optional.empty() : Optional.ofNullable(store.tenantsById().get(id));
        }

        @Override
        public void bindUserRoleIfAbsent(long userId, long tenantId, String role) {
            if (!store.tenantsById().containsKey(tenantId)) {
                throw new IllegalArgumentException("Tenant not found: " + tenantId);
            }

            store.userTenantBindings()
                    .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                    .putIfAbsent(tenantId, role);
        }
    }

    /* ========================= UserService ========================= */
    // ❌ ЗАКОММЕНТИРОВАНО: Spring будет использовать UserServiceImpl с @Service + @Primary
    /*
    @Bean
    public UserService userService(Store store, TenantService tenantService) {
        return new UserServiceImpl(store, tenantService);
    }

    static final class UserServiceImpl implements UserService {
        private final Store store;
        private final TenantService tenantService;

        UserServiceImpl(Store store, TenantService tenantService) {
            this.store = store;
            this.tenantService = tenantService;
        }

        @Override
        public UserDto register(String email, String password, String name) {
            String key = email.toLowerCase();
            Long existing = store.userIdByEmail().get(key);
            if (existing != null) return store.usersById().get(existing);
            long id = store.userSeq().getAndIncrement();
            UserDto dto = new UserDto(id, email, name, Instant.now());
            store.usersById().put(id, dto);
            store.userIdByEmail().put(key, id);
            store.passwordByEmail().put(key, password);
            return dto;
        }

        @Override
        public Optional<ru.soat.ohapp.saas.dto.auth.UserDto> authenticate(String email, String password) {
            String key = email.toLowerCase();
            Long id = store.userIdByEmail().get(key);
            if (id == null) return Optional.empty();
            String stored = store.passwordByEmail().get(key);
            if (stored == null || !stored.equals(password)) return Optional.empty();
            UserDto domainUser = store.usersById().get(id);
            if (domainUser == null) return Optional.empty();
            return Optional.of(toAuthDto(domainUser, "UNKNOWN"));
        }

        @Override
        public Optional<ru.soat.ohapp.saas.dto.auth.UserDto> authenticate(String email, String password, String tenantSlug) {
            String key = email.toLowerCase();
            Long id = store.userIdByEmail().get(key);
            if (id == null) return Optional.empty();
            String stored = store.passwordByEmail().get(key);
            if (stored == null || !stored.equals(password)) return Optional.empty();
            UserDto domainUser = store.usersById().get(id);
            if (domainUser == null) return Optional.empty();

            Optional<TenantDto> tenantOpt = tenantService.findBySlug(tenantSlug);
            if (tenantOpt.isEmpty()) return Optional.empty();
            long tenantId = tenantOpt.get().id();

            if (!isUserBoundToTenant(domainUser.id(), tenantId)) return Optional.empty();
            String role = getRoleFor(domainUser.id(), tenantId);
            if (role == null || role.isBlank()) role = "USER";
            return Optional.of(toAuthDto(domainUser, role));
        }

        @Override
        public void addUserToTenant(long userId, long tenantId, String role) {
            if (!store.usersById().containsKey(userId)) {
                throw new IllegalArgumentException("User not found: " + userId);
            }
            if (!store.tenantsById().containsKey(tenantId)) {
                throw new IllegalArgumentException("Tenant not found: " + tenantId);
            }
            store.userTenantBindings()
                    .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                    .putIfAbsent(tenantId, role);
        }

        @Override
        public boolean isUserBoundToTenant(long userId, long tenantId) {
            Map<Long, String> map = store.userTenantBindings().get(userId);
            return map != null && map.containsKey(tenantId);
        }

        private String getRoleFor(long userId, long tenantId) {
            Map<Long, String> map = store.userTenantBindings().get(userId);
            return map == null ? null : map.get(tenantId);
        }

        @Override
        public Optional<UserDto> getByEmail(String email) {
            Long id = store.userIdByEmail().get(email.toLowerCase());
            return id == null ? Optional.empty() : Optional.ofNullable(store.usersById().get(id));
        }

        @Override
        public Optional<UserDto> findUserInTenant(String email, String tenantSlug) {
            Optional<UserDto> userOpt = getByEmail(email);
            if (userOpt.isEmpty()) return Optional.empty();
            Optional<TenantDto> tenantOpt = tenantService.findBySlug(tenantSlug);
            if (tenantOpt.isEmpty()) return Optional.empty();
            long userId = userOpt.get().id();
            long tenantId = tenantOpt.get().id();
            return isUserBoundToTenant(userId, tenantId) ? userOpt : Optional.empty();
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

        private ru.soat.ohapp.saas.dto.auth.UserDto toAuthDto(UserDto u, String role) {
            return new ru.soat.ohapp.saas.dto.auth.UserDto(
                    u.email(),
                    u.name(),
                    role
            );
        }
    }
    */
}
