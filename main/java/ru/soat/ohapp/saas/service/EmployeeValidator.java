package ru.soat.ohapp.saas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.soat.ohapp.saas.dto.EmployeeDto;
import ru.soat.ohapp.saas.dto.MedicalExamDto;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Умная бизнес-валидация с уровнями важности.
 */
@Slf4j
@Component
public class EmployeeValidator {

    /**
     * Валидация с уровнями важности:
     * - CRITICAL: блокируют сохранение
     * - WARNING: рекомендации (не блокируют)
     * - INFO: подсказки
     */
    public ValidationResult validate(EmployeeDto dto) {
        List<ValidationMessage> messages = new ArrayList<>();

        // 1. Проверка РЕКОМЕНДУЕМЫХ полей (WARNING)
        if (dto.getBirthDate() == null) {
            messages.add(ValidationMessage.warning(
                    "Не указана дата рождения",
                    "birthDate",
                    "Добавьте дату рождения для расчёта возраста и медосмотров"
            ));
        } else {
            validateBirthDate(dto.getBirthDate(), messages);
        }

        if (dto.getHireDate() == null) {
            messages.add(ValidationMessage.warning(
                    "Не указана дата приёма на работу",
                    "hireDate",
                    "Важно для расчёта стажа и отпусков"
            ));
        } else {
            validateHireDate(dto.getHireDate(), messages);
        }

        if (dto.getDepartment() == null || dto.getDepartment().isBlank()) {
            messages.add(ValidationMessage.warning(
                    "Не указано подразделение",
                    "department",
                    "Необходимо для отчётности и фильтрации"
            ));
        }

        if (dto.getPosition() == null || dto.getPosition().isBlank()) {
            messages.add(ValidationMessage.warning(
                    "Не указана должность",
                    "position",
                    "Важно для определения видов медосмотров"
            ));
        }

        // 2. Проверка медосмотров (INFO)
        if (dto.getMedicalExams() == null || dto.getMedicalExams().isEmpty()) {
            messages.add(ValidationMessage.info(
                    "Медосмотры не добавлены",
                    "medicalExams",
                    "Добавьте медосмотры когда будут данные"
            ));
        } else {
            validateMedicalExams(dto.getMedicalExams(), messages);
        }

        // 3. Логическая связность данных
        validateLogicalConsistency(dto, messages);

        log.debug("Validation for '{}': {} messages",
                dto.getFullName(), messages.size());

        return new ValidationResult(messages);
    }

    private void validateBirthDate(LocalDate birthDate, List<ValidationMessage> messages) {
        if (birthDate.isAfter(LocalDate.now())) {
            messages.add(ValidationMessage.warning(
                    "Дата рождения в будущем",
                    "birthDate",
                    "Проверьте правильность даты"
            ));
            return;
        }

        int age = Period.between(birthDate, LocalDate.now()).getYears();

        if (age < 14) {
            messages.add(ValidationMessage.warning(
                    "Возраст меньше 14 лет",
                    "birthDate",
                    "Проверьте правильность даты рождения"
            ));
        } else if (age < 18) {
            messages.add(ValidationMessage.info(
                    "Несовершеннолетний сотрудник",
                    "birthDate",
                    "Убедитесь что есть разрешение на работу"
            ));
        }

        if (age > 80) {
            messages.add(ValidationMessage.info(
                    "Возраст больше 80 лет",
                    "birthDate",
                    "Проверьте правильность даты"
            ));
        }
    }

    private void validateHireDate(LocalDate hireDate, List<ValidationMessage> messages) {
        if (hireDate.isAfter(LocalDate.now())) {
            messages.add(ValidationMessage.info(
                    "Дата приёма в будущем",
                    "hireDate",
                    "Сотрудник ещё не приступил к работе"
            ));
        }

        if (hireDate.isBefore(LocalDate.now().minusYears(50))) {
            messages.add(ValidationMessage.warning(
                    "Дата приёма более 50 лет назад",
                    "hireDate",
                    "Проверьте правильность даты"
            ));
        }
    }

