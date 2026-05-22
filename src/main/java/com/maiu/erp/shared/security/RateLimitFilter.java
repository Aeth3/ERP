package com.maiu.erp.shared.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final long capacity;
    private final Duration refillDuration;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.enabled:false}") boolean enabled,
            @Value("${app.rate-limit.capacity:120}") long capacity,
            @Value("${app.rate-limit.refill-duration:PT1M}") Duration refillDuration) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillDuration = refillDuration;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(
                resolveClientKey(request),
                ignored -> newBucket());

        if (bucket.tryConsume(1)) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
        response.setHeader("Retry-After", String.valueOf(Math.max(1L, refillDuration.toSeconds())));
        response.getWriter().write("{\"message\":\"Too many requests. Please try again shortly.\"}");
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.simple(capacity, refillDuration);
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
