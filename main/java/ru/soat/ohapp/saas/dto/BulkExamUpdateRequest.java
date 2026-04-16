package ru.soat.ohapp.saas.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Пакетное обновление (upsert) записей медосмотров по списку сотрудников и типов.
 * Заполняй только те поля, которые нужно изменить (остальные остаются без изменения).
 */
@Data
public class BulkExamUpdateRequest {

    /** Кому применяем (обязателен хотя бы один) */
    @NotEmpty
    private List<Long> employeeIds;

    /** Какие типы изменяем (пример: PERIODIC_1Y, PERIODIC_2Y, PSYCHIATRIC) */
    @NotEmpty
    private List<String> examTypes;

    /** Новые значения (любой из этих полей может быть null -> не меняем) */
    private LocalDate lastExamDate;
    private LocalDate nextExamDate;
    private LocalDate referralDate;
    private String points;
    private Integer frequencyYears; // если нужно изменить частоту

    /**
     * Если true и lastExamDate/ frequencyYears заданы/присутствуют у записи —
     * пересчитать nextExamDate = lastExamDate + frequencyYears.
     */
    private boolean recalcNextFromLast = true;

    /** Произвольный комментарий, попадёт в аудит */
    private String comment;
}
