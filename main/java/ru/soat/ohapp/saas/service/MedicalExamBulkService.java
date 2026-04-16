package ru.soat.ohapp.saas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.dto.BulkExamUpdateRequest;
import ru.soat.ohapp.saas.model.Employee;
import ru.soat.ohapp.saas.model.MedicalExam;
import ru.soat.ohapp.saas.model.MedicalExamAudit;
import ru.soat.ohapp.saas.repo.EmployeeRepository;
import ru.soat.ohapp.saas.repo.MedicalExamAuditRepository;
import ru.soat.ohapp.saas.repo.MedicalExamRepository;
import ru.soat.ohapp.saas.repo.TenantRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MedicalExamBulkService {

    private final EmployeeRepository employeeRepository;
    private final MedicalExamRepository medicalExamRepository;
    private final MedicalExamAuditRepository auditRepository;

    @Transactional
    public int bulkUpsert(BulkExamUpdateRequest req) {
        if (req.getEmployeeIds() == null || req.getEmployeeIds().isEmpty()) return 0;
        if (req.getExamTypes() == null || req.getExamTypes().isEmpty()) return 0;

        // Загружаем всех сотрудников (без графов — тут только id нужны, остальное через exam repo)
        List<Employee> employees = employeeRepository.findAllById(req.getEmployeeIds());
        if (employees.isEmpty()) return 0;

        int changed = 0;
        String actor = "system"; // при наличии Security — взять из контекста
        for (Employee e : employees) {
            for (String examType : req.getExamTypes()) {
                Optional<MedicalExam> opt = medicalExamRepository.findByEmployeeIdAndExamType(e.getId(), examType);
                MedicalExam before = opt.map(this::cloneForAudit).orElse(null);

                MedicalExam exam = opt.orElseGet(MedicalExam::new);
                exam.setEmployee(e);
                exam.setExamType(examType);

                boolean modified = false;

                if (req.getLastExamDate() != null) {
                    exam.setLastExamDate(req.getLastExamDate());
                    modified = true;
                }
                if (req.getNextExamDate() != null) {
                    exam.setNextExamDate(req.getNextExamDate());
                    modified = true;
                }
                if (req.getReferralDate() != null) {
                    exam.setReferralDate(req.getReferralDate());
                    modified = true;
                }
                if (req.getPoints() != null) {
                    exam.setPoints(req.getPoints());
                    modified = true;
                }
                if (req.getFrequencyYears() != null) {
                    exam.setFrequencyYears(req.getFrequencyYears());
                    modified = true;
                }

                // Пересчёт next от last если надо
                if (req.isRecalcNextFromLast()) {
                    Integer freq = exam.getFrequencyYears();
                    if (exam.getLastExamDate() != null && freq != null) {
                        exam.setNextExamDate(exam.getLastExamDate().plusYears(freq));
                        modified = true;
                    }
                }

                if (modified) {
                    medicalExamRepository.save(exam);
                    changed++;

                    // Аудит
                    MedicalExamAudit audit = new MedicalExamAudit();
                    audit.setEmployeeId(e.getId());
                    audit.setExamType(examType);
                    audit.setChangedAt(LocalDateTime.now());
                    audit.setChangedBy(actor);
                    audit.setChangeType("BULK_UPDATE");
                    audit.setBeforeJson(toJson(before));
                    audit.setAfterJson(toJson(exam));
                    audit.setComment(req.getComment());
                    auditRepository.save(audit);
                }
            }
        }
        return changed;
    }

    private MedicalExam cloneForAudit(MedicalExam m) {
        if (m == null) return null;
        MedicalExam c = new MedicalExam();
        c.setId(m.getId());
        c.setExamType(m.getExamType());
        c.setLastExamDate(m.getLastExamDate());
        c.setNextExamDate(m.getNextExamDate());
        c.setReferralDate(m.getReferralDate());
        c.setPoints(m.getPoints());
        c.setFrequencyYears(m.getFrequencyYears());
        return c;
    }

    private String toJson(MedicalExam m) {
        if (m == null) return null;
        // Без внешних зависимостей: простая сериализация
        return "{"
                + "\"id\":" + (m.getId() == null ? "null" : m.getId())
                + ",\"examType\":\"" + safe(m.getExamType()) + "\""
                + ",\"lastExamDate\":\"" + (m.getLastExamDate() == null ? "" : m.getLastExamDate()) + "\""
                + ",\"nextExamDate\":\"" + (m.getNextExamDate() == null ? "" : m.getNextExamDate()) + "\""
                + ",\"referralDate\":\"" + (m.getReferralDate() == null ? "" : m.getReferralDate()) + "\""
                + ",\"points\":\"" + safe(m.getPoints()) + "\""
                + ",\"frequencyYears\":" + (m.getFrequencyYears() == null ? "null" : m.getFrequencyYears())
                + "}";
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
