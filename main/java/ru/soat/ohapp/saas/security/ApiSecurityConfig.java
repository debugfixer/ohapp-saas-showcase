package ru.soat.ohapp.saas.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.soat.ohapp.saas.tenancy.TenantHeaderFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ApiSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthEntryPoint restAuthEntryPoint;
    private final TenantHeaderFilter tenantHeaderFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(restAuthEntryPoint))
                .authorizeHttpRequests(reg -> reg
                        // ✅ SWAGGER UI — публичный доступ
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Публичные эндпоинты
                        .requestMatchers("/actuator/health", "/error", "/", "/index.html", "/assets/**", "/static/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/provision").permitAll()
                        .requestMatchers("/api/tenant-provision/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/tenants/select").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Все остальные требуют аутентификации
                        .anyRequest().authenticated()
                );

        // ВАЖНО: якорь — только встроенный фильтр Spring Security.
        // 1) Разрешаем определить арендатора до аутентификации
        http.addFilterBefore(tenantHeaderFilter, UsernamePasswordAuthenticationFilter.class);
        // 2) Выполняем JWT-аутентификацию до стандартного UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
