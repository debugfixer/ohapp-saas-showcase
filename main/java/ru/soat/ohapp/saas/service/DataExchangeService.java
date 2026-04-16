package ru.soat.ohapp.saas.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.soat.ohapp.saas.model.Employee;
import ru.soat.ohapp.saas.model.MedicalExam;
import ru.soat.ohapp.saas.repo.EmployeeRepository;
import ru.soat.ohapp.saas.repo.MedicalExamRepository;
import ru.soat.ohapp.saas.security.CurrentTenant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataExchangeService {

    private final EmployeeRepository employeeRepository;
    private final MedicalExamRepository medicalExamRepository;

    @Transactional
    public void importEmployeesFromExcel(MultipartFile file) throws IOException {
        UUID tenantId = CurrentTenant.require();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // пропускаем заголовок
                if (row.getCell(0) == null || getStringValue(row.getCell(0)).isEmpty()) continue;

                String personnelNumber = getStringValue(row.getCell(0)).trim();

                Employee employee = employeeRepository
                        .findByTenantIdAndPersonnelNumber(tenantId, personnelNumber)
                        .orElseGet(() -> {
                            Employee e = new Employee();
                            e.setTenantId(tenantId); // ключ: не допускаем NULL
                            return e;
                        });

                // базовые поля
                employee.setPersonnelNumber(personnelNumber);
                employee.setFullName(getStringValue(row.getCell(1)).trim());
                employee.setPosition(getStringValue(row.getCell(2)).trim());
                employee.setDepartment(getStringValue(row.getCell(3)).trim());
                employee.setHireDate(getDateValue(row.getCell(4)));
                employee.setBirthDate(getDateValue(row.getCell(5)));
                employee.setRequiredPoints1y(getStringValue(row.getCell(6)).trim()); // “пункты МО (1г)”
                employee.setRequiredPoints2y(getStringValue(row.getCell(7)).trim()); // “пункты МО (2г)”
                employee.setComment(getStringValue(row.getCell(14)).trim());

                Employee saved = employeeRepository.save(employee);

                // апсерт записей медосмотра по типам:
                // Психиатрическое: дата (8), пункты (9), дата направления (10)
                upsertExam(saved, "PSYCHIATRIC",
                        getDateValue(row.getCell(8)),
                        getStringValue(row.getCell(9)).trim(),
                        getDateValue(row.getCell(10)));

                // Периодический 1 раз в год: дата (11), пункты берём из employee.requiredPoints1y
                upsertExam(saved, "PERIODIC_1Y",
                        getDateValue(row.getCell(11)),
                        emptyToNull(saved.getRequiredPoints1y()),
                        null);

                // Периодический 2 раза в год: дата (12), пункты из employee.requiredPoints2y
                upsertExam(saved, "PERIODIC_2Y",
                        getDateValue(row.getCell(12)),
                        emptyToNull(saved.getRequiredPoints2y()),
                        null);

                // Кардиолог (если используете): дата (13) — как отдельный тип, при желании
                // upsertExam(saved, "CARDIO", getDateValue(row.getCell(13)), null, null);
            }
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportEmployeesToExcel() throws IOException {
        UUID tenantId = CurrentTenant.require();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Сотрудники");

            String[] headers = {
                    "Табельный номер", "ФИО", "Должность", "Подразделение", "Дата приема", "Дата рождения",
                    "Пункты МО (раз в год)", "Пункты МО (два раза в год)",
                    "Дата псих. освидетельствования", "Пункты псих. освидетельствования", "Дата направления на псих. осв.",
                    "Дата МО (раз в год)", "Дата МО (два раза в год)",
                    "Дата осмотра кардиологом",
                    "Комментарий"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            List<Employee> employees = employeeRepository.findAllByTenantId(tenantId);
            int rowNum = 1;
            for (Employee emp : employees) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(nv(emp.getPersonnelNumber()));
                row.createCell(1).setCellValue(nv(emp.getFullName()));
                row.createCell(2).setCellValue(nv(emp.getPosition()));
                row.createCell(3).setCellValue(nv(emp.getDepartment()));
                setDateCell(row.createCell(4), emp.getHireDate());
                setDateCell(row.createCell(5), emp.getBirthDate());
                row.createCell(6).setCellValue(nv(emp.getRequiredPoints1y()));
                row.createCell(7).setCellValue(nv(emp.getRequiredPoints2y()));

                setDateCell(row.createCell(8),  findLatestExamDate(emp.getId(), "PSYCHIATRIC"));
                row.createCell(9).setCellValue(findLatestExamPoints(emp.getId(), "PSYCHIATRIC"));
                setDateCell(row.createCell(10), findLatestExamReferralDate(emp.getId(), "PSYCHIATRIC"));

                setDateCell(row.createCell(11), findLatestExamDate(emp.getId(), "PERIODIC_1Y"));
                setDateCell(row.createCell(12), findLatestExamDate(emp.getId(), "PERIODIC_2Y"));

                setDateCell(row.createCell(13), findLatestExamDate(emp.getId(), "CARDIO"));
                row.createCell(14).setCellValue(nv(emp.getComment()));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ===== helpers =====

    private void upsertExam(Employee employee, String examType, LocalDate examDate, String points, LocalDate referralDate) {
        // если вообще ничего нет — не создаём “пустышки”
        if (examDate == null && (points == null || points.isBlank()) && referralDate == null) return;

        MedicalExam exam = medicalExamRepository
                .findByEmployeeIdAndExamType(employee.getId(), examType)
                .orElseGet(MedicalExam::new);

        exam.setEmployee(employee);
        exam.setExamType(examType);

        if (examDate != null) exam.setLastExamDate(examDate);
        if (points != null && !points.isBlank()) exam.setPoints(points);
        if (referralDate != null) exam.setReferralDate(referralDate);

        // частота
        int freq = examType.contains("2Y") ? 2 : (examType.startsWith("PSYCHIATRIC") ? 5 : 1);
        exam.setFrequencyYears(freq);
        if (exam.getLastExamDate() != null) {
            exam.setNextExamDate(exam.getLastExamDate().plusYears(freq));
        }

        medicalExamRepository.save(exam);
    }

    private LocalDate findLatestExamDate(Long employeeId, String prefix) {
        return medicalExamRepository.findAllByEmployeeId(employeeId).stream()
                .filter(e -> e.getExamType() != null && e.getExamType().startsWith(prefix) && e.getLastExamDate() != null)
                .map(MedicalExam::getLastExamDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private String findLatestExamPoints(Long employeeId, String prefix) {
        return medicalExamRepository.findAllByEmployeeId(employeeId).stream()
                .filter(e -> e.getExamType() != null && e.getExamType().startsWith(prefix) && e.getPoints() != null && !e.getPoints().isBlank())
                .map(MedicalExam::getPoints)
                .findFirst()
                .orElse("");
    }

    private LocalDate findLatestExamReferralDate(Long employeeId, String prefix) {
        return medicalExamRepository.findAllByEmployeeId(employeeId).stream()
                .filter(e -> e.getExamType() != null && e.getExamType().startsWith(prefix) && e.getReferralDate() != null)
                .map(MedicalExam::getReferralDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) yield "";
                double d = cell.getNumericCellValue();
                long l = (long) d;
                yield (d == l) ? String.valueOf(l) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private LocalDate getDateValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date javaDate = cell.getDateCellValue();
            return javaDate != null ? javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        }
        return null;
    }

    private void setDateCell(Cell cell, LocalDate date) {
        if (cell == null) return;
        if (date != null) {
            cell.setCellValue(date);
            CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
            CreationHelper createHelper = cell.getSheet().getWorkbook().getCreationHelper();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.MM.yyyy"));
            cell.setCellStyle(cellStyle);
        } else {
            cell.setBlank();
        }
    }

    private String nv(String s) { return (s == null) ? "" : s; }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
