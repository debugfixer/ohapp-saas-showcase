package ru.soat.ohapp.saas.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ReferralRequest {

    @NotEmpty
    private List<Long> employeeIds;

    /**
     * УСТАРЕВШЕЕ поле — оставлено для обратной совместимости.
     * Если examTypes не передан, сервис использует это значение как одиночный тип.
     */
    private String examType;

    /** НОВОЕ: можно передать несколько типов сразу (например ["PERIODIC_1Y", "PSYCHIATRIC"]) */
    private List<String> examTypes;

    @NotNull
    private LocalDate referralDate;

    private String clinicName;
    private String doctorName;
    private String employerName;
    private String notes;

    /**
     * "single-per-employee" — индивидуальные файлы для каждого сотрудника
     * "group" — один общий файл на всех сотрудников
     */
    private String outputMode = "single-per-employee";
}
