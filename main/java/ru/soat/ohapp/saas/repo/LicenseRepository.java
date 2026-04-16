package ru.soat.ohapp.saas.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.soat.ohapp.saas.domain.License;
import ru.soat.ohapp.saas.domain.LicenseStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LicenseRepository extends JpaRepository<License, UUID> {
    Optional<License> findFirstByTenantIdOrderByExpiresAtDesc(UUID tenantId);
    List<License> findAllByStatusAndExpiresAtBefore(LicenseStatus status, Instant cutoff);
    List<License> findAllByTenantId(UUID tenantId);
}
