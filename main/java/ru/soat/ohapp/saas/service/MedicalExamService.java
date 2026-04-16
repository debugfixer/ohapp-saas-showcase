package ru.soat.ohapp.saas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.dto.MedicalExamDto;
import ru.soat.ohapp.saas.model.Employee;
import ru.soat.ohapp.saas.model.MedicalExam;
import ru.soat.ohapp.saas.repo.EmployeeRepository;
import ru.soat.ohapp.saas.repo.MedicalExamRepository;
import ru.soat.ohapp.saas.repo.TenantRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalExamService {

    private final MedicalExamRepository medicalExamRepository;
    private final EmployeeRepository employeeRepository;

    public MedicalExamDto createMedicalExam(MedicalExamDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + dto.getEmployeeId()));

        MedicalExam exam = new MedicalExam();
        exam.setEmployee(employee);
        updateEntityFromDto(exam, dto);
        recalcNextExamDate(exam);

        return convertToDto(medicalExamRepository.save(exam));
    }

    public MedicalExamDto updateMedicalExam(Long examId, MedicalExamDto dto) {
        MedicalExam exam = medicalExamRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("MedicalExam not found with id: " + examId));

        updateEntityFromDto(exam, dto);
        recalcNextExamDate(exam);

        return convertToDto(medicalExamRepository.save(exam));
    }

    public void deleteMedicalExam(Long examId) {
        medicalExamRepository.deleteById(examId);
    }

    private void updateEntityFromDto(MedicalExam exam, MedicalExamDto dto) {
        exam.setExamType(dto.getExamType());
        exam.setPoints(dto.getPoints());
        exam.setLastExamDate(dto.getLastExamDate());
        exam.setNextExamDate(dto.getNextExamDate());
        exam.setReferralDate(dto.getReferralDate());
        exam.setFrequencyYears(frequencyFromType(dto.getExamType())); // всегда задаём
    }

    private void recalcNextExamDate(MedicalExam exam) {
        LocalDate last = exam.getLastExamDate();
        Integer years = exam.getFrequencyYears();
        if (last != null && years != null && years > 0) {
            exam.setNextExamDate(last.plusYears(years));
        }
    }

    private int frequencyFromType(String type) {
        if (type == null) return 1;
        if (type.contains("2Y")) return 2;
        if (type.startsWith("PSYCHIATRIC")) return 5; // часто так по правилам, можно вынести в настройки
        return 1;
    }

    private MedicalExamDto convertToDto(MedicalExam exam) {
        return new MedicalExamDto(
                exam.getId(),
                exam.getExamType(),
                exam.getPoints(),
                exam.getLastExamDate(),
                exam.getNextExamDate(),
                exam.getReferralDate(),
                exam.getEmployee().getId()
        );
    }
}
