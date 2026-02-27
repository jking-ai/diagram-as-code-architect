package com.jkingai.diagramarchitect.service;

import com.jkingai.diagramarchitect.exception.UnsupportedDiagramTypeException;
import com.jkingai.diagramarchitect.model.CodeLanguage;
import com.jkingai.diagramarchitect.model.DiagramType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeAnalysisServiceTest {

    private CodeAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new CodeAnalysisService();
    }

    @Test
    void validateAndNormalize_validJavaFlowchart_returnsNormalizedCode() {
        String code = "  public class Hello { }  ";
        String result = service.validateAndNormalize(code, DiagramType.FLOWCHART, CodeLanguage.JAVA);
        assertEquals("public class Hello { }", result);
    }

    @Test
    void validateAndNormalize_validHclInfrastructure_returnsNormalizedCode() {
        String code = "resource \"google_compute_network\" \"vpc\" { name = \"main\" }";
        String result = service.validateAndNormalize(code, DiagramType.INFRASTRUCTURE, CodeLanguage.HCL);
        assertEquals(code, result);
    }

    @Test
    void validateAndNormalize_nullCode_throwsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.validateAndNormalize(null, DiagramType.FLOWCHART, CodeLanguage.JAVA));
        assertTrue(ex.getMessage().contains("required"));
    }

    @Test
    void validateAndNormalize_blankCode_throwsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.validateAndNormalize("   ", DiagramType.FLOWCHART, CodeLanguage.JAVA));
        assertTrue(ex.getMessage().contains("required"));
    }

    @Test
    void validateAndNormalize_codeTooLarge_throwsIllegalArgument() {
        String largeCode = "x".repeat(50_001);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.validateAndNormalize(largeCode, DiagramType.FLOWCHART, CodeLanguage.JAVA));
        assertTrue(ex.getMessage().contains("50,000"));
    }

    @Test
    void validateAndNormalize_codeExactlyAtLimit_succeeds() {
        String code = "x".repeat(50_000);
        String result = service.validateAndNormalize(code, DiagramType.FLOWCHART, CodeLanguage.JAVA);
        assertEquals(50_000, result.length());
    }

    @Test
    void validateAndNormalize_sequenceWithHcl_throwsUnsupportedDiagramType() {
        UnsupportedDiagramTypeException ex = assertThrows(UnsupportedDiagramTypeException.class,
                () -> service.validateAndNormalize("code", DiagramType.SEQUENCE, CodeLanguage.HCL));
        assertTrue(ex.getMessage().contains("SEQUENCE"));
        assertTrue(ex.getMessage().contains("HCL"));
    }

    @Test
    void validateAndNormalize_infrastructureWithJava_throwsUnsupportedDiagramType() {
        assertThrows(UnsupportedDiagramTypeException.class,
                () -> service.validateAndNormalize("code", DiagramType.INFRASTRUCTURE, CodeLanguage.JAVA));
    }

    @Test
    void validateAndNormalize_normalizesWindowsLineEndings() {
        String code = "line1\r\nline2\r\nline3";
        String result = service.validateAndNormalize(code, DiagramType.FLOWCHART, CodeLanguage.JAVA);
        assertEquals("line1\nline2\nline3", result);
    }

    @Test
    void validateAndNormalize_normalizesOldMacLineEndings() {
        String code = "line1\rline2";
        String result = service.validateAndNormalize(code, DiagramType.FLOWCHART, CodeLanguage.JAVA);
        assertEquals("line1\nline2", result);
    }
}
