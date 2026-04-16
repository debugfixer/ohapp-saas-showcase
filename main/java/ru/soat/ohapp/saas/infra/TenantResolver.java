package ru.soat.ohapp.saas.infra;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.soat.ohapp.saas.model.Tenant;
import ru.soat.ohapp.saas.repo.TenantRepository;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantResolver {

    private final TenantRepository tenants;

    /**
     * Основной способ: достаём слаг из заголовка X-Tenant или query param tenant и отдаём UUID тенанта.
     * Возвращает null, если слаг не передан или тенант не найден.
     */
    public UUID resolveTenantId(HttpServletRequest req) {
        String slug = Optional.ofNullable(req.getHeader("X-Tenant"))
                .filter(s -> !s.isBlank())
                .orElseGet(() -> {
                    String q = req.getParameter("tenant");
                    return (q != null && !q.isBlank()) ? q : null;
                });

        if (slug == null) return null;

        return tenants.findBySlug(slug)
                .map(Tenant::getId)
                .orElse(null);
    }

    /**
     * Вспомогательный метод, если где-то нужно получить UUID по слагу напрямую.
     */
    public UUID resolveTenantIdBySlug(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return tenants.findBySlug(slug)
                .map(Tenant::getId)
                .orElse(null);
    }
}
