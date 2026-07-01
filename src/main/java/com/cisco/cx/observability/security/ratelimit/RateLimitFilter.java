package com.cisco.cx.observability.security.ratelimit;

import com.cisco.cx.observability.config.PcceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final PcceProperties pcceProperties;
    private final Map<String, Deque<Long>> requestsByClient = new ConcurrentHashMap<>();

    public RateLimitFilter(PcceProperties pcceProperties) {
        this.pcceProperties = pcceProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        PcceProperties.Security.RateLimit rateLimit = pcceProperties.getSecurity().getRateLimit();
        if (rateLimit == null || !rateLimit.isEnabled() || "/actuator/health".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        int maxRequests = Math.max(1, rateLimit.getMaxRequestsPerMinute());
        String key = clientKey(request);
        long now = Instant.now().toEpochMilli();
        Deque<Long> timestamps = requestsByClient.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= maxRequests) {
                response.setStatus(429);
                response.setHeader("Retry-After", "60");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}");
                return;
            }
            timestamps.addLast(now);
        }
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
