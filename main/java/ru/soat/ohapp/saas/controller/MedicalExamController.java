package ru.soat.ohapp.saas.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.dto.MedicalExamDto;
import ru.soat.ohapp.saas.service.MedicalExamService;

@RestController
@RequestMapping("/api/medical-exams")
@RequiredArgsConstructor
public class MedicalExamController {

    private final MedicalExamService medicalExamService;

    @PostMapping
    public ResponseEntity<MedicalExamDto> createMedicalExam(@RequestBody MedicalExamDto medicalExamDto) {
        try {
            MedicalExamDto createdExamDto = medicalExamService.createMedicalExam(medicalExamDto);
            return ResponseEntity.status(201).body(createdExamDto);
        } catch (RuntimeException e) {
            // Например, если сотрудник с medicalExamDto.getEmployeeId() не найден
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalExamDto> updateMedicalExam(@PathVariable Long id, @RequestBody MedicalExamDto medicalExamDto) {
        try {
            MedicalExamDto updatedExamDto = medicalExamService.updateMedicalExam(id, medicalExamDto);
            return ResponseEntity.ok(updatedExamDto);
        } catch (RuntimeException e) {
            // Например, если медосмотр с таким id не найден
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicalExam(@PathVariable Long id) {
        medicalExamService.deleteMedicalExam(id);
        return ResponseEntity.noContent().build();
    }
}