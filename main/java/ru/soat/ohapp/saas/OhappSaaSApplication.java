// src/main/java/ru/soat/ohapp/saas/OhappSaaSApplication.java
package ru.soat.ohapp.saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EntityScan(basePackages = {
        "ru.soat.ohapp.saas.domain",  // Reminder и др.
        "ru.soat.ohapp.saas.model"    // Tenant и др.
})
@EnableJpaRepositories(basePackages = "ru.soat.ohapp.saas.repo")
@EnableScheduling
public class OhappSaaSApplication {
    public static void main(String[] args) {
        SpringApplication.run(OhappSaaSApplication.class, args);
    }
}
