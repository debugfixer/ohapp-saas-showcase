package ru.soat.ohapp.saas.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordHashTool {
    public static void main(String[] args) {
        String raw = args.length > 0 ? args[0] : "admin";

        // 1) Делегирующий (даёт {bcrypt}.. по умолчанию)
        PasswordEncoder delegating = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        System.out.println("Delegating (default bcrypt): " + delegating.encode(raw));


    }
}