package ru.soat.ohapp.saas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.dto.DueExamDto;
import ru.soat.ohapp.saas.model.Employee;
import ru.soat.ohapp.saas.model.MedicalExam;
import ru.soat.ohapp.saas.repo.EmployeeRepository;
import ru.soat.ohapp.saas.security.CurrentTenant;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExamDueService {

    private final EmployeeRepository employeeRepository;

    /**
     * Ищем сотрудников, у кого ближайшая дата следующего осмотра попадает в интервал [сегодня; сегодня+withinDays]
     * Если types непустой — фильтруем только по этим типам (prefix match: startsWith)
     */
    @Transactional(readOnly = true)
    public List<DueExamDto> findDueWithinDays(int withinDays, List<String> types) {
        LocalDate now = LocalDate.now();
        LocalDate until = now.plusDays(withinDays);

        // Загружаем всех сотрудников арендатора с медосмотрами (EAGER-граф через @EntityGraph)
        var page = employeeRepository.findAllWithExams(CurrentTenant.require(), Pageable.unpaged());
        List<Employee> emps = page.getContent();

        List<DueExamDto> out = new ArrayList<>();
        for (Employee e : emps) {
            if (e.getMedicalExams() == null) continue;

            for (MedicalExam m : e.getMedicalExams()) {
                if (m.getExamType() == null) continue;
                if (types != null && !types.isEmpty() && types.stream().noneMatch(t -> m.getExamType().startsWith(t))) {
                    continue;
                }

                LocalDate next = m.getNextExamDate();
                if (next == null && m.getLastExamDate() != null && m.getFrequencyYears() != null) {
                    next = m.getLastExamDate().plusYears(m.getFrequencyYears());
                }
                if (next == null) continue;

                // Берём только тех, у кого дата впереди и не дальше заданного интервала
                if (!next.isBefore(now) && !next.isAfter(until)) {
                    long daysLeft = ChronoUnit.DAYS.between(now, next);
                    out.add(new DueExamDto(
                            e.getId(),
                            e.getFullName(),
                            e.getDepartment(),
                            e.getPosition(),
                            m.getExamType(),
                            m.getLastExamDate(),
                            next,
                            daysLeft
                    ));
                }
            }
        }

        // Сортируем по срочности (меньше дней — выше)
        out.sort(Comparator.comparingLong(DueExamDto::getDaysLeft));
        return out;
    }
}
