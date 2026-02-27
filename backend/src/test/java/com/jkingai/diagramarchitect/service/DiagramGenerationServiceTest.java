package com.jkingai.diagramarchitect.service;

import com.jkingai.diagramarchitect.dto.DiagramRequest;
import com.jkingai.diagramarchitect.dto.DiagramResponse;
import com.jkingai.diagramarchitect.exception.DiagramGenerationException;
import com.jkingai.diagramarchitect.exception.UnsupportedDiagramTypeException;
import com.jkingai.diagramarchitect.model.CodeLanguage;
import com.jkingai.diagramarchitect.model.DiagramType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiagramGenerationServiceTest {

    @Mock private CodeAnalysisService codeAnalysisService;
    @Mock private PromptTemplateEngine promptTemplateEngine;
    @Mock private MermaidSyntaxExtractor mermaidSyntaxExtractor;
    @Mock private ResilientLlmClient resilientLlmClient;

    private DiagramGenerationService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new DiagramGenerationService(
                codeAnalysisService, promptTemplateEngine, mermaidSyntaxExtractor, resilientLlmClient);
        Field modelField = DiagramGenerationService.class.getDeclaredField("modelName");
        modelField.setAccessible(true);
        modelField.set(service, "gemini-2.0-flash");
    }

    @Test
    void generate_validRequest_returnsResponse() {
        DiagramRequest request = new DiagramRequest(
                "public class Hello {}", DiagramType.FLOWCHART, CodeLanguage.JAVA, null);

        when(codeAnalysisService.validateAndNormalize(anyString(), any(), any()))
                .thenReturn("public class Hello {}");
        when(promptTemplateEngine.assemblePrompt(any(), any(), anyString(), any()))
                .thenReturn("assembled prompt");
        when(resilientLlmClient.call(anyString()))
                .thenReturn("```mermaid\nflowchart TB\n    A --> B\n```");
        when(mermaidSyntaxExtractor.extract(anyString()))
                .thenReturn("flowchart TB\n    A --> B");

        DiagramResponse response = service.generate(request);

        assertNotNull(response);
        assertEquals("flowchart TB\n    A --> B", response.mermaidSyntax());
        assertEquals(DiagramType.FLOWCHART, response.diagramType());
        assertEquals(CodeLanguage.JAVA, response.codeLanguage());
        assertNotNull(response.metadata());
        assertTrue((int) response.metadata().get("inputCharacters") > 0);
        assertTrue((long) response.metadata().get("processingTimeMs") >= 0);
    }

    @Test
    void generate_validationFails_propagatesException() {
        DiagramRequest request = new DiagramRequest(
                "", DiagramType.FLOWCHART, CodeLanguage.JAVA, null);

        when(codeAnalysisService.validateAndNormalize(anyString(), any(), any()))
                .thenThrow(new IllegalArgumentException("code is required"));

        assertThrows(IllegalArgumentException.class, () -> service.generate(request));
    }

    @Test
    void generate_unsupportedCombination_propagatesException() {
        DiagramRequest request = new DiagramRequest(
                "code", DiagramType.SEQUENCE, CodeLanguage.HCL, null);

        when(codeAnalysisService.validateAndNormalize(anyString(), any(), any()))
                .thenThrow(new UnsupportedDiagramTypeException("not supported"));

        assertThrows(UnsupportedDiagramTypeException.class, () -> service.generate(request));
    }

    @Test
    void generate_llmCallFails_throwsDiagramGenerationException() {
        DiagramRequest request = new DiagramRequest(
                "public class Hello {}", DiagramType.FLOWCHART, CodeLanguage.JAVA, null);

        when(codeAnalysisService.validateAndNormalize(anyString(), any(), any()))
                .thenReturn("code");
        when(promptTemplateEngine.assemblePrompt(any(), any(), anyString(), any()))
                .thenReturn("prompt");
        when(resilientLlmClient.call(anyString()))
                .thenThrow(new RuntimeException("LLM unavailable"));

        DiagramGenerationException ex = assertThrows(DiagramGenerationException.class,
                () -> service.generate(request));
        assertTrue(ex.getMessage().contains("upstream AI service"));
        assertNotNull(ex.getCause());
    }

    @Test
    void generate_extractionFails_propagatesException() {
        DiagramRequest request = new DiagramRequest(
                "public class Hello {}", DiagramType.FLOWCHART, CodeLanguage.JAVA, null);

        when(codeAnalysisService.validateAndNormalize(anyString(), any(), any()))
                .thenReturn("code");
        when(promptTemplateEngine.assemblePrompt(any(), any(), anyString(), any()))
                .thenReturn("prompt");
        when(resilientLlmClient.call(anyString()))
                .thenReturn("no valid mermaid here");
        when(mermaidSyntaxExtractor.extract(anyString()))
                .thenThrow(new DiagramGenerationException("no valid mermaid"));

        assertThrows(DiagramGenerationException.class, () -> service.generate(request));
    }

    @Test
    void generate_withContext_passesContextThrough() {
        DiagramRequest request = new DiagramRequest(
                "public class Hello {}", DiagramType.FLOWCHART, CodeLanguage.JAVA, "Focus on auth");

        when(codeAnalysisService.validateAndNormalize(anyString(), any(), any()))
                .thenReturn("code");
        when(promptTemplateEngine.assemblePrompt(any(), any(), anyString(), eq("Focus on auth")))
                .thenReturn("prompt");
        when(resilientLlmClient.call(anyString()))
                .thenReturn("flowchart TB\n    A --> B");
        when(mermaidSyntaxExtractor.extract(anyString())).thenReturn("flowchart TB\n    A --> B");

        DiagramResponse response = service.generate(request);
        assertNotNull(response);

        verify(promptTemplateEngine).assemblePrompt(
                eq(CodeLanguage.JAVA), eq(DiagramType.FLOWCHART), eq("code"), eq("Focus on auth"));
    }
}
