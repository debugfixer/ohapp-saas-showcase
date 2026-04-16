package ru.soat.ohapp.saas.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // до аутентификации
public class TenantHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // ✅ ПРОПУСКАЕМ SWAGGER UI БЕЗ ПРОВЕРКИ TENANT
        String uri = req.getRequestURI();
        if (uri.startsWith("/swagger-ui") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/api-docs") ||
                uri.equals("/swagger-ui.html") ||
                uri.startsWith("/swagger-resources") ||
                uri.startsWith("/webjars/")) {

            chain.doFilter(req, res);
            return;
        }

        String slug = firstNonBlank(
                req.getHeader("X-Tenant-Slug"),
                req.getHeader("X-Tenant"),
                req.getParameter("tenant") // fallback: /auth/me?tenant=...
        );

        try {
            if (slug != null && !slug.isBlank()) {
                TenantContext.set(slug);
            }
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }
}
