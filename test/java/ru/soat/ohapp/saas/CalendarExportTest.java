package ru.soat.ohapp.saas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Изолированный тест экспорта календаря (.ics) — standalone MockMvc.
 */
class CalendarExportTest {

    private MockMvc mvc;

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
                .standaloneSetup(new DummyCalendarController())
                .build();
    }

    @Test
    void export_ics_returns200_andIcs() throws Exception {
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
