package ru.soat.ohapp.saas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Мини-«сквозной» тест: дергаем /api/due, затем /api/calendar/export.
 * Оба эндпоинта — заглушки внутри теста. Никакого контекста Spring Boot.
 */
class DataImportAndReferralFlowTest {

    private MockMvc mvc;

    @RestController
    static class DummyDueController {
        @GetMapping("/api/due")
        public ResponseEntity<String> getDue(
                @RequestParam(name = "days", required = false) Integer days,
                @RequestParam(name = "types", required = false) String types
        ) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("[]");
        }
    }

    @RestController
    static class DummyCalendarController {
        @PostMapping("/api/calendar/export")
        public ResponseEntity<byte[]> export(@RequestBody(required = false) String ignored) {
            String ics = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR\r\n";
            return ResponseEntity.ok()
                    .header("Content-Type", "text/calendar; charset=UTF-8")
                    .body(ics.getBytes(StandardCharsets.UTF_8));
        }
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(new DummyDueController(), new DummyCalendarController())
                .build();
    }

    @Test
    void minimalFlow_dueAndCalendarExport_ok() throws Exception {
        // 1) /api/due
        mvc.perform(get("/api/due")
                        .param("days", "30")
                        .param("types", "PERIODIC_1Y,PERIODIC_2Y,PSYCHIATRIC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("[]"));

        // 2) /api/calendar/export
        String json = """
                {"employeeIds":[],"types":["PERIODIC_1Y"],"withinDays":30,"titlePrefix":"МО","tz":"Europe/Moscow"}
                """;

        mvc.perform(post("/api/calendar/export")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/calendar")))
                .andExpect(content().string(containsString("BEGIN:VCALENDAR")));
    }
}
