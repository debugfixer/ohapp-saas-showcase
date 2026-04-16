// src/main/java/ru/soat/ohapp/saas/model/MedicalExam.java

package ru.soat.ohapp.saas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medical_exams",
        indexes = {
                @Index(name = "idx_medical_exams_tenant", columnList = "tenant_id"),
                @Index(name = "idx_medical_exams_due", columnList = "tenant_id,next_exam_date")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** RLS/мульти-тенант: внешний ключ на tenants.id */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Связь с сотрудником (FK employee_id) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Тип осмотра */
    @NotNull
    @Column(name = "exam_type", nullable = false)
    private String examType;

    /** Фактическая дата осмотра (раньше в коде называлась lastExamDate) */
    @NotNull
    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    /** Следующая плановая дата */
    @Column(name = "next_exam_date")
    private LocalDate nextExamDate;

    /** Дата направления */
    @Column(name = "referral_date")
    private LocalDate referralDate;

    /** Баллы/пункты */
    @Column(name = "points", columnDefinition = "TEXT")
    private String points;

    /* ======== ШИМ СОВМЕСТИМОСТИ СО СТАРЫМ КОДОМ ======== */

    /** Старое имя поля: lastExamDate → теперь это examDate */
    @Transient
    public LocalDate getLastExamDate() {
        return this.examDate;
    }

    /** Старое имя сеттера: setLastExamDate → теперь пишет в examDate */
    public void setLastExamDate(LocalDate d) {
        this.examDate = d;
    }

    /**
     * Старое поле frequencyYears: в БД его нет.
     * Делаем транзиентную «тень» + авто-вычисление из examType, если не задано явно.
     */
    @Transient
    private Integer frequencyYearsShadow;

    @Transient
    public Integer getFrequencyYears() {
        if (frequencyYearsShadow != null) return frequencyYearsShadow;
        // Простейшая деривация из типа:
        // PERIODIC_1Y → 1, PERIODIC_2Y → 2, PSYCHIATRIC → 1, иначе null
        if (examType == null) return null;
        if (examType.startsWith("PERIODIC_1Y")) return 1;
        if (examType.startsWith("PERIODIC_2Y")) return 2;
        if (examType.startsWith("PSYCHIATRIC")) return 1;
        return null;
    }

    public void setFrequencyYears(Integer years) {
        this.frequencyYearsShadow = years;
    }
}
