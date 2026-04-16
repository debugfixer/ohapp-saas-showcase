package ru.soat.ohapp.saas.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.model.MedicalExamAudit;

@Repository
@Transactional(readOnly = true)
public interface MedicalExamAuditRepository extends JpaRepository<MedicalExamAudit, Long> {
}
