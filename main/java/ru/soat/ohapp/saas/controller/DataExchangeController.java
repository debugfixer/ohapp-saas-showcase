// src/main/java/ru/soat/ohapp/saas/controller/DataExchangeController.java
package ru.soat.ohapp.saas.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.soat.ohapp.saas.service.DataExchangeService;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataExchangeController {

    private final DataExchangeService dataExchangeService;

    @PostMapping("/import")
    public ResponseEntity<String> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Пожалуйста, выберите файл.");
        }
        try {
            dataExchangeService.importEmployeesFromExcel(file);
            return ResponseEntity.ok("Файл успешно импортирован.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при импорте файла: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            byte[] excelData = dataExchangeService.exportEmployeesToExcel();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=employees.xlsx");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}