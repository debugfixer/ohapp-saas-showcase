package ru.soat.ohapp.saas.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.model.MedicalExam;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface MedicalExamRepository extends JpaRepository<MedicalExam, Long> {

    @Query("select m from MedicalExam m where m.employee.id = :employeeId and m.examType = :examType")
    Optional<MedicalExam> findByEmployeeIdAndExamType(Long employeeId, String examType);

    @Query("select m from MedicalExam m where m.employee.id = :employeeId")
    List<MedicalExam> findAllByEmployeeId(Long employeeId);
}
