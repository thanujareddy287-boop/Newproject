package com.example.studentapp.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Demo protection for login/register endpoints.
        if (!path.equals("/api/auth/login") && !path.equals("/api/auth/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        long now = Instant.now().getEpochSecond();

        Window window = windows.compute(ip, (key, old) -> {
            if (old == null || now - old.start >= WINDOW_SECONDS) {
                return new Window(now, 1);
            }
            return new Window(old.start, old.count + 1);
        });

        if (window.count > MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many requests. Try again later.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private record Window(long start, int count) {}
}
