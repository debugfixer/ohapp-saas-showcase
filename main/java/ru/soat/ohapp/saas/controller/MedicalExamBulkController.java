package ru.soat.ohapp.saas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.dto.BulkExamUpdateRequest;
import ru.soat.ohapp.saas.service.MedicalExamBulkService;

import java.util.Map;

@RestController
@RequestMapping("/api/medical-exams")
@RequiredArgsConstructor
public class MedicalExamBulkController {

    private final MedicalExamBulkService bulkService;

    /**
     * Пакетное обновление (upsert) медосмотров.
     * Пример payload:
     * {
     *   "employeeIds":[1,2,3],
     *   "examTypes":["PERIODIC_1Y","PERIODIC_2Y"],
     *   "lastExamDate":"2025-09-21",
     *   "points":"Химические факторы, шум",
     *   "recalcNextFromLast":true,
     *   "comment":"После осмотра 20.09"
     * }
     */
    @PostMapping("/bulk-update")
    public ResponseEntity<Map<String, Object>> bulkUpdate(@Valid @RequestBody BulkExamUpdateRequest req) {
        int changed = bulkService.bulkUpsert(req);
        return ResponseEntity.ok(Map.of("updated", changed));
    }
}
