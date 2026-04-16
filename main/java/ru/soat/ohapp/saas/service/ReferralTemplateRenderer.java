package ru.soat.ohapp.saas.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.springframework.stereotype.Service;
import ru.soat.ohapp.saas.dto.ReferralRequest;
import ru.soat.ohapp.saas.model.Employee;
import ru.soat.ohapp.saas.model.MedicalExam;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReferralTemplateRenderer {

    private final ReferralTemplateStorage storage;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private ReferralTemplateStorage.Key resolveKey(ReferralRequest req) {
        List<String> types = req.getExamTypes() != null && !req.getExamTypes().isEmpty()
                ? req.getExamTypes()
                : (req.getExamType() != null ? List.of(req.getExamType()) : List.of());
        boolean hasPeriodic = types.stream().anyMatch(t -> t != null && (t.startsWith("PERIODIC_1Y") || t.startsWith("PERIODIC_2Y")));
        boolean hasPsych = types.stream().anyMatch(t -> t != null && t.startsWith("PSYCHIATRIC"));
        if (hasPeriodic) return ReferralTemplateStorage.Key.PERIODIC;
        if (hasPsych)    return ReferralTemplateStorage.Key.PSYCHIATRIC;
        return ReferralTemplateStorage.Key.PERIODIC;
    }

    public byte[] tryRenderSingle(Employee e, ReferralRequest req) {
        ReferralTemplateStorage.Key key = resolveKey(req);
        if (!storage.defaultExists(key)) return null;
        try {
            byte[] bytes = storage.loadDefault(key);
            XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes));

            Map<String, String> vars = new HashMap<>();
            vars.put("${fullName}", nv(e.getFullName()));
            vars.put("${birthDate}", format(e.getBirthDate()));
            vars.put("${department}", nv(e.getDepartment()));
            vars.put("${position}", nv(e.getPosition()));

            List<String> types = req.getExamTypes() != null && !req.getExamTypes().isEmpty()
                    ? req.getExamTypes()
                    : (req.getExamType() != null ? List.of(req.getExamType()) : List.of());

            vars.put("${examTypes}", types.isEmpty() ? "—" : String.join(", ", types));
            vars.put("${points}", aggregatePoints(e, types)); // с фолбэком к requiredPoints*

            vars.put("${FIO}", nv(e.getFullName()));
            vars.put("${DEPT}", nv(e.getDepartment()));
            vars.put("${POSITION}", nv(e.getPosition()));

            replaceAll(doc, vars);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                doc.write(out);
                return out.toByteArray();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка рендера по шаблону: " + ex.getMessage(), ex);
        }
    }

    public byte[] tryRenderGroup(List<Employee> list, ReferralRequest req) { return null; }

    // ===== публичное: агрегируем пункты по типам с фолбэком =====
    public String aggregatePoints(Employee e, List<String> examTypes) {
        if (e == null || examTypes == null || examTypes.isEmpty()) return "—";
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String t : examTypes) {
            String s = pointsForExamOrFallback(e, t);
            if (!"—".equals(s)) set.add(s);
        }
        return set.isEmpty() ? "—" : String.join("; ", set);
    }

    // ===== private =====

    private String pointsForExamOrFallback(Employee e, String examType) {
        String fromExam = pointsForExam(e, examType);
        if (!"—".equals(fromExam)) return fromExam;

        // Фолбэк: если тип периодический — берём из Employee.requiredPoints*
        if (examType != null) {
            if (examType.startsWith("PERIODIC_1Y")) {
                return nv(e.getRequiredPoints1y()).isBlank() ? "—" : nv(e.getRequiredPoints1y());
            }
            if (examType.startsWith("PERIODIC_2Y")) {
                return nv(e.getRequiredPoints2y()).isBlank() ? "—" : nv(e.getRequiredPoints2y());
            }
        }
        return "—";
    }

    private String pointsForExam(Employee e, String examType) {
        if (e == null || e.getMedicalExams() == null || examType == null) return "—";
        return e.getMedicalExams().stream()
                .filter(m -> m.getExamType() != null && m.getExamType().startsWith(examType))
                .sorted((a, b) -> cmpDesc(a.getLastExamDate(), b.getLastExamDate()))
                .map(m -> nv(m.getPoints()))
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse("—");
    }

    private int cmpDesc(LocalDate a, LocalDate b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return b.compareTo(a);
    }

    private String nv(String s) { return s == null ? "" : s; }
    private String format(LocalDate d) { return d == null ? "—" : DF.format(d); }

    private void replaceAll(XWPFDocument doc, Map<String, String> vars) {
        for (XWPFParagraph p : doc.getParagraphs()) replaceInParagraphPreservingRuns(p, vars);
        for (XWPFTable t : doc.getTables()) {
            for (XWPFTableRow r : t.getRows()) {
                for (XWPFTableCell c : r.getTableCells()) {
                    for (XWPFParagraph p : c.getParagraphs()) replaceInParagraphPreservingRuns(p, vars);
                }
            }
        }
        if (doc.getHeaderList() != null) {
            for (XWPFHeader h : doc.getHeaderList()) {
                for (XWPFParagraph p : h.getParagraphs()) replaceInParagraphPreservingRuns(p, vars);
                for (XWPFTable t : h.getTables()) {
                    for (XWPFTableRow r : t.getRows()) {
                        for (XWPFTableCell c : r.getTableCells()) {
                            for (XWPFParagraph p : c.getParagraphs()) replaceInParagraphPreservingRuns(p, vars);
                        }
                    }
                }
            }
        }
        if (doc.getFooterList() != null) {
            for (XWPFFooter f : doc.getFooterList()) {
                for (XWPFParagraph p : f.getParagraphs()) replaceInParagraphPreservingRuns(p, vars);
                for (XWPFTable t : f.getTables()) {
                    for (XWPFTableRow r : t.getRows()) {
                        for (XWPFTableCell c : r.getTableCells()) {
                            for (XWPFParagraph p : c.getParagraphs()) replaceInParagraphPreservingRuns(p, vars);
                        }
                    }
                }
            }
        }
    }

    private void replaceInParagraphPreservingRuns(XWPFParagraph p, Map<String, String> vars) {
        List<XWPFRun> runs = p.getRuns();
        if (runs == null || runs.isEmpty()) return;

        // шаг 1: замена внутри каждого run без слияния
        for (XWPFRun run : runs) {
            String txt = run.getText(0);
            if (txt == null || txt.isEmpty()) continue;
            String newTxt = applyVars(txt, vars);
            if (!newTxt.equals(txt)) run.setText(newTxt, 0);
        }

        // если остались плейсхолдеры (разорваны), fallback: склеиваем и переносим базовый стиль
        String full = collectText(p);
        if (!containsAny(full, vars.keySet())) return;

        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr baseRPr = firstNonEmptyRPr(p);
        String replaced = applyVars(full, vars);
        for (int i = runs.size() - 1; i >= 0; i--) p.removeRun(i);
        XWPFRun r = p.createRun();
        if (baseRPr != null) r.getCTR().setRPr(baseRPr);
        r.setText(replaced, 0);
    }

    private String applyVars(String src, Map<String, String> vars) {
        String out = src;
        for (var e : vars.entrySet()) out = out.replace(e.getKey(), e.getValue());
        return out;
    }

    private boolean containsAny(String text, Collection<String> tokens) {
        for (String t : tokens) if (text.contains(t)) return true;
        return false;
    }

    private String collectText(XWPFParagraph p) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : p.getRuns()) {
            String part = run.getText(0);
            if (part != null) sb.append(part);
        }
        return sb.toString();
    }

    private org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr firstNonEmptyRPr(XWPFParagraph p) {
        for (XWPFRun run : p.getRuns()) {
            String t = run.getText(0);
            if (t != null && !t.isEmpty() && run.getCTR() != null) {
                return run.getCTR().getRPr();
            }
        }
        return null;
    }
}
