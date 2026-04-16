package ru.soat.ohapp.saas;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = OhappSaaSApplication.class, // если класс называется иначе — подставь точное имя мейн-класса
        properties = {
                "spring.flyway.enabled=false",
                "provision.secret=test-secret",
                // на всякий случай не трогаем схему в тестах
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
@ActiveProfiles("test")
class OhappSaasApplicationTests {

    @Test
    void contextLoads() {
        // just load context
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        JavaMailSender javaMailSender() {
            return Mockito.mock(JavaMailSender.class);
        }
    }
}
