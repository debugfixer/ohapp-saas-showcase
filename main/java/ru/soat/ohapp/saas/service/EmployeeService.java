package ru.soat.ohapp.saas.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.dto.EmployeeDto;
import ru.soat.ohapp.saas.dto.MedicalExamDto;
import ru.soat.ohapp.saas.model.Employee;
import ru.soat.ohapp.saas.repo.EmployeeRepository;
import ru.soat.ohapp.saas.security.CurrentTenant;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeValidator employeeValidator;

    // ════════════════════════════════════════════════════════════════
    // СУЩЕСТВУЮЩИЕ МЕТОДЫ (не изменены)
    // ════════════════════════════════════════════════════════════════

    public EmployeeDto create(EmployeeDto dto) {
        UUID tenantId = CurrentTenant.require();

        // уникальность табельного номера в рамках арендатора
        employeeRepository.findByTenantIdAndPersonnelNumber(tenantId, dto.getPersonnelNumber())
                .ifPresent(e -> {
                    throw new IllegalArgumentException("Табельный номер уже используется");
                });

        Employee e = new Employee();
        e.setTenantId(tenantId);
        updateEmployeeFromDto(e, dto);
        e = employeeRepository.save(e);

        return convertToDto(e);
    }

    public EmployeeDto update(Long id, EmployeeDto dto) {
        UUID tenantId = CurrentTenant.require();

        Employee e = employeeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Сотрудник не найден: id=" + id));

        // проверка уникальности табельного номера (если меняется)
        if (!Objects.equals(e.getPersonnelNumber(), dto.getPersonnelNumber())) {
            employeeRepository.findByTenantIdAndPersonnelNumber(tenantId, dto.getPersonnelNumber())
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("Табельный номер уже используется другим сотрудником");
                    });
        }

        updateEmployeeFromDto(e, dto);
        return convertToDto(e);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeDto> getPageWithExams(Pageable pageable) {
        UUID tenantId = CurrentTenant.require();
        return employeeRepository.findAllWithExams(tenantId, pageable).map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> getAllEmployees() {
        UUID tenantId = CurrentTenant.require();
        return employeeRepository.findAllByTenantId(tenantId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<EmployeeDto> getEmployeeById(Long id) {
        UUID tenantId = CurrentTenant.require();
        return employeeRepository.findByIdAndTenantId(id, tenantId).map(this::convertToDto);
    }

    public void deleteEmployee(Long id) {
        UUID tenantId = CurrentTenant.require();
        Employee e = employeeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Сотрудник не найден: id=" + id));
        employeeRepository.delete(e);
    }

    // ════════════════════════════════════════════════════════════════
    // ✨ НОВЫЕ МЕТОДЫ: WITH VALIDATION
    // ════════════════════════════════════════════════════════════════

    /**
     * Создать сотрудника с validation warnings.
     * Сохраняет даже при неполных данных, но возвращает рекомендации.
     */
    public EmployeeCreationResult createWithValidation(EmployeeDto dto) {
        UUID tenantId = CurrentTenant.require();

        // Проверяем уникальность табельного номера
        employeeRepository.findByTenantIdAndPersonnelNumber(tenantId, dto.getPersonnelNumber())
                .ifPresent(e -> {
                    throw new IllegalArgumentException("Табельный номер уже используется");
                });

        // Валидация
        EmployeeValidator.ValidationResult validation = employeeValidator.validate(dto);

        // Создаём сотрудника (даже если есть warnings)
        Employee e = new Employee();
        e.setTenantId(tenantId);
        updateEmployeeFromDto(e, dto);
        e = employeeRepository.save(e);

        EmployeeDto savedDto = convertToDto(e);

        return new EmployeeCreationResult(savedDto, validation);
    }

    /**
     * Обновить сотрудника с validation warnings.
     */
    public EmployeeCreationResult updateWithValidation(Long id, EmployeeDto dto) {
        UUID tenantId = CurrentTenant.require();

        Employee e = employeeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Сотрудник не найден: id=" + id));

        // проверка уникальности табельного номера (если меняется)
        if (!Objects.equals(e.getPersonnelNumber(), dto.getPersonnelNumber())) {
            employeeRepository.findByTenantIdAndPersonnelNumber(tenantId, dto.getPersonnelNumber())
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("Табельный номер уже используется другим сотрудником");
                    });
        }

        // Валидация
        EmployeeValidator.ValidationResult validation = employeeValidator.validate(dto);

        // Обновляем (даже если есть warnings)
        updateEmployeeFromDto(e, dto);
        EmployeeDto savedDto = convertToDto(e);

        return new EmployeeCreationResult(savedDto, validation);
    }

    // ════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════

    private void updateEmployeeFromDto(Employee employee, EmployeeDto dto) {
        employee.setPersonnelNumber(dto.getPersonnelNumber());
        employee.setFullName(dto.getFullName());
        employee.setDepartment(dto.getDepartment());
        employee.setPosition(dto.getPosition());
        employee.setHireDate(dto.getHireDate());
        employee.setBirthDate(dto.getBirthDate());
        employee.setComment(dto.getComment());
        employee.setRequiredPoints1y(dto.getRequiredPoints1y());
        employee.setRequiredPoints2y(dto.getRequiredPoints2y());
    }

    public EmployeeDto convertToDto(Employee employee) {
        List<MedicalExamDto> examDtos = employee.getMedicalExams().stream()
                .map(exam -> new MedicalExamDto(
                        exam.getId(),
                        exam.getExamType(),
                        exam.getPoints(),  // ← ИСПРАВЛЕНО: теперь String
                        exam.getLastExamDate(),
                        exam.getNextExamDate(),
                        exam.getReferralDate(),
                        employee.getId()))
                .collect(Collectors.toList());

        return new EmployeeDto(
                employee.getId(),
                employee.getPersonnelNumber(),
                employee.getFullName(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getHireDate(),
                employee.getBirthDate(),
                employee.getComment(),
                employee.getRequiredPoints1y(),
                employee.getRequiredPoints2y(),
                examDtos
        );
    }

    // ════════════════════════════════════════════════════════════════
    // ✨ НОВЫЙ ВНУТРЕННИЙ КЛАСС: Result wrapper
    // ════════════════════════════════════════════════════════════════

    /**
     * Обёртка результата создания/обновления сотрудника с validation.
     */
    @Getter
    public static class EmployeeCreationResult {
        private final EmployeeDto employee;
        private final EmployeeValidator.ValidationResult validationResult;

        public EmployeeCreationResult(EmployeeDto employee, EmployeeValidator.ValidationResult validationResult) {
            this.employee = employee;
            this.validationResult = validationResult;
        }

        public boolean hasWarnings() {
            return !validationResult.getWarnings().isEmpty();
        }
    }
}
