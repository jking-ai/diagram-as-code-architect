package com.jkingai.diagramarchitect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jkingai.diagramarchitect.dto.DiagramRequest;
import com.jkingai.diagramarchitect.dto.DiagramResponse;
import com.jkingai.diagramarchitect.exception.DiagramGenerationException;
import com.jkingai.diagramarchitect.exception.UnsupportedDiagramTypeException;
import com.jkingai.diagramarchitect.model.CodeLanguage;
import com.jkingai.diagramarchitect.model.DiagramType;
import com.jkingai.diagramarchitect.config.SecurityConfig;
import com.jkingai.diagramarchitect.service.DiagramGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DiagramController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.api-key=test-api-key",
        "app.security.allowed-origins=*",
        "app.rate-limit.enabled=false"
})
class DiagramControllerTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String TEST_API_KEY = "test-api-key";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private DiagramGenerationService generationService;

    @Test
    void generate_validRequest_returns200WithResponse() throws Exception {
        DiagramResponse response = new DiagramResponse(
                "flowchart TB\n    A --> B",
                DiagramType.FLOWCHART,
                CodeLanguage.JAVA,
                Map.of("model", "gemini-3.1-flash-lite-preview", "inputCharacters", 20, "processingTimeMs", 1500L));

        when(generationService.generate(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "public class Hello {}", "diagramType": "FLOWCHART", "codeLanguage": "JAVA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mermaidSyntax").value("flowchart TB\n    A --> B"))
                .andExpect(jsonPath("$.diagramType").value("FLOWCHART"))
                .andExpect(jsonPath("$.codeLanguage").value("JAVA"))
                .andExpect(jsonPath("$.metadata.model").value("gemini-3.1-flash-lite-preview"));
    }

    @Test
    void generate_missingCode_returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diagramType": "FLOWCHART", "codeLanguage": "JAVA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void generate_blankCode_returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "   ", "diagramType": "FLOWCHART", "codeLanguage": "JAVA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void generate_invalidDiagramType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "class A {}", "diagramType": "INVALID", "codeLanguage": "JAVA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void generate_invalidCodeLanguage_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "class A {}", "diagramType": "FLOWCHART", "codeLanguage": "PYTHON"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void generate_unsupportedCombination_returns400() throws Exception {
        when(generationService.generate(any()))
                .thenThrow(new UnsupportedDiagramTypeException("SEQUENCE not supported for HCL"));

        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "resource {}", "diagramType": "SEQUENCE", "codeLanguage": "JAVA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_DIAGRAM_TYPE"));
    }

    @Test
    void generate_llmFailure_returns502() throws Exception {
        when(generationService.generate(any()))
                .thenThrow(new DiagramGenerationException("LLM failed", new RuntimeException("upstream error")));

        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "class A {}", "diagramType": "FLOWCHART", "codeLanguage": "JAVA"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("LLM_ERROR"));
    }

    @Test
    void generate_missingApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/diagrams/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "class A {}", "diagramType": "FLOWCHART", "codeLanguage": "JAVA"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void getTypes_returns200WithAllFiveTypes() throws Exception {
        mockMvc.perform(get("/api/v1/diagrams/types")
                        .header(API_KEY_HEADER, TEST_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagramTypes", hasSize(5)))
                .andExpect(jsonPath("$.diagramTypes[*].type",
                        containsInAnyOrder("FLOWCHART", "SEQUENCE", "CLASS", "ENTITY_RELATIONSHIP", "INFRASTRUCTURE")))
                .andExpect(jsonPath("$.diagramTypes[0].supportedLanguages").isArray())
                .andExpect(jsonPath("$.diagramTypes[0].mermaidDirective").isString());
    }

    @Test
    void getTypes_flowchartSupportsJavaAndHcl() throws Exception {
        mockMvc.perform(get("/api/v1/diagrams/types")
                        .header(API_KEY_HEADER, TEST_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagramTypes[?(@.type == 'FLOWCHART')].supportedLanguages[*]",
                        hasItem("JAVA")))
                .andExpect(jsonPath("$.diagramTypes[?(@.type == 'FLOWCHART')].supportedLanguages[*]",
                        hasItem("HCL")));
    }
}
