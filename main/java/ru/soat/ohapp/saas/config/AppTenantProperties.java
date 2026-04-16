package ru.soat.ohapp.saas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Настройки арендатора на уровне приложения.
 * app.tenant.default-slug — какой slug использовать по умолчанию,
 * если фронт не прислал ничего.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.tenant")
public class AppTenantProperties {
    /**
     * Дефолтный slug. Если не задан в yml, будет "demo".
     */
    private String defaultSlug = "demo";
}
