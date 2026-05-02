package com.jkingai.diagramarchitect.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-IP rate limiting filter, mirroring the classroom-clarity-rag pattern.
 *
 * <p>Two buckets per client IP:
 * <ul>
 *   <li>A general bucket consumed on every request that reaches the filter.</li>
 *   <li>A stricter "generate" bucket consumed only on
 *       {@code POST /api/v1/diagrams/generate}.</li>
 * </ul>
 *
 * <p>The filter sits in front of the API key filter so unauthenticated traffic
 * is also throttled — attackers can't burn auth checks (or upstream Vertex AI
 * quota) cheaply.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    static final String GENERATE_PATH = "/api/v1/diagrams/generate";
    private static final long RETRY_AFTER_SECONDS = 60L;

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> generateBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();

        // Stricter limit for the LLM-backed generate endpoint
        if (GENERATE_PATH.equals(path)) {
            Bucket generateBucket = generateBuckets.computeIfAbsent(clientIp, k -> createGenerateBucket());
            if (!generateBucket.tryConsume(1)) {
                writeRateLimitResponse(response);
                return;
            }
        }

        // General per-IP limit applied to every non-skipped path
        Bucket generalBucket = generalBuckets.computeIfAbsent(clientIp, k -> createGeneralBucket());
        if (!generalBucket.tryConsume(1)) {
            writeRateLimitResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/v1/health") || path.startsWith("/actuator/");
    }

    Bucket createGeneralBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.burstCapacity())
                        .refillGreedy(properties.requestsPerMinute(), Duration.ofMinutes(1))
                        .build())
                .build();
    }

    Bucket createGenerateBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(properties.generateBurstCapacity())
                        .refillGreedy(properties.generateRequestsPerMinute(), Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(RETRY_AFTER_SECONDS));

        // LinkedHashMap to keep field order stable in the JSON body.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "RATE_LIMIT_EXCEEDED");
        body.put("message", "Too many requests. Please try again later.");
        body.put("retryAfterSeconds", RETRY_AFTER_SECONDS);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
