package ru.soat.ohapp.saas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class DueExamDto {
    private Long employeeId;
    private String fullName;
    private String department;
    private String position;
    private String examType;
    private LocalDate lastExamDate;
    private LocalDate nextExamDate;
    private long daysLeft;
}
