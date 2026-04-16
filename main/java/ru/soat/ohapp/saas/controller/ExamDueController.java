package ru.soat.ohapp.saas.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.dto.DueExamDto;
import ru.soat.ohapp.saas.service.ExamDueService;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class ExamDueController {

    private final ExamDueService examDueService;

    /**
     * Пример: GET /api/employees/due?withinDays=60&types=PERIODIC_1Y&types=PERIODIC_2Y
     * Вернёт список сотрудников/осмотров, которым скоро надо проходить МО.
     */
    @GetMapping("/due")
    public List<DueExamDto> findDue(
            @RequestParam(defaultValue = "60") int withinDays,
            @RequestParam(required = false) List<String> types
    ) {
        return examDueService.findDueWithinDays(withinDays, types);
    }
}