    private void validateMedicalExams(List<MedicalExamDto> exams, List<ValidationMessage> messages) {
        for (MedicalExamDto exam : exams) {
            String field = "medicalExams[" + exam.getExamType() + "]";

            if (!exam.hasData()) {
                messages.add(ValidationMessage.info(
                        "Медосмотр '" + exam.getExamType() + "' без данных",
                        field,
                        "Добавьте даты и пункты приказа когда будут доступны"
                ));
                continue;
            }

            if (exam.getLastExamDate() != null && exam.getNextExamDate() != null) {
                if (exam.getNextExamDate().isBefore(exam.getLastExamDate())) {
                    messages.add(ValidationMessage.warning(
                            "Дата следующего осмотра раньше последнего",
                            field,
                            "Проверьте правильность дат"
                    ));
                }
            }

            if (exam.getLastExamDate() == null && exam.getNextExamDate() != null) {
                messages.add(ValidationMessage.info(
                        "Не указана дата последнего осмотра",
                        field,
                        "Рекомендуется заполнить для полноты данных"
                ));
            }
        }
    }

    private void validateLogicalConsistency(EmployeeDto dto, List<ValidationMessage> messages) {
        if (dto.getBirthDate() != null && dto.getHireDate() != null) {
            int ageAtHire = Period.between(dto.getBirthDate(), dto.getHireDate()).getYears();

            if (ageAtHire < 0) {
                messages.add(ValidationMessage.warning(
                        "Дата приёма раньше даты рождения",
                        "hireDate",
                        "Проверьте правильность дат"
                ));
            } else if (ageAtHire < 14) {
                messages.add(ValidationMessage.warning(
                        "Возраст при приёме меньше 14 лет",
                        "hireDate",
                        "Проверьте правильность дат"
                ));
            }
        }
    }

    // ════════════════════════════════════════════
    // ВЛОЖЕННЫЕ КЛАССЫ
    // ════════════════════════════════════════════

    /**
     * Результат валидации.
     */
    public static class ValidationResult {
        private final List<ValidationMessage> messages;

        public ValidationResult(List<ValidationMessage> messages) {
            this.messages = messages;
        }

        public List<ValidationMessage> getMessages() {
            return messages;
        }

        public List<ValidationMessage> getWarnings() {
            return messages.stream()
                    .filter(m -> m.level == Level.WARNING)
                    .toList();
        }

        public List<ValidationMessage> getInfos() {
            return messages.stream()
                    .filter(m -> m.level == Level.INFO)
                    .toList();
        }

        public boolean hasWarnings() {
            return messages.stream().anyMatch(m -> m.level == Level.WARNING);
        }

        public boolean hasAnyMessages() {
            return !messages.isEmpty();
        }
    }

    /**
     * Сообщение валидации с уровнем важности.
     */
    public static class ValidationMessage {
        private final Level level;
        private final String message;
        private final String field;
        private final String suggestion;

        private ValidationMessage(Level level, String message, String field, String suggestion) {
            this.level = level;
            this.message = message;
            this.field = field;
            this.suggestion = suggestion;
        }

        public static ValidationMessage warning(String message, String field, String suggestion) {
            return new ValidationMessage(Level.WARNING, message, field, suggestion);
        }

        public static ValidationMessage info(String message, String field, String suggestion) {
            return new ValidationMessage(Level.INFO, message, field, suggestion);
        }

        public Level getLevel() { return level; }
        public String getMessage() { return message; }
        public String getField() { return field; }
        public String getSuggestion() { return suggestion; }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s (%s)", level, field, message, suggestion);
        }
    }

    public enum Level {
        WARNING,  // ⚠️ Рекомендуется исправить
        INFO      // 💡 Необязательная подсказка
    }
}
