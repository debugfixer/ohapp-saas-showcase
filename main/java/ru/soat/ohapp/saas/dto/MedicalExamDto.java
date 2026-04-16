package ru.soat.ohapp.saas.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;

/**
 * DTO для медицинского осмотра.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MedicalExamDto {

    private Long id;

    // МИНИМУМ - только тип и связь
    @NotBlank(message = "Тип медосмотра обязателен")
    @Size(max = 50, message = "Тип не должен превышать 50 символов")
    private String examType;

    // ВСЁ ОСТАЛЬНОЕ ОПЦИОНАЛЬНО
    @Size(max = 200, message = "Пункты не должны превышать 200 символов")
    private String points;

    private LocalDate lastExamDate;

    private LocalDate nextExamDate;

    private LocalDate referralDate;

    @NotNull(message = "ID сотрудника обязателен")
    @Positive(message = "ID должен быть положительным")
    private Long employeeId;

    /**
     * Проверяет что медосмотр имеет хоть какие-то данные кроме типа.
     */
    public boolean hasData() {
        return lastExamDate != null
                || nextExamDate != null
                || (points != null && !points.isBlank());
    }
}
