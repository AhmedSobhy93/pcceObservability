package com.cisco.cx.observability.controller;

import com.cisco.cx.observability.config.PcceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final PcceProperties pcceProperties;

    public RequestLoggingFilter(PcceProperties pcceProperties) {
        this.pcceProperties = pcceProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            String message = "http_request method={} path={} status={} elapsedMs={}";
            if (elapsedMs >= pcceProperties.getPerformance().getSlowRequestWarningMs()) {
                log.warn(message, request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
            } else {
                log.info(message, request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
            }
        }
    }
}
