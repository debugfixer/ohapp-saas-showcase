package ru.soat.ohapp.saas.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.soat.ohapp.saas.model.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByUsername(String username);
}
