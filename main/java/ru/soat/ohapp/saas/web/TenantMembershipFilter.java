package ru.soat.ohapp.saas.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import ru.soat.ohapp.saas.tenancy.TenantContext;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantMembershipFilter extends GenericFilter {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest http = (HttpServletRequest) req;
        final String uri = http.getRequestURI();
        final String method = http.getMethod();

        try {
            // ✅ Публичные ветки
            if ("OPTIONS".equalsIgnoreCase(method)
                    || uri.startsWith("/api/auth")
                    || uri.startsWith("/api/tenants")
                    || uri.startsWith("/api/health")
                    || uri.startsWith("/error")
                    || uri.startsWith("/actuator/health")
                    || uri.startsWith("/actuator/info")
                    || uri.startsWith("/static")
                    || uri.startsWith("/assets")
                    || uri.startsWith("/swagger-ui")
                    || uri.startsWith("/v3/api-docs")
                    || uri.startsWith("/api-docs")
                    || uri.equals("/swagger-ui.html")
                    || uri.startsWith("/swagger-resources")
                    || uri.startsWith("/webjars/")
                    || "/".equals(uri)) {
                chain.doFilter(req, res);
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                chain.doFilter(req, res);
                return;
            }

            // ✅ Получить tenantId из request attribute (проставил TenantResolverFilter)
            Object raw = http.getAttribute("TENANT_ID");
            UUID tenantId = (raw instanceof UUID) ? (UUID) raw : null;
            if (tenantId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenant is not resolved");
            }

            UUID userId = resolveUserId(auth);
            if (userId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "cannot_resolve_user_id");
            }

            // ✅ Проверка membership в БД
            boolean hasMembership = Boolean.TRUE.equals(jdbc.queryForObject("""
                    select exists(
                        select 1 from user_tenant_memberships where user_id=:u and tenant_id=:t
                        union all
                        select 1 from user_tenant_membership where user_id=:u and tenant_id=:t
                    )
                    """,
                    new MapSqlParameterSource()
                            .addValue("u", userId)
                            .addValue("t", tenantId),
                    (rs, rn) -> rs.getBoolean(1)));

            if (!hasMembership) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no_membership");
            }

            // ✅ СОХРАНИТЬ tenantId в TenantContext (ThreadLocal)
            TenantContext.setTenantId(tenantId);

            log.debug("✅ Enriched TenantContext with tenantId: {}", tenantId);

            chain.doFilter(req, res);

        } finally {
            // ✅ ОЧИСТИТЬ ThreadLocal после обработки запроса
            TenantContext.clear();
        }
    }

    private UUID resolveUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal != null) {
            try {
                var method = principal.getClass().getMethod("getId");
                Object value = method.invoke(principal);
                if (value instanceof UUID) return (UUID) value;
                if (value instanceof String) return UUID.fromString((String) value);
            } catch (Exception ignored) {
            }
        }

        String name = Objects.toString(auth.getName(), "");
        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException ignored) {
        }

        String key = name.toLowerCase();
        try {
            return jdbc.queryForObject("""
                    select id from user_accounts
                    where lower(username)=:n or lower(email)=:n
                    """,
                    new MapSqlParameterSource("n", key),
                    (rs, rn) -> UUID.fromString(rs.getString("id")));
        } catch (Exception e) {
            return null;
        }
    }
}
