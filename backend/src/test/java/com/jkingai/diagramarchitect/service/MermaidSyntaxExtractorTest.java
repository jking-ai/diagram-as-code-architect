package com.jkingai.diagramarchitect.service;

import com.jkingai.diagramarchitect.exception.DiagramGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MermaidSyntaxExtractorTest {

    private MermaidSyntaxExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new MermaidSyntaxExtractor();
    }

    @Test
    void extract_fencedMermaidBlock_returnsSyntax() {
        String input = "Here is the diagram:\n```mermaid\nflowchart TB\n    A --> B\n```\nDone.";
        String result = extractor.extract(input);
        assertEquals("flowchart TB\n    A --> B", result);
    }

    @Test
    void extract_fencedBlockWithoutMermaidLabel_returnsSyntax() {
        String input = "```\nsequenceDiagram\n    A->>B: hello\n```";
        String result = extractor.extract(input);
        assertEquals("sequenceDiagram\n    A->>B: hello", result);
    }

    @Test
    void extract_rawFlowchart_returnsSyntax() {
        String input = "flowchart TB\n    A --> B\n    B --> C";
        String result = extractor.extract(input);
        assertEquals(input, result);
    }

    @Test
    void extract_rawSequenceDiagram_returnsSyntax() {
        String input = "sequenceDiagram\n    A->>B: call";
        assertEquals(input, extractor.extract(input));
    }

    @Test
    void extract_rawClassDiagram_returnsSyntax() {
        String input = "classDiagram\n    class Foo";
        assertEquals(input, extractor.extract(input));
    }

    @Test
    void extract_rawErDiagram_returnsSyntax() {
        String input = "erDiagram\n    USER ||--o{ ORDER : places";
        assertEquals(input, extractor.extract(input));
    }

    @Test
    void extract_graphDirective_returnsSyntax() {
        String input = "graph TD\n    A --> B";
        assertEquals(input, extractor.extract(input));
    }

    @Test
    void extract_nullInput_throwsException() {
        assertThrows(DiagramGenerationException.class, () -> extractor.extract(null));
    }

    @Test
    void extract_emptyInput_throwsException() {
        assertThrows(DiagramGenerationException.class, () -> extractor.extract(""));
    }

    @Test
    void extract_blankInput_throwsException() {
        assertThrows(DiagramGenerationException.class, () -> extractor.extract("   "));
    }

    @Test
    void extract_noValidDirective_throwsException() {
        DiagramGenerationException ex = assertThrows(DiagramGenerationException.class,
                () -> extractor.extract("This is just plain text without any diagram."));
        assertTrue(ex.getMessage().contains("Mermaid"));
    }

    @Test
    void extract_fencedBlockWithLeadingWhitespace_returnsTrimmedSyntax() {
        String input = "```mermaid\n  flowchart LR\n    A --> B\n```";
        String result = extractor.extract(input);
        assertTrue(result.startsWith("flowchart"));
    }
}
