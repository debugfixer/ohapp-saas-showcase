package ru.soat.ohapp.saas.web;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.domain.UserService;
import ru.soat.ohapp.saas.dto.auth.LoginRequest;
import ru.soat.ohapp.saas.dto.auth.LoginResponse;
import ru.soat.ohapp.saas.dto.auth.TenantDto;
import ru.soat.ohapp.saas.dto.auth.UserDto;
import ru.soat.ohapp.saas.security.JwtService;

import java.time.Duration;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    private static final String JWT_COOKIE_NAME = "ACCESSTOKEN";
    private static final int COOKIE_MAX_AGE_SECONDS = 24 * 60 * 60;
    private static final boolean COOKIE_HTTP_ONLY = true;
    private static final boolean COOKIE_SECURE = false;
    // ВРЕМЕННО ОТКЛЮЧЕНО для отладки:
    // private static final String COOKIE_SAME_SITE = "Lax";

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
        System.err.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.err.println("║         AuthController CONSTRUCTOR CALLED                        ║");
        System.err.println("║ UserService class: " + userService.getClass().getName() + " ║");
        System.err.println("║ JwtService class: " + jwtService.getClass().getName() + "  ║");
        System.err.println("╚═══════════════════════════════════════════════════════════════════╝");
    }

    @PostConstruct
    public void init() {
        System.err.println("\n\n");
        System.err.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.err.println("║    🔥🔥🔥 AuthController @PostConstruct INITIALIZED 🔥🔥🔥      ║");
        System.err.println("║ UserService class: " + userService.getClass().getName() + " ║");
        System.err.println("║ JwtService injected: " + (jwtService != null ? "YES ✅" : "NO ❌") + "                         ║");
        System.err.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.err.println("\n\n");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest req,
            HttpServletResponse httpResponse) {

        System.err.println("========================================");
        System.err.println("=== AUTH CONTROLLER LOGIN CALLED ===");
        System.err.println("Request email: " + req.getEmail());
        System.err.println("Request tenant: " + req.getTenant());
        System.err.println("Password provided: " + (req.getPassword() != null ? "YES" : "NO"));
        System.err.println("HttpServletResponse class: " + httpResponse.getClass().getName());
        System.err.println("========================================");

        Optional<UserDto> userOpt;
        try {
            System.err.println("🔹 Calling userService.authenticate()...");
            userOpt = userService.authenticate(req.getEmail(), req.getPassword(), req.getTenant());
            System.err.println("🔹 userService.authenticate() completed");
        } catch (Exception e) {
            System.err.println("=== EXCEPTION IN AUTHENTICATE ===");
            System.err.println("Exception type: " + e.getClass().getName());
            System.err.println("Exception message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==================================");
            return ResponseEntity.status(500)
                    .body(new LoginResponse(null, null, null, "INTERNAL_ERROR"));
        }

        System.err.println("Authentication result: " + (userOpt.isPresent() ? "SUCCESS ✅" : "FAILED ❌"));

        if (userOpt.isEmpty()) {
            System.err.println("=== AUTH CONTROLLER: RETURNING 401 ===");
            return ResponseEntity.status(401)
                    .body(new LoginResponse(null, null, null, "UNAUTHORIZED"));
        }

        UserDto user = userOpt.get();
        try {
            System.err.println("🔹 Generating JWT token for user ID: " + user.getId());
            System.err.println("🔹 Tenant slug: " + req.getTenant());

            String jwtToken = jwtService.generateToken(
                    user.getId(),
                    req.getTenant(),
                    Duration.ofHours(24)
            );

            System.err.println("🔹 JWT token generated successfully!");
            System.err.println("🔹 Token subject (userID): " + user.getId());
            System.err.println("🔹 Token tenant: " + req.getTenant());
            System.err.println("🔹 Token preview: " + jwtToken.substring(0, Math.min(30, jwtToken.length())) + "...");

            // ============================================
            // CRITICAL: Set JWT in HttpOnly Cookie
            // ============================================
            Cookie jwtCookie = createJwtCookie(jwtToken);
            httpResponse.addCookie(jwtCookie);

            System.err.println("✅ JWT cookie set: name=" + JWT_COOKIE_NAME +
                    ", httpOnly=" + COOKIE_HTTP_ONLY +
                    ", secure=" + COOKIE_SECURE +
                    ", maxAge=" + COOKIE_MAX_AGE_SECONDS);

            TenantDto tenant = new TenantDto(req.getTenant(), "Demo Tenant");

            LoginResponse response = new LoginResponse(
                    null,  // ✅ НЕ возвращаем token в body!
                    user,
                    tenant,
                    user.getRole()
            );

            System.err.println("=== AUTH CONTROLLER: RETURNING 200 OK (token in cookie, NOT in body) ===");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("🔴 ERROR GENERATING JWT:");
            System.err.println("Exception: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new LoginResponse(null, null, null, "TOKEN_GENERATION_ERROR"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse httpResponse) {
        System.err.println("========================================");
        System.err.println("AUTH CONTROLLER: LOGOUT CALLED");

        // ✅ Удаляем cookie установив maxAge=0
        Cookie logoutCookie = createLogoutCookie();
        httpResponse.addCookie(logoutCookie);

        System.err.println("✅ JWT cookie cleared (maxAge=0)");
        System.err.println("AUTH CONTROLLER: RETURNING 200 OK");
        System.err.println("========================================");

        return ResponseEntity.ok().build();
    }

    private Cookie createJwtCookie(String token) {
        System.err.println("🔹 Creating JWT cookie...");
        System.err.println("   - Name: " + JWT_COOKIE_NAME);
        System.err.println("   - Token length: " + token.length());
        System.err.println("   - HttpOnly: " + COOKIE_HTTP_ONLY);
        System.err.println("   - Secure: " + COOKIE_SECURE);
        System.err.println("   - MaxAge: " + COOKIE_MAX_AGE_SECONDS);

        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
        cookie.setHttpOnly(COOKIE_HTTP_ONLY);
        cookie.setSecure(COOKIE_SECURE);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);

        // ВРЕМЕННО ОТКЛЮЧЕНО для отладки cross-origin:
        // cookie.setAttribute("SameSite", COOKIE_SAME_SITE);

        System.err.println("✅ JWT cookie created successfully");
        return cookie;
    }

    private Cookie createLogoutCookie() {
        System.err.println("🔹 Creating logout cookie (maxAge=0)...");

        Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
        cookie.setHttpOnly(COOKIE_HTTP_ONLY);
        cookie.setSecure(COOKIE_SECURE);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // ✅ Удаляем cookie

        System.err.println("✅ Logout cookie created");
        return cookie;
    }
}
