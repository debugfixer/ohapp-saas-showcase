package ru.soat.ohapp.saas.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.service.CalendarExportService;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarExportService calendarExportService;

    @PostMapping(value = "/export", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ByteArrayResource> export(@RequestBody CalendarExportService.Request req) {
        byte[] data = calendarExportService.buildIcs(req);
        ByteArrayResource res = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"medical_exams.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .contentLength(data.length)
                .body(res);
    }
}
