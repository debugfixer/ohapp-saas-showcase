package ru.soat.ohapp.saas.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.model.PasswordResetToken;
import ru.soat.ohapp.saas.model.UserAccount;
import ru.soat.ohapp.saas.repo.PasswordResetTokenRepository;
import ru.soat.ohapp.saas.repo.UserAccountRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthResetController {

    private final UserAccountRepository userRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private JavaMailSender mailSender;

    public AuthResetController(UserAccountRepository userRepo,
                               PasswordResetTokenRepository tokenRepo,
                               PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /* ------------------------ Запрос на сброс пароля ------------------------ */

    @PostMapping("/forgot")
    public ResponseEntity<?> forgot(@RequestBody ForgotReq req) {
        String email = safe(req.getEmail());
        if (email.isBlank()) return ResponseEntity.badRequest().body("email is required");

        UserAccount user = userRepo.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok().build();
        }

        String raw = java.util.UUID.randomUUID().toString().replace("-", "");
        String hash = passwordEncoder.encode(raw);

        PasswordResetToken token = new PasswordResetToken();
        token.setUserAccountId(user.getId());
        token.setTokenHash(hash);
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
        tokenRepo.save(token);

        String frontend = safe(req.getFrontendUrl());
        if (frontend.isBlank()) frontend = "http://localhost:3000";

        String link = frontend + "/reset-password?token=" + raw + "&email=" + user.getEmail()
                + (req.getTenant() != null && !req.getTenant().isBlank() ? "&tenant=" + req.getTenant().trim() : "");

        if (mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(user.getEmail());
                msg.setSubject("Password reset");
                msg.setText("Use this link to reset your password:\n" + link);
                mailSender.send(msg);
            } catch (Exception e) {
                System.out.println("[DEV] Failed to send email, link: " + link + " error: " + e.getMessage());
            }
        } else {
            System.out.println("[DEV] Mail sender is not configured. Reset link for " + user.getEmail() + ": " + link);
        }

        return ResponseEntity.ok().build();
    }

    /* -------------------------- Подтверждение сброса ------------------------ */

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestBody ResetReq req) {
        String email = safe(req.getEmail());
        String token = safe(req.getToken());
        String newPassword = safe(req.getNewPassword());

        if (email.isBlank() || token.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("email, token and newPassword are required");
        }

        UserAccount user = userRepo.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Invalid email or token");

        Instant now = Instant.now();
        List<PasswordResetToken> tokens = tokenRepo.findByUserAccountId(user.getId());

        PasswordResetToken match = tokens.stream()
                .filter(t -> t.getUsedAt() == null)
                .filter(t -> t.getExpiresAt() != null && t.getExpiresAt().isAfter(now))
                .filter(t -> passwordEncoder.matches(token, t.getTokenHash()))
                .findFirst()
                .orElse(null);

        if (match == null) return ResponseEntity.badRequest().body("Invalid or expired token");

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        match.setUsedAt(now);
        tokenRepo.save(match);

        return ResponseEntity.ok().build();
    }

    /* --------------------------------- DTOs -------------------------------- */

    public record ForgotReq(
            @Email @NotBlank String email,
            String frontendUrl,
            String tenant
    ) {
        public String getEmail() { return email; }
        public String getFrontendUrl() { return frontendUrl; }
        public String getTenant() { return tenant; }
    }

    public record ResetReq(
            @Email @NotBlank String email,
            @NotBlank String token,
            @NotBlank String newPassword
    ) {
        public String getEmail() { return email; }
        public String getToken() { return token; }
        public String getNewPassword() { return newPassword; }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
