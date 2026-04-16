package ru.soat.ohapp.saas.api.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.dto.auth.ProfileView;
import ru.soat.ohapp.saas.security.JwtAuthFilter.JwtPrincipal;
import ru.soat.ohapp.saas.tenancy.TenantContext;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {

    private final NamedParameterJdbcTemplate jdbc;

    @GetMapping("/me")
    public ProfileView me(Authentication auth) {
        UUID userId = resolveUserId(auth);
        String tenantSlug = TenantContext.get();

        return jdbc.queryForObject("""
            select ua.id,
                   ua.email,
                   ua.display_name,
                   coalesce(m.role,'EMPLOYEE') as role,
                   t.slug as tenant_slug,
                   t.name as tenant_name
              from user_accounts ua
              join tenants t on t.slug=:t
              left join user_tenant_memberships m
                     on m.user_id=ua.id and m.tenant_id=t.id
             where ua.id=:u
        """,
                new MapSqlParameterSource()
                        .addValue("u", userId)
                        .addValue("t", tenantSlug),
                (rs, rn) -> new ProfileView(
                        (UUID) rs.getObject("id"),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("role"),
                        rs.getString("tenant_slug"),
                        rs.getString("tenant_name")
                ));
    }

    private UUID resolveUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof JwtPrincipal jp) {
            return UUID.fromString(jp.uid());
        }
        return UUID.fromString(auth.getName());
    }
}
