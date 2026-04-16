package ru.soat.ohapp.saas;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

/** Проверяем, что Spring-контекст стартует в профиле test. */
@SpringBootTest(
        classes = OhappSaaSApplication.class, // поменяй, если у тебя другое имя main-класса
        properties = {
                "spring.flyway.enabled=false",
                "provision.secret=test-secret",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
@ActiveProfiles("test")
class SmokeContextTest {

    @Test
    void contextLoads() { }

    @TestConfiguration
    static class TestBeans {
        @Bean
        JavaMailSender javaMailSender() {
            return Mockito.mock(JavaMailSender.class);
        }
    }
}
