package com.jkingai.diagramarchitect.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.api-key=test-api-key",
        "app.security.allowed-origins=*",
        "app.rate-limit.enabled=false"
})
class DiagramGenerationIntegrationTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String TEST_API_KEY = "test-api-key";

    @TestConfiguration
    static class MockChatClientConfig {
        @Bean
        @Primary
        public ChatClient.Builder chatClientBuilder() {
            ChatClient chatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(anyString())).thenReturn(requestSpec);
            when(requestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.content()).thenReturn("""
                    ```mermaid
                    flowchart TB
                        subgraph API["API Layer"]
                            OC[OrderController]
                        end
                        subgraph Services["Service Layer"]
                            OS[OrderService]
                            PS[PaymentService]
                        end
                        OC --> OS
                        OC --> PS
                    ```
                    """);

            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.build()).thenReturn(chatClient);
            return builder;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generateFlowchart_endToEnd_returns200WithMermaidSyntax() throws Exception {
        String javaCode = "@RestController\\n@RequestMapping(\\\"/api/v1/orders\\\")\\npublic class OrderController {\\n    private final OrderService orderService;\\n}";

        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"" + javaCode + "\", \"diagramType\": \"FLOWCHART\", \"codeLanguage\": \"JAVA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mermaidSyntax", startsWith("flowchart")))
                .andExpect(jsonPath("$.diagramType").value("FLOWCHART"))
                .andExpect(jsonPath("$.codeLanguage").value("JAVA"))
                .andExpect(jsonPath("$.metadata.model").isString())
                .andExpect(jsonPath("$.metadata.processingTimeMs").isNumber())
                .andExpect(jsonPath("$.metadata.inputCharacters").isNumber());
    }

    @Test
    void generateWithContext_endToEnd_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"public class Hello {}\", \"diagramType\": \"FLOWCHART\", \"codeLanguage\": \"JAVA\", \"context\": \"Focus on main method\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mermaidSyntax").isString());
    }

    @Test
    void generateWithInvalidInput_endToEnd_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagramType\": \"FLOWCHART\", \"codeLanguage\": \"JAVA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void generate_missingApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"class A {}\", \"diagramType\": \"FLOWCHART\", \"codeLanguage\": \"JAVA\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void health_noApiKey_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
