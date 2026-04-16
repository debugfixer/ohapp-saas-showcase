// src/main/java/ru/soat/ohapp/saas/config/CorsConfig.java

package ru.soat.ohapp.saas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS конфигурация с поддержкой dev и production origins.
 */
@Configuration
public class CorsConfig {

    /**
     * Разрешённые origins из application.yml.
     * Dev: http://localhost:3000, http://localhost:5173
     * Prod: https://app.yourdomain.com (из переменной окружения)
     */
    @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // ═══════════════════════════════════════════════════════════
        // ALLOWED ORIGINS (поддержка dev + prod через конфиг)
        // ═══════════════════════════════════════════════════════════
        cfg.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // ═══════════════════════════════════════════════════════════
        // ALLOWED METHODS
        // ═══════════════════════════════════════════════════════════
        cfg.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));

        // ═══════════════════════════════════════════════════════════
        // ALLOWED HEADERS
        // ═══════════════════════════════════════════════════════════
        if ("*".equals(allowedHeaders)) {
            cfg.setAllowedHeaders(List.of("*"));
        } else {
            cfg.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        }

        // ═══════════════════════════════════════════════════════════
        // CREDENTIALS (для cookies, JWT в Authorization)
        // ═══════════════════════════════════════════════════════════
        cfg.setAllowCredentials(allowCredentials);

        // ═══════════════════════════════════════════════════════════
        // EXPOSED HEADERS (доступны в браузере)
        // ═══════════════════════════════════════════════════════════
        cfg.setExposedHeaders(List.of(
                "Authorization",
                "Set-Cookie",
                "X-Tenant-Slug",
                "Content-Disposition",  // Для скачивания файлов
                "X-Total-Count"         // Для пагинации
        ));

        // ═══════════════════════════════════════════════════════════
        // MAX AGE (кеш preflight)
        // ═══════════════════════════════════════════════════════════
        cfg.setMaxAge(maxAge);

        // ═══════════════════════════════════════════════════════════
        // APPLY TO ALL ENDPOINTS
        // ═══════════════════════════════════════════════════════════
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);

        return source;
    }
}
