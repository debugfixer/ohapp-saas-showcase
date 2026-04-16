package ru.soat.ohapp.saas.web;

import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantRoleEnricherFilter extends GenericFilter {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // TenantContext.get() -> String; конвертируем в UUID безопасно
        String tenantIdStr = TenantContext.get();
        UUID tenantId = null;
        try {
            if (tenantIdStr != null && !tenantIdStr.isBlank()) {
                tenantId = UUID.fromString(tenantIdStr);
            }
        } catch (IllegalArgumentException ignored) {
            tenantId = null;
        }

        if (auth != null && auth.isAuthenticated() && tenantId != null) {
            UUID userId = extractUserId(auth);
            if (userId != null) {
                String role = jdbc.query("""
                        select role
                        from user_tenant_memberships
                        where user_id=:u and tenant_id=:t
                        union
                        select role
                        from user_tenant_membership
                        where user_id=:u and tenant_id=:t
                        """,
                        new MapSqlParameterSource().addValue("u", userId).addValue("t", tenantId),
                        rs -> rs.next() ? rs.getString(1) : null);

                if (role != null && !role.isBlank()) {
                    Set<SimpleGrantedAuthority> merged = new HashSet<>();
                    auth.getAuthorities().forEach(a -> merged.add(new SimpleGrantedAuthority(a.getAuthority())));
                    merged.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));

                    Authentication enriched = new UsernamePasswordAuthenticationToken(
                            auth.getPrincipal(), auth.getCredentials(), merged);
                    SecurityContextHolder.getContext().setAuthentication(enriched);
                }
            }
        }

        chain.doFilter(request, response);
    }

    private UUID extractUserId(Authentication a) {
        try {
            var m = a.getPrincipal().getClass().getMethod("getId");
            Object v = m.invoke(a.getPrincipal());
            if (v instanceof UUID u) return u;
            if (v instanceof String s) return UUID.fromString(s);
        } catch (Exception ignored) {}
        try {
            return UUID.fromString(a.getName());
        } catch (Exception ignored) {}
        return null;
    }
}
