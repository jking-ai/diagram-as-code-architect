package com.jkingai.diagramarchitect.service;

import com.jkingai.diagramarchitect.exception.UnsupportedDiagramTypeException;
import com.jkingai.diagramarchitect.model.CodeLanguage;
import com.jkingai.diagramarchitect.model.DiagramType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptTemplateEngineTest {

    private PromptTemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PromptTemplateEngine();
    }

    @Test
    void assemblePrompt_javaFlowchart_returnsPromptWithCodeAndContext() {
        String code = "public class OrderService { }";
        String context = "Focus on service dependencies";

        String result = engine.assemblePrompt(CodeLanguage.JAVA, DiagramType.FLOWCHART, code, context);

        assertTrue(result.contains(code), "Prompt should contain the code");
        assertTrue(result.contains(context), "Prompt should contain the context");
        assertFalse(result.contains("{code}"), "Placeholder {code} should be replaced");
        assertFalse(result.contains("{context}"), "Placeholder {context} should be replaced");
    }

    @Test
    void assemblePrompt_hclInfrastructure_returnsPromptWithCode() {
        String code = "resource \"google_compute_network\" \"vpc\" { }";

        String result = engine.assemblePrompt(CodeLanguage.HCL, DiagramType.INFRASTRUCTURE, code, null);

        assertTrue(result.contains(code));
        assertTrue(result.contains("No additional context provided."));
    }

    @Test
    void assemblePrompt_nullContext_replacesWithDefault() {
        String result = engine.assemblePrompt(CodeLanguage.JAVA, DiagramType.CLASS, "class Foo {}", null);
        assertTrue(result.contains("No additional context provided."));
    }

    @Test
    void assemblePrompt_blankContext_replacesWithDefault() {
        String result = engine.assemblePrompt(CodeLanguage.JAVA, DiagramType.CLASS, "class Foo {}", "   ");
        assertTrue(result.contains("No additional context provided."));
    }

    @Test
    void assemblePrompt_hclSequence_throwsUnsupportedDiagramType() {
        assertThrows(UnsupportedDiagramTypeException.class,
                () -> engine.assemblePrompt(CodeLanguage.HCL, DiagramType.SEQUENCE, "code", null));
    }

    @Test
    void assemblePrompt_javaInfrastructure_throwsUnsupportedDiagramType() {
        assertThrows(UnsupportedDiagramTypeException.class,
                () -> engine.assemblePrompt(CodeLanguage.JAVA, DiagramType.INFRASTRUCTURE, "code", null));
    }

    @Test
    void assemblePrompt_allValidCombinations_loadSuccessfully() {
        // Verify all 6 valid template combinations load without error
        assertDoesNotThrow(() -> engine.assemblePrompt(CodeLanguage.JAVA, DiagramType.FLOWCHART, "code", null));
        assertDoesNotThrow(() -> engine.assemblePrompt(CodeLanguage.JAVA, DiagramType.SEQUENCE, "code", null));
        assertDoesNotThrow(() -> engine.assemblePrompt(CodeLanguage.JAVA, DiagramType.CLASS, "code", null));
        assertDoesNotThrow(() -> engine.assemblePrompt(CodeLanguage.JAVA, DiagramType.ENTITY_RELATIONSHIP, "code", null));
        assertDoesNotThrow(() -> engine.assemblePrompt(CodeLanguage.HCL, DiagramType.FLOWCHART, "code", null));
        assertDoesNotThrow(() -> engine.assemblePrompt(CodeLanguage.HCL, DiagramType.INFRASTRUCTURE, "code", null));
    }
}
