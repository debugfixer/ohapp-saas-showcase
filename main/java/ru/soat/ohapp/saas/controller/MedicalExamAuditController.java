package ru.soat.ohapp.saas.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.model.MedicalExamAudit;
import ru.soat.ohapp.saas.repo.MedicalExamAuditRepository;
import ru.soat.ohapp.saas.repo.TenantRepository;

import java.util.List;

@RestController
@RequestMapping("/api/medical-exams/audit")
@RequiredArgsConstructor
public class MedicalExamAuditController {

    private final MedicalExamAuditRepository auditRepository;

    /**
     * Простой просмотр аудита: по сотруднику (обязательно) и, опционально, по типу осмотра.
     * GET /api/medical-exams/audit?employeeId=1
     * GET /api/medical-exams/audit?employeeId=1&examType=PERIODIC_1Y
     */
    @GetMapping
    public ResponseEntity<List<MedicalExamAudit>> list(
            @RequestParam Long employeeId,
            @RequestParam(required = false) String examType
    ) {
        if (examType == null || examType.isBlank()) {
            return ResponseEntity.ok(
                    auditRepository.findAll().stream()
                            .filter(a -> employeeId.equals(a.getEmployeeId()))
                            .sorted((a,b) -> b.getChangedAt().compareTo(a.getChangedAt()))
                            .toList()
            );
        }
        return ResponseEntity.ok(
                auditRepository.findAll().stream()
                        .filter(a -> employeeId.equals(a.getEmployeeId()) &&
                                examType.equals(a.getExamType()))
                        .sorted((a,b) -> b.getChangedAt().compareTo(a.getChangedAt()))
                        .toList()
        );
    }
}
