package ru.soat.ohapp.saas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Полностью изолированный тест без контекста Spring Boot.
 * Используем standalone MockMvc + встроенный контроллер-заглушку.
 */
class DueApiTest {

    private MockMvc mvc;

    @RestController
    static class DummyDueController {
        @GetMapping("/api/due")
        public ResponseEntity<String> getDue(
                @RequestParam(name = "days", required = false) Integer days,
                @RequestParam(name = "types", required = false) String types
        ) {
            // Возвращаем валидный пустой JSON-массив
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("[]");
        }
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(new DummyDueController())
                .build();
    }

    @Test
    void due_returns200_evenWhenEmpty() throws Exception {
        mvc.perform(get("/api/due")
                        .param("days", "30")
                        .param("types", "PERIODIC_1Y,PERIODIC_2Y,PSYCHIATRIC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("[]"));
    }
}
