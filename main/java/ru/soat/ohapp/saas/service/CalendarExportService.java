// src/main/java/ru/soat/ohapp/saas/service/CalendarExportService.java
package ru.soat.ohapp.saas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.model.Employee;
import ru.soat.ohapp.saas.model.MedicalExam;
import ru.soat.ohapp.saas.repo.EmployeeRepository;   // <-- ВАЖНО: правильный импорт

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarExportService {

    // Было: private final TenantRepository.EmployeeRepository employeeRepository;
    private final EmployeeRepository employeeRepository;

    public static class Request {
        public List<Long> employeeIds;
        public List<String> types;   // ["PERIODIC_1Y","PERIODIC_2Y","PSYCHIATRIC"]
        public Integer withinDays;   // если задано — только события в ближайшие N дней
        public String titlePrefix;   // опционально: префикс к заголовку события
        public String tz;            // опционально: например "Europe/Moscow"
    }

    @Transactional(readOnly = true)
    public byte[] buildIcs(Request req) {
        List<Long> ids = Optional.ofNullable(req.employeeIds).orElse(List.of());
        Set<String> types = new HashSet<>(Optional.ofNullable(req.types)
                .orElse(List.of("PERIODIC_1Y", "PERIODIC_2Y", "PSYCHIATRIC")));
        Integer within = req.withinDays;
        ZoneId zone = ZoneId.of(Optional.ofNullable(req.tz).orElse("Europe/Moscow"));

        List<Employee> employees;
        if (ids.isEmpty()) {
            employees = employeeRepository.findAll();
        } else {
            // можно заменить на кастомный метод findAllByIdInOrderByIdAsc(ids) при желании
            employees = employeeRepository.findAllById(ids);
        }

        LocalDate now = LocalDate.now(zone);
        LocalDate limit = within != null ? now.plusDays(within) : null;

        List<Event> events = new ArrayList<>();
        for (Employee e : employees) {
            if (e.getMedicalExams() == null) continue;

            // по каждому типу берём самый "свежий" по lastExamDate осмотр, у которого есть nextExamDate
            Map<String, Optional<MedicalExam>> latestByType = e.getMedicalExams().stream()
                    .filter(m -> m.getExamType() != null && m.getNextExamDate() != null)
                    .collect(Collectors.groupingBy(
                            MedicalExam::getExamType,
                            Collectors.maxBy(Comparator.comparing(
                                    m -> Optional.ofNullable(m.getLastExamDate()).orElse(LocalDate.MIN)
                            ))
                    ));

            for (String t : types) {
                Optional<MedicalExam> opt = latestByType.entrySet().stream()
                        .filter(en -> en.getKey().startsWith(t))
                        .map(Map.Entry::getValue)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst();
                if (opt.isEmpty()) continue;

                MedicalExam m = opt.get();
                LocalDate date = m.getNextExamDate();
                if (date == null) continue;
                if (limit != null && (date.isBefore(now) || date.isAfter(limit))) continue;

                String title = (req.titlePrefix == null || req.titlePrefix.isBlank()
                        ? ""
                        : (req.titlePrefix.trim() + " — "))
                        + labelFor(t) + ": " + e.getFullName();
                String desc = buildDescription(e, m);

                events.add(new Event(date, title, desc));
            }
        }

        // Строим ICS (VEVENT целодневные)
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//OHAPP-SaaS//Medical Exams//RU\r\n")
                .append("CALSCALE:GREGORIAN\r\n")
                .append("METHOD:PUBLISH\r\n");

        DateTimeFormatter dtStamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        String nowUtc = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).format(dtStamp);

        for (Event ev : events) {
            String dtStart = ev.date.format(DateTimeFormatter.BASIC_ISO_DATE);
            String dtEnd   = ev.date.plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
            String uid = UUID.randomUUID() + "@ohapp-saas";

            sb.append("BEGIN:VEVENT\r\n")
                    .append("UID:").append(uid).append("\r\n")
                    .append("DTSTAMP:").append(nowUtc).append("\r\n")
                    .append("SUMMARY:").append(escape(ev.title)).append("\r\n")
                    .append("DESCRIPTION:").append(escape(ev.description)).append("\r\n")
                    .append("DTSTART;VALUE=DATE:").append(dtStart).append("\r\n")
                    .append("DTEND;VALUE=DATE:").append(dtEnd).append("\r\n")
                    .append("TRANSP:OPAQUE\r\n")
                    .append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String labelFor(String type) {
        return switch (type) {
            case "PERIODIC_1Y" -> "МО (1 раз в год)";
            case "PERIODIC_2Y" -> "МО (1 раз в 2 года)";
            case "PSYCHIATRIC" -> "Псих. освидетельствование";
            default -> type;
        };
    }

    private String buildDescription(Employee e, MedicalExam m) {
        StringBuilder d = new StringBuilder();
        d.append("Сотрудник: ").append(nullSafe(e.getFullName())).append("\\n");
        d.append("Подразделение: ").append(nullSafe(e.getDepartment())).append("\\n");
        d.append("Должность: ").append(nullSafe(e.getPosition())).append("\\n");
        d.append("Тип: ").append(labelFor(m.getExamType())).append("\\n");
        if (m.getLastExamDate() != null) {
            d.append("Последний осмотр: ").append(m.getLastExamDate()).append("\\n");
        }
        if (m.getPoints() != null && !m.getPoints().isBlank()) {
            d.append("Пункты: ").append(m.getPoints().replace("\n", " ")).append("\\n");
        }
        return d.toString();
    }

    private String nullSafe(String v) { return v == null ? "" : v; }

    private String escape(String s) {
        if (s == null) return "";
        // RFC 5545 escaping
        return s.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }

    private record Event(LocalDate date, String title, String description) {}
}
