package ru.soat.ohapp.saas.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Не отправляем null поля
public class EmployeeDto {

    private Long id;

    // ════════════════════════════════════════════
    // КРИТИЧНЫЕ ПОЛЯ - АБСОЛЮТНЫЙ МИНИМУМ
    // ════════════════════════════════════════════

    @NotBlank(message = "Табельный номер обязателен")
    @Size(min = 1, max = 50, message = "Табельный номер от 1 до 50 символов")
    private String personnelNumber;

    @NotBlank(message = "ФИО обязательно")
    @Size(min = 2, max = 200, message = "ФИО от 2 до 200 символов")
    private String fullName;

    // ════════════════════════════════════════════
    // ВСЁ ОСТАЛЬНОЕ - ОПЦИОНАЛЬНО
    // Только size limits для безопасности
    // ════════════════════════════════════════════

    @Size(max = 100, message = "Подразделение не должно превышать 100 символов")
    private String department;

    @Size(max = 100, message = "Должность не должна превышать 100 символов")
    private String position;

    // Даты могут быть null - заполнят позже
    private LocalDate hireDate;
    private LocalDate birthDate;

    @Size(max = 500, message = "Комментарий не должен превышать 500 символов")
    private String comment;

    @Size(max = 100, message = "Поле не должно превышать 100 символов")
    private String requiredPoints1y;

    @Size(max = 100, message = "Поле не должно превышать 100 символов")
    private String requiredPoints2y;

    // Медосмотры добавятся позже
    private List<MedicalExamDto> medicalExams;

    // ════════════════════════════════════════════
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ПРОВЕРКИ ПОЛНОТЫ
    // ════════════════════════════════════════════

    /**
     * Проверяет полноту критичных данных.
     * @return true если заполнены рекомендуемые поля
     */
    public boolean isComplete() {
        return birthDate != null
                && department != null && !department.isBlank()
                && position != null && !position.isBlank()
                && hireDate != null;
    }

    /**
     * Возвращает процент заполненности профиля (для UI).
     */
    public int getCompletenessPercent() {
        int total = 6; // критичные поля
        int filled = 2; // personnelNumber + fullName всегда заполнены

        if (birthDate != null) filled++;
        if (department != null && !department.isBlank()) filled++;
        if (position != null && !position.isBlank()) filled++;
        if (hireDate != null) filled++;

        return (int) ((filled / (double) total) * 100);
    }
}
