package ru.soat.ohapp.saas.infra;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Простой in-memory rate limit: 100 req / 60s на IP.
 * Никаких внешних зависимостей.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter implements Filter {

    private static final int LIMIT = 100;
    private static final long WINDOW_MS = 60_000L;

    static class Counter {
        volatile long windowStartMs = Instant.now().toEpochMilli();
        AtomicInteger count = new AtomicInteger(0);
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req  = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String key = req.getRemoteAddr();
        long now = System.currentTimeMillis();

        Counter c = counters.computeIfAbsent(key, k -> new Counter());
        synchronized (c) {
            if (now - c.windowStartMs > WINDOW_MS) {
                c.windowStartMs = now;
                c.count.set(0);
            }
            if (c.count.incrementAndGet() > LIMIT) {
                res.setStatus(429);
                res.getWriter().write("Too Many Requests");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
