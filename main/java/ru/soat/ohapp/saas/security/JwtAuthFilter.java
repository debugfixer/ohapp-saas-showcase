package ru.soat.ohapp.saas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        System.err.println("========================================");
        System.err.println("JWT FILTER: " + request.getMethod() + " " + path);

        // ПРОПУСКАЕМ auth endpoints БЕЗ ПРОВЕРКИ
        if (path.startsWith("/api/auth/")) {
            System.err.println("JWT FILTER: SKIPPING auth endpoint");
            System.err.println("========================================");
            filterChain.doFilter(request, response);
            return;
        }

        // ПРОВЕРЯЕМ COOKIES
        Cookie[] cookies = request.getCookies();
        System.err.println("JWT FILTER: Cookies count: " + (cookies != null ? cookies.length : 0));

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                System.err.println("JWT FILTER: Cookie: " + cookie.getName() + " = " +
                        (cookie.getValue().length() > 20 ? cookie.getValue().substring(0, 20) + "..." : cookie.getValue()));
            }
        }

        log.debug("🔹 JwtAuthFilter: Processing {} {}", request.getMethod(), path);

        String token = resolveToken(request);

        if (token != null) {
            System.err.println("JWT FILTER: ✅ Token FOUND!");
            System.err.println("JWT FILTER: Token preview: " + token.substring(0, Math.min(30, token.length())) + "...");

            try {
                log.debug("🔹 JWT token found, validating...");
                Claims claims = jwt.parseClaims(token);
                String subjectStr = claims.getSubject();

                if (subjectStr != null && !subjectStr.isEmpty()) {
                    log.debug("✅ JWT valid for subject: {}", subjectStr);
                    System.err.println("JWT FILTER: ✅ Token VALID! UserID: " + subjectStr);

                    // Извлекаем tenant SLUG
                    String tenantSlug = claims.get("tenant", String.class);
                    System.err.println("JWT FILTER: Tenant from token: " + tenantSlug);

                    // Извлекаем роли
                    Collection<SimpleGrantedAuthority> authorities = extractAuthorities(claims);
                    if (authorities.isEmpty()) {
                        authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                    }

                    System.err.println("JWT FILTER: Authorities: " + authorities);

                    // ✅ КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: Создаём JwtPrincipal с tenantSlug
                    JwtPrincipal principal = new JwtPrincipal(subjectStr, tenantSlug);
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            authorities
                    );

                    // ✅ Сохраняем tenantSlug в details для TenantMembershipFilter
                    Map<String, Object> details = new HashMap<>();
                    details.put("uid", subjectStr);
                    if (tenantSlug != null) {
                        details.put("tenantSlug", tenantSlug); // ✅ SLUG!
                        request.setAttribute("tenant", tenantSlug);
                    }

                    authentication.setDetails(details);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    System.err.println("JWT FILTER: ✅ Authentication SET in SecurityContext");
                    log.debug("✅ Authentication set for principal: {} with authorities: {}", principal, authorities);
                } else {
                    System.err.println("JWT FILTER: ⚠️ Token has NO SUBJECT!");
                    log.warn("⚠️ JWT has no subject, skipping authentication");
                }

            } catch (ExpiredJwtException e) {
                System.err.println("JWT FILTER: ❌ Token EXPIRED: " + e.getMessage());
                log.warn("🔴 JWT expired: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (MalformedJwtException | SignatureException e) {
                System.err.println("JWT FILTER: ❌ Token INVALID: " + e.getMessage());
                log.warn("🔴 Invalid JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                System.err.println("JWT FILTER: ❌ Unexpected error: " + e.getMessage());
                log.error("🔴 Unexpected error parsing JWT: {}", e.getMessage(), e);
                SecurityContextHolder.clearContext();
            }

        } else {
            System.err.println("JWT FILTER: ❌ Token NOT FOUND!");
            log.debug("🔹 No JWT token found in request");
        }

        System.err.println("========================================");
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // Проверяем Authorization header
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            System.err.println("JWT FILTER: Token found in Authorization header");
            return auth.substring(7);
        }

        // Проверяем cookies - ИСПРАВЛЕНО: ACCESSTOKEN без подчёркивания!
        var cookies = request.getCookies();
        if (cookies != null) {
            for (var c : cookies) {
                // ✅ КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: было "ACCESS_TOKEN", стало "ACCESSTOKEN"
                if ("ACCESSTOKEN".equals(c.getName())) {
                    System.err.println("JWT FILTER: Token found in ACCESSTOKEN cookie");
                    return c.getValue();
                }
            }
        }

        System.err.println("JWT FILTER: No token found in headers or cookies");
        return null;
    }

    private Collection<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof Collection<?> col) {
            return col.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .toList();
        }

        String roleStr = Optional.ofNullable(claims.get("role"))
                .map(Object::toString)
                .orElse("");

        if (roleStr.contains(",")) {
            return Arrays.stream(roleStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .toList();
        }

        roleStr = roleStr.trim();
        if (!roleStr.isEmpty()) {
            return List.of(new SimpleGrantedAuthority(roleStr));
        }

        return List.of();
    }

    /**
     * ✅ JwtPrincipal содержит userId и tenantSlug
     */
    public record JwtPrincipal(String uid, String tenantSlug) {
        public String getId() {
            return uid;
        }

        /**
         * ✅ Возвращает tenant SLUG (НЕ UUID!)
         */
        public String tenant() {
            return tenantSlug;
        }

        @Override
        public String toString() {
            return "JwtPrincipal(uid=" + uid + ", tenant=" + tenantSlug + ")";
        }
    }
}
