package ru.soat.ohapp.saas.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import ru.soat.ohapp.saas.config.AppTenantProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantResolverFilter extends OncePerRequestFilter {

    private final NamedParameterJdbcTemplate jdbc;
    private final AppTenantProperties props;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        final String method = req.getMethod();
        final String uri = req.getRequestURI();

        // 0) Публичные/статические пути всегда пропускаем
        if (HttpMethod.OPTIONS.matches(method)
                || uri.startsWith("/api/auth/")             // ← логин/логаут/refresh разрешаем без tenant
                || uri.startsWith("/actuator")
                || uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui")
                || uri.startsWith("/static") || uri.startsWith("/assets")
                || "/".equals(uri) || "/index.html".equals(uri) || "/favicon.ico".equals(uri)) {
            chain.doFilter(req, res);
            return;
        }

        // 1) Запрашиваем slug: header -> cookies -> query
        String slug = trim(req.getHeader("X-Tenant-Slug"));
        if (slug == null) slug = cookie(req, "TENANT_SLUG");
        if (slug == null) slug = cookie(req, "tenantSlug");
        if (slug == null) slug = trim(req.getParameter("tenant"));
        if (slug == null) slug = trim(req.getParameter("slug"));

        // 2) default slug (если настроен)
        if (slug == null) slug = trim(props.getDefaultSlug());

        // Если slug так и не получили — это ошибка для защищённых API
        if (slug == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenant slug is required");
        }

        // 3) resolve tenant_id
        UUID tenantId;
        try {
            tenantId = jdbc.queryForObject(
                    "select id from tenants where slug=:slug",
                    new MapSqlParameterSource("slug", slug),
                    (rs, rn) -> (UUID) rs.getObject(1));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "tenant not found: " + slug);
        }

        // 4) Пишем в запрос и ThreadLocal-контекст
        req.setAttribute("TENANT_ID", tenantId);
        req.setAttribute("TENANT_SLUG", slug);

        String previous = null;
        try {
            previous = TenantContext.get();
            TenantContext.set(tenantId.toString()); // у тебя контекст хранит String
            chain.doFilter(req, res);
        } finally {
            if (previous != null) {
                TenantContext.set(previous);
            } else {
                TenantContext.clear();
            }
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private static String cookie(HttpServletRequest req, String name) {
        return Optional.ofNullable(req.getCookies())
                .map(Arrays::stream).orElseGet(java.util.stream.Stream::empty)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .map(TenantResolverFilter::trim)
                .filter(v -> v != null)
                .findFirst().orElse(null);
    }
}
