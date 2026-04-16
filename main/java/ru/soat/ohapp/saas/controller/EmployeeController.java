package ru.soat.ohapp.saas.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.dto.EmployeeDto;
import ru.soat.ohapp.saas.service.EmployeeService;
import ru.soat.ohapp.saas.service.EmployeeValidator;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Validated
public class EmployeeController {

    private final EmployeeService employeeService;

    // ════════════════════════════════════════════════════════════════
    // СУЩЕСТВУЮЩИЕ МЕТОДЫ (не изменены)
    // ════════════════════════════════════════════════════════════════

    /**
     * Получить всех сотрудников с медосмотрами (для таблицы/дашборда).
     */
    @GetMapping("/with-exams")
    public List<EmployeeDto> getAllEmployeesWithExams() {
        return employeeService.getAllEmployees();
    }

    /**
     * Получить всех сотрудников.
     */
    @GetMapping
    public List<EmployeeDto> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    /**
     * Получить сотрудника по ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployee(@PathVariable Long id) {
        Optional<EmployeeDto> dto = employeeService.getEmployeeById(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Создать нового сотрудника (стандартный endpoint).
     */
    @PostMapping
    public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody EmployeeDto employeeDto) {
        EmployeeDto created = employeeService.create(employeeDto);
        return ResponseEntity.created(URI.create("/api/employees/" + created.getId())).body(created);
    }

    /**
     * Обновить сотрудника (стандартный endpoint).
     */
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDto> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeDto employeeDto
    ) {
        try {
            return ResponseEntity.ok(employeeService.update(id, employeeDto));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Удалить сотрудника.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════
    // ✨ НОВЫЕ ENDPOINTS С VALIDATION WARNINGS
    // ════════════════════════════════════════════════════════════════

    /**
     * Создать сотрудника с validation warnings.
     * Сохраняет даже при неполных данных, но возвращает рекомендации.
     *
     * @param employeeDto данные сотрудника
     * @return Response с employee, completenessPercent, warnings, info
     */
    @PostMapping("/with-validation")
    public ResponseEntity<Map<String, Object>> createWithValidation(
            @Valid @RequestBody EmployeeDto employeeDto
    ) {
        EmployeeService.EmployeeCreationResult result =
                employeeService.createWithValidation(employeeDto);

        EmployeeValidator.ValidationResult validation = result.getValidationResult();

        Map<String, Object> response = new HashMap<>();
        response.put("employee", result.getEmployee());
        response.put("completenessPercent", result.getEmployee().getCompletenessPercent());
        response.put("isComplete", result.getEmployee().isComplete());

        // Группируем сообщения по уровням
        response.put("warnings", validation.getWarnings().stream()
                .map(msg -> Map.of(
                        "field", msg.getField(),
                        "message", msg.getMessage(),
                        "suggestion", msg.getSuggestion()
                ))
                .toList());

        response.put("info", validation.getInfos().stream()
                .map(msg -> Map.of(
                        "field", msg.getField(),
                        "message", msg.getMessage(),
                        "suggestion", msg.getSuggestion()
                ))
                .toList());

        return ResponseEntity
                .created(URI.create("/api/employees/" + result.getEmployee().getId()))
                .body(response);
    }

    /**
     * Обновить сотрудника с validation warnings.
     *
     * @param id ID сотрудника
     * @param employeeDto обновлённые данные
     * @return Response с employee, completenessPercent, warnings, info
     */
    @PutMapping("/{id}/with-validation")
    public ResponseEntity<Map<String, Object>> updateWithValidation(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeDto employeeDto
    ) {
        EmployeeService.EmployeeCreationResult result =
                employeeService.updateWithValidation(id, employeeDto);

        EmployeeValidator.ValidationResult validation = result.getValidationResult();

        Map<String, Object> response = new HashMap<>();
        response.put("employee", result.getEmployee());
        response.put("completenessPercent", result.getEmployee().getCompletenessPercent());
        response.put("isComplete", result.getEmployee().isComplete());

        response.put("warnings", validation.getWarnings().stream()
                .map(msg -> Map.of(
                        "field", msg.getField(),
                        "message", msg.getMessage(),
                        "suggestion", msg.getSuggestion()
                ))
                .toList());

        response.put("info", validation.getInfos().stream()
                .map(msg -> Map.of(
                        "field", msg.getField(),
                        "message", msg.getMessage(),
                        "suggestion", msg.getSuggestion()
                ))
                .toList());

        return ResponseEntity.ok(response);
    }
}
