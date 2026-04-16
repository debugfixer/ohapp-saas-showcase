package ru.soat.ohapp.saas.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.model.Employee;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /** Уникальность табельного номера в пределах арендатора. */
    Optional<Employee> findByTenantIdAndPersonnelNumber(UUID tenantId, String personnelNumber);

    /** Строгая выборка по id в пределах арендатора. */
    Optional<Employee> findByIdAndTenantId(Long id, UUID tenantId);

    /** Списки сотрудников арендатора. */
    List<Employee> findAllByTenantId(UUID tenantId);

    /** Списки по id в пределах арендатора (для массовых операций/генераций). */
    List<Employee> findAllByTenantIdAndIdIn(UUID tenantId, Collection<Long> ids);

    /** EAGER-граф для выборки по списку id (генерация направлений) в пределах арендатора. */
    @EntityGraph(attributePaths = {"medicalExams"})
    List<Employee> findAllByTenantIdAndIdInOrderByIdAsc(UUID tenantId, Collection<Long> ids);

    /** Пагинация со сразу подгруженными медосмотрами (для таблицы/дашборда) в пределах арендатора. */
    @Query("select e from Employee e where e.tenantId = :tenantId")
    @EntityGraph(attributePaths = {"medicalExams"})
    Page<Employee> findAllWithExams(UUID tenantId, Pageable pageable);
}
