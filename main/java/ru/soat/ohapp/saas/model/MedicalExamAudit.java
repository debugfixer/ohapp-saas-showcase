package ru.soat.ohapp.saas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Простой аудит изменений по медосмотрам: кто, когда, что менял.
 * Для простоты храним before/after как JSON-подобную строку.
 */
@Entity
@Table(name = "medical_exam_audit")
@Getter
@Setter
public class MedicalExamAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    @Column(length = 64)
    private String examType;

    private LocalDateTime changedAt;

    @Column(length = 128)
    private String changedBy; // можно подставлять из SecurityContext; сейчас — "system"

    @Column(length = 64)
    private String changeType; // BULK_UPDATE, IMPORT, EDIT, etc.

    @Column(columnDefinition = "text")
    private String beforeJson;

    @Column(columnDefinition = "text")
    private String afterJson;

    @Column(columnDefinition = "text")
    private String comment;
}