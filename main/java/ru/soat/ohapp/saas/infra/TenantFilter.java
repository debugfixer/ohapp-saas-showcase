package ru.soat.ohapp.saas.infra;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantFilter implements Filter {

    private final TenantResolver tenantResolver;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        UUID tenantId = tenantResolver.resolveTenantId(req);
        if (tenantId != null) {
            // используем request-атрибут; его читает membership-фильтр
            req.setAttribute("TENANT_ID", tenantId);
        }

        chain.doFilter(request, response);
    }
}
