package com.jkingai.diagramarchitect.service;

import com.jkingai.diagramarchitect.exception.UnsupportedDiagramTypeException;
import com.jkingai.diagramarchitect.model.CodeLanguage;
import com.jkingai.diagramarchitect.model.DiagramType;
import org.springframework.stereotype.Service;

@Service
public class CodeAnalysisService {

    private static final int MAX_CODE_LENGTH = 50_000;

    public String validateAndNormalize(String code, DiagramType diagramType, CodeLanguage codeLanguage) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("The 'code' field is required and must not be blank.");
        }

        if (code.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Code input exceeds the maximum allowed size of 50,000 characters. Received: %,d characters.",
                            code.length()));
        }

        if (!diagramType.supportsLanguage(codeLanguage)) {
            throw new UnsupportedDiagramTypeException(
                    String.format("Diagram type %s is not supported for code language %s. Supported types for %s: %s.",
                            diagramType, codeLanguage, codeLanguage, getSupportedTypesForLanguage(codeLanguage)));
        }

        // Normalize whitespace: trim and normalize line endings
        return code.strip().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
    }

    private String getSupportedTypesForLanguage(CodeLanguage language) {
        StringBuilder sb = new StringBuilder();
        for (DiagramType dt : DiagramType.values()) {
            if (dt.supportsLanguage(language)) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(dt.name());
            }
        }
        return sb.toString();
    }
}
