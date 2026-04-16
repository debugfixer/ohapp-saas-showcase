package ru.soat.ohapp.saas.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.dto.ReferralRequest;
import ru.soat.ohapp.saas.model.Employee;
import ru.soat.ohapp.saas.repo.EmployeeRepository;
import ru.soat.ohapp.saas.security.CurrentTenant;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final EmployeeRepository employeeRepository;
    private final ReferralTemplateRenderer templateRenderer;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Transactional(readOnly = true)
    public Result generate(ReferralRequest req) {
        // Загружаем сотрудников с медосмотрами (EAGER-граф) в пределах текущего арендатора
        List<Employee> employees = employeeRepository
                .findAllByTenantIdAndIdInOrderByIdAsc(CurrentTenant.require(), req.getEmployeeIds());

        if (employees.isEmpty()) {
            throw new NoSuchElementException("Сотрудники не найдены по переданным id");
        }

        // Нормализуем список типов: если examTypes пуст — используем одиночный examType
        if ((req.getExamTypes() == null || req.getExamTypes().isEmpty()) && req.getExamType() != null) {
            req.setExamTypes(List.of(req.getExamType()));
        }

        String mode = Optional.ofNullable(req.getOutputMode()).orElse("single-per-employee");
        switch (mode) {
            case "group" -> {
                byte[] t = templateRenderer.tryRenderGroup(employees, req);
                if (t != null) {
                    return Result.ofDocx("Направление_групповое_" + DF.format(req.getReferralDate()) + ".docx", t);
                }
                byte[] doc = buildGroupDoc(employees, req);
                return Result.ofDocx("Направление_групповое_" + DF.format(req.getReferralDate()) + ".docx", doc);
            }
            case "single-per-employee" -> {
                if (employees.size() == 1) {
                    Employee e = employees.get(0);
                    byte[] t = templateRenderer.tryRenderSingle(e, req);
                    if (t != null) {
                        return Result.ofDocx(buildSingleFileName(e, req), t);
                    }
                    return Result.ofDocx(buildSingleFileName(e, req), buildSingleDoc(e, req));
                } else {
                    Map<String, byte[]> entries = new LinkedHashMap<>();
                    for (Employee e : employees) {
                        byte[] t = templateRenderer.tryRenderSingle(e, req);
                        entries.put(buildSingleFileName(e, req), t != null ? t : buildSingleDoc(e, req));
                    }
                    return Result.ofZip("Направления_" + DF.format(req.getReferralDate()) + ".zip", zipInMemory(entries));
                }
            }
            default -> throw new IllegalArgumentException("Неизвестный outputMode: " + mode);
        }
    }

    private String buildSingleFileName(Employee e, ReferralRequest req) {
        String safeFio = safe(e.getFullName());
        List<String> types = Optional.ofNullable(req.getExamTypes())
                .orElseGet(() -> req.getExamType() == null ? List.of() : List.of(req.getExamType()));
        String typesPart = types.isEmpty() ? "MO" : String.join("+", types);
        return "Направление_" + safeFio + "_" + safe(typesPart) + "_" + DF.format(req.getReferralDate()) + ".docx";
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    // ===== Fallback генерация без шаблона =====

    private byte[] buildSingleDoc(Employee e, ReferralRequest req) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            addTitle(doc, "Направление на медицинский осмотр");
            addPara(doc, "Дата направления: " + DF.format(req.getReferralDate()), false, 11);
            addPara(doc, "Сотрудник: " + nv(e.getFullName()), true, 11);
            addPara(doc, "Подразделение: " + nv(e.getDepartment()), false, 11);
            addPara(doc, "Должность: " + nv(e.getPosition()), false, 11);

            List<String> types = Optional.ofNullable(req.getExamTypes())
                    .orElseGet(() -> req.getExamType() == null ? List.of() : List.of(req.getExamType()));
            addPara(doc, "Тип(ы) осмотра: " + humanExamJoined(types), false, 11);
            addPara(doc, "Пункты/факторы: " + templateRenderer.aggregatePoints(e, types), false, 11);

            doc.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка формирования .docx: " + ex.getMessage(), ex);
        }
    }

    private byte[] buildGroupDoc(List<Employee> list, ReferralRequest req) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            addTitle(doc, "Сводное направление на медицинский осмотр");
            List<String> types = Optional.ofNullable(req.getExamTypes())
                    .orElseGet(() -> req.getExamType() == null ? List.of() : List.of(req.getExamType()));
            addPara(doc, "Тип(ы) осмотра: " + humanExamJoined(types) + " | Дата: " + DF.format(req.getReferralDate()), false, 11);

            XWPFTable table = doc.createTable(Math.max(2, list.size() + 1), 6);
            setHeader(table.getRow(0).getCell(0), "№");
            setHeader(table.getRow(0).getCell(1), "ФИО");
            setHeader(table.getRow(0).getCell(2), "Подразделение");
            setHeader(table.getRow(0).getCell(3), "Должность");
            setHeader(table.getRow(0).getCell(4), "Типы осмотров");
            setHeader(table.getRow(0).getCell(5), "Пункты/Факторы");

            AtomicInteger idx = new AtomicInteger(1);
            for (int i = 0; i < list.size(); i++) {
                Employee e = list.get(i);
                XWPFTableRow r = table.getRow(i + 1);
                setCell(r.getCell(0), String.valueOf(idx.getAndIncrement()));
                setCell(r.getCell(1), nv(e.getFullName()));
                setCell(r.getCell(2), nv(e.getDepartment()));
                setCell(r.getCell(3), nv(e.getPosition()));
                setCell(r.getCell(4), humanExamJoined(types));
                setCell(r.getCell(5), templateRenderer.aggregatePoints(e, types));
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка формирования группового .docx: " + ex.getMessage(), ex);
        }
    }

    private String humanExamJoined(List<String> types) {
        if (types == null || types.isEmpty()) return "—";
        return String.join(", ", types.stream().map(this::humanExam).toList());
    }

    private String humanExam(String code) {
        if (code == null) return "—";
        return switch (code) {
            case "PERIODIC_1Y" -> "Периодический (1 раз в год)";
            case "PERIODIC_2Y" -> "Периодический (2 раза в год)";
            case "PSYCHIATRIC" -> "Психиатрическое освидетельствование";
            default -> code;
        };
    }

    private void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setBold(true);
        run.setFontSize(14);
        run.setText(text);
    }

    private void addPara(XWPFDocument doc, String text, boolean bold, int size) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun run = p.createRun();
        run.setBold(bold);
        run.setFontSize(size);
        run.setText(text);
    }

    private void setHeader(XWPFTableCell cell, String text) {
        XWPFParagraph p = cell.getParagraphArray(0);
        if (p == null) p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setBold(true);
        r.setFontSize(10);
        r.setText(text);
    }

    private void setCell(XWPFTableCell cell, String text) {
        XWPFParagraph p = cell.getParagraphArray(0);
        if (p == null) p = cell.addParagraph();
        XWPFRun r = p.createRun();
        r.setFontSize(10);
        r.setText(Optional.ofNullable(text).orElse(""));
    }

    private String nv(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    private byte[] zipInMemory(Map<String, byte[]> entries) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> en : entries.entrySet()) {
                java.util.zip.ZipEntry ze = new java.util.zip.ZipEntry(en.getKey());
                zos.putNextEntry(ze);
                zos.write(en.getValue());
                zos.closeEntry();
            }
            zos.finish();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка формирования ZIP: " + e.getMessage(), e);
        }
    }

    public record Result(String fileName, String contentType, byte[] data) {
        public static Result ofDocx(String name, byte[] bytes) {
            return new Result(name, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes);
        }
        public static Result ofZip(String name, byte[] bytes) {
            return new Result(name, "application/zip", bytes);
        }
    }
}
