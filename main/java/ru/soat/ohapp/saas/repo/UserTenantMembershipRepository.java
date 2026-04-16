package ru.soat.ohapp.saas.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.model.UserTenantMembership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTenantMembershipRepository extends JpaRepository<UserTenantMembership, UUID> {

    // Корректные методы по вложенным свойствам
    Optional<UserTenantMembership> findByUser_IdAndTenant_Id(UUID userId, UUID tenantId);

    boolean existsByUser_IdAndTenant_Id(UUID userId, UUID tenantId);

    List<UserTenantMembership> findAllByUser_Id(UUID userId);

    // Обёртки для совместимости с уже используемыми именами в коде
    @Transactional(readOnly = true)
    default Optional<UserTenantMembership> findByUserIdAndTenantId(UUID userId, UUID tenantId) {
        return findByUser_IdAndTenant_Id(userId, tenantId);
    }

    @Transactional(readOnly = true)
    default boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId) {
        return existsByUser_IdAndTenant_Id(userId, tenantId);
    }

    @Transactional(readOnly = true)
    default List<UserTenantMembership> findAllByUserId(UUID userId) {
        return findAllByUser_Id(userId);
    }
}
