package com.jkingai.diagramarchitect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-IP rate limit settings backed by Bucket4j token buckets.
 *
 * <p>Two independent buckets are tracked per client IP:
 * <ul>
 *   <li><b>General</b> bucket — applied to every authenticated endpoint.</li>
 *   <li><b>Generate</b> bucket — additional, stricter limit for
 *       {@code POST /api/v1/diagrams/generate} since that endpoint hits Vertex AI.</li>
 * </ul>
 *
 * <p>Both buckets refill greedily over a one-minute window with the configured
 * {@code requests-per-minute} rate; {@code burst-capacity} is the maximum number
 * of tokens that can accumulate (i.e. how many requests are allowed in a sudden
 * burst before the refill rate kicks in).
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int requestsPerMinute,
        int burstCapacity,
        int generateRequestsPerMinute,
        int generateBurstCapacity
) {
    public RateLimitProperties {
        if (requestsPerMinute <= 0) {
            requestsPerMinute = 60;
        }
        if (burstCapacity <= 0) {
            burstCapacity = 10;
        }
        if (generateRequestsPerMinute <= 0) {
            generateRequestsPerMinute = 30;
        }
        if (generateBurstCapacity <= 0) {
            generateBurstCapacity = 5;
        }
    }
}
