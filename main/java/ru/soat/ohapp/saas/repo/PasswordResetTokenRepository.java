package ru.soat.ohapp.saas.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.soat.ohapp.saas.model.PasswordResetToken;

import java.util.List;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    List<PasswordResetToken> findByUserAccountId(UUID userAccountId);  // <-- было String
}
