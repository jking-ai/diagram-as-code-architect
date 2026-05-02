package com.jkingai.diagramarchitect.config;

import com.jkingai.diagramarchitect.controller.DiagramController;
import com.jkingai.diagramarchitect.service.DiagramGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that per-IP rate limiting fires <em>before</em> the API key auth filter,
 * so unauthenticated traffic cannot burn auth checks (or upstream Vertex AI quota)
 * cheaply.
 *
 * <p>The generate-bucket capacity is set to 5 here, matching the production default
 * defined in {@code application.yml}. With no {@code X-API-Key} header on any
 * request, the first 5 hits should return {@code 401 UNAUTHORIZED} (rate limit
 * passes, then API key check fails) and the 6th should return {@code 429
 * RATE_LIMIT_EXCEEDED} (rate limit fires before auth).
 */
@WebMvcTest(DiagramController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.api-key=test-api-key",
        "app.security.allowed-origins=*",
        "app.rate-limit.enabled=true",
        "app.rate-limit.requests-per-minute=60",
        "app.rate-limit.burst-capacity=10",
        "app.rate-limit.generate-requests-per-minute=30",
        "app.rate-limit.generate-burst-capacity=5"
})
class RateLimitFilterTest {

    private static final String GENERATE_PATH = "/api/v1/diagrams/generate";
    private static final String VALID_BODY = """
            {"code": "class A {}", "diagramType": "FLOWCHART", "codeLanguage": "JAVA"}
            """;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private DiagramGenerationService generationService;

    @Test
    void generate_sixthUnauthenticatedRequest_returns429NotUntil401() throws Exception {
        // First 5 requests: rate limit passes (bucket has 5 tokens), API key check fails -> 401
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post(GENERATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BODY))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        // 6th request: generate bucket exhausted -> 429 BEFORE auth runs
        mockMvc.perform(post(GENERATE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.retryAfterSeconds").value(60));
    }
}
