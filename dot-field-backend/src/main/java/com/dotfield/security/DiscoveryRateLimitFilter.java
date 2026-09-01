package com.dotfield.security;

import com.dotfield.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client rate limiter filter for discovery and ingestion endpoints.
 * Uses Bucket4j in-memory token buckets keyed by composite key (userId or client IP address).
 */
@Slf4j
@Component
public class DiscoveryRateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int capacity;
    private final int refillIntervalSeconds;

    @org.springframework.beans.factory.annotation.Autowired
    public DiscoveryRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${rate.limiter.discovery.enabled:true}") boolean enabled,
            @Value("${rate.limiter.discovery.capacity:100}") int capacity,
            @Value("${rate.limiter.discovery.refill-interval-seconds:60}") int refillIntervalSeconds) {
        ObjectMapper mapper = objectMapper != null ? objectMapper.copy() : new ObjectMapper();
        this.objectMapper = mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillIntervalSeconds = refillIntervalSeconds;
    }

    public DiscoveryRateLimitFilter(ObjectMapper objectMapper, int capacity, int refillIntervalSeconds) {
        this(objectMapper, true, capacity, refillIntervalSeconds);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return !path.endsWith("/jobs/discover") && !path.endsWith("/jobs/ingestion/run")
                && !path.endsWith("/auth/login") && !path.endsWith("/auth/register");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String key = resolveClientKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client key: {} on path: {}", key, request.getRequestURI());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(refillIntervalSeconds));

            ApiError error = ApiError.builder()
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .message("Rate limit exceeded for discovery endpoint. Please try again later.")
                    .timestamp(LocalDateTime.now())
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(error));
        }
    }

    private String resolveClientKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return "ip:" + xForwardedFor.split(",")[0].trim();
        }

        return "ip:" + request.getRemoteAddr();
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofSeconds(refillIntervalSeconds))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
