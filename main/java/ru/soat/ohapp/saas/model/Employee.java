package ru.soat.ohapp.saas.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "employees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_emp_tenant_tab", columnNames = {"tenant_id", "tab_number"})
        }
)
@Data
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UUID арендатора (NOT NULL в БД)
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // В БД колонка называется tab_number
    @Column(name = "tab_number", nullable = false) // <-- БЫЛО: @Column(nullable = false)
    private String personnelNumber;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String department;
    private String position;
    private LocalDate hireDate;
    private LocalDate birthDate;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "required_points1y", columnDefinition = "TEXT")
    private String requiredPoints1y;

    @Column(name = "required_points2y", columnDefinition = "TEXT")
    private String requiredPoints2y;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MedicalExam> medicalExams = new ArrayList<>();
}
