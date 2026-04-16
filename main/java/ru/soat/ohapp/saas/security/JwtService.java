package ru.soat.ohapp.saas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secret; // минимум 32 байта или Base64

    @Value("${security.jwt.base64-secret:true}")
    private boolean base64Mode;

    private SecretKey getSigningKey() {
        byte[] keyBytes = base64Mode
                ? Decoders.BASE64.decode(secret)
                : secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /* ==== БАЗОВЫЙ МЕТОД (subject + expiration) ==== */
    public String generateToken(String subject, Duration ttl) {
        var key = getSigningKey();
        var now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /* ==== ✅ НОВЫЙ МЕТОД: JWT С TENANT CLAIM! ==== */
    /**
     * Генерация JWT с subject (userId) и tenant claim
     * @param userId User ID (UUID string)
     * @param tenantSlug Tenant slug (например, "demo-clinic")
     * @param ttl Время жизни токена
     * @return JWT token
     */
    public String generateToken(String userId, String tenantSlug, Duration ttl) {
        var key = getSigningKey();
        var now = Instant.now();
        return Jwts.builder()
                .subject(userId)                    // ✅ USER ID
                .claim("tenant", tenantSlug)        // ✅ TENANT SLUG!
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /* ==== PARSING METHODS ==== */
    public Claims parseClaims(String token) {
        var key = getSigningKey();
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Совместимость с JwtAuthFilter: jwt.parse(token).getPayload()
    public Jwt<?, Claims> parse(String token) {
        var key = getSigningKey();
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /* ==== ✅ ИЗВЛЕЧЕНИЕ TENANT ИЗ JWT ==== */
    /**
     * Извлекает tenant slug из JWT claim
     * @param token JWT токен
     * @return tenant slug или null, если отсутствует
     */
    public String extractTenant(String token) {
        return parseClaims(token).get("tenant", String.class);
    }

    /* ==== ДОПОЛНИТЕЛЬНЫЕ ПЕРЕГРУЗКИ ==== */

    /** Генерация по subject и TTL в минутах */
    public String generate(String subject, int ttlMinutes) {
        return generateToken(subject, Duration.ofMinutes(ttlMinutes));
    }

    /** Генерация по произвольным claim'ам и TTL в минутах */
    public String generate(Map<String, Object> claims, int ttlMinutes) {
        var key = getSigningKey();
        var now = Instant.now();
        return Jwts.builder()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(ttlMinutes))))
                .claims(claims)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
