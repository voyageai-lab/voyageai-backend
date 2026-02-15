package com.voyageai.voyageaibackend.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate limiting configuration using Resilience4j RateLimiter.
 *
 * <p>Provides per-user rate limiting on planning API endpoints to prevent
 * abuse and protect downstream AI services (which are expensive per call).
 *
 * <p>Implementation uses a ConcurrentHashMap of per-user RateLimiters,
 * lazily created on first request. Each user gets their own limiter.
 *
 * <p>Default: 10 requests per minute per user on /api/planning/**.
 */
@Configuration
@Slf4j
public class RateLimitConfig implements WebMvcConfigurer {

    @Value("${rate.limit.requests-per-minute:10}")
    private int requestsPerMinute;

    @Value("${rate.limit.timeout-ms:0}")
    private long timeoutMs;

    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor())
                .addPathPatterns("/api/planning/**");
    }

    /**
     * Get or create a RateLimiter for the given user.
     */
    private RateLimiter getLimiter(String userId) {
        return limiters.computeIfAbsent(userId, id -> {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(requestsPerMinute)
                    .limitRefreshPeriod(Duration.ofMinutes(1))
                    .timeoutDuration(Duration.ofMillis(timeoutMs))
                    .build();
            return RateLimiter.of("user-" + id, config);
        });
    }

    /**
     * Extract user identifier from the request.
     * Falls back to IP address for unauthenticated requests.
     */
    private String extractUserId(HttpServletRequest request) {
        // Try authenticated user principal
        if (request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }
        // Fall back to IP address
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Spring MVC interceptor that enforces rate limits.
     */
    private class RateLimitInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(
                HttpServletRequest request,
                HttpServletResponse response,
                Object handler) throws Exception {

            String userId = extractUserId(request);
            RateLimiter limiter = getLimiter(userId);

            if (limiter.acquirePermission()) {
                return true;
            }

            log.warn("Rate limit exceeded for user: {}", userId);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Rate limit exceeded. "
                    + "Maximum " + requestsPerMinute
                    + " requests per minute.\"}");
            return false;
        }
    }
}
