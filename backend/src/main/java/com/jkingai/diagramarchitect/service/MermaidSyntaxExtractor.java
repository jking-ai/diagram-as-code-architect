package com.jkingai.diagramarchitect.service;

import com.jkingai.diagramarchitect.exception.DiagramGenerationException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MermaidSyntaxExtractor {

    private static final Set<String> VALID_DIRECTIVES = Set.of(
            "flowchart", "sequenceDiagram", "classDiagram", "erDiagram", "graph"
    );

    private static final Pattern FENCED_BLOCK = Pattern.compile(
            "```(?:mermaid)?\\s*\\n(.*?)\\n\\s*```", Pattern.DOTALL
    );

    public String extract(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            throw new DiagramGenerationException("LLM returned an empty response.");
        }

        String syntax = extractFromFencedBlock(llmResponse);
        if (syntax == null) {
            syntax = llmResponse.strip();
        }

        // Validate it starts with a known Mermaid directive
        if (!startsWithValidDirective(syntax)) {
            throw new DiagramGenerationException(
                    "Failed to extract valid Mermaid syntax from LLM response. " +
                    "Response did not contain a recognized Mermaid directive.");
        }

        return syntax;
    }

    private String extractFromFencedBlock(String text) {
        Matcher matcher = FENCED_BLOCK.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).strip();
        }
        return null;
    }

    private boolean startsWithValidDirective(String syntax) {
        String firstLine = syntax.lines().findFirst().orElse("").strip();
        return VALID_DIRECTIVES.stream().anyMatch(d -> firstLine.startsWith(d));
    }
}
